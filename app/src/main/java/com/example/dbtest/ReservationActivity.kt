package com.example.dbtest

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dbtest.data.Reservation
import com.example.dbtest.data.ReservationRepository
import com.example.dbtest.data.UserRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class ReservationActivity : AppCompatActivity() {

    private lateinit var tvBuildingName: TextView
    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var etPurpose: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button

    private lateinit var tvExistingSlotsLabel: TextView
    private lateinit var llExistingSlots: LinearLayout
    private lateinit var tvNoBookings: TextView
    private lateinit var llManageReservations: LinearLayout

    private lateinit var reservationRepository: ReservationRepository
    private val userRepository = UserRepository()
    private lateinit var buildingName: String
    private var currentUsername: String = ""
    private var selectedDate: String? = null
    private var selectedStartTime: String? = null
    private var selectedEndTime: String? = null

    //
    private var activeSlotsForSelectedDate: List<Reservation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reservation)

        // Initialize views
        tvBuildingName = findViewById(R.id.tv_building_name)
        etDate = findViewById(R.id.et_date)
        etStartTime = findViewById(R.id.et_start_time)
        etEndTime = findViewById(R.id.et_end_time)
        etPurpose = findViewById(R.id.et_purpose)
        btnSubmit = findViewById(R.id.btn_submit_reservation)
        btnCancel = findViewById(R.id.btn_cancel_reservation)

        tvExistingSlotsLabel = findViewById(R.id.tv_existing_slots_label)
        llExistingSlots = findViewById(R.id.ll_existing_slots)
        tvNoBookings = findViewById(R.id.tv_no_bookings)
        llManageReservations = findViewById(R.id.ll_manage_reservations)

        buildingName = intent.getStringExtra("BUILDING_NAME") ?: "Unknown Building"
        tvBuildingName.text = "Building: $buildingName"

        currentUsername = SessionManager.getLoggedInUsername(this) ?: ""

        reservationRepository = ReservationRepository()

        etDate.isFocusable = false
        etDate.isCursorVisible = false
        etDate.setOnClickListener { showDatePicker() }

        etStartTime.isFocusable = false
        etStartTime.isCursorVisible = false
        etStartTime.setOnClickListener { showTimePicker(isStart = true) }

        etEndTime.isFocusable = false
        etEndTime.isCursorVisible = false
        etEndTime.setOnClickListener { showTimePicker(isStart = false) }

        btnSubmit.setOnClickListener { submitReservation() }

        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }


        loadManageableReservations()
    }


    private fun showDatePicker() {
        val today = Calendar.getInstance()

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                selectedDate = date
                etDate.setText(String.format(Locale.US, "%02d/%02d/%04d", month + 1, dayOfMonth, year))
                selectedStartTime = null
                selectedEndTime = null
                etStartTime.setText("")
                etEndTime.setText("")
                loadExistingSlotsForDate(date)
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = today.timeInMillis

        dialog.show()
    }

    private fun showTimePicker(isStart: Boolean) {
        if (selectedDate == null) {
            Toast.makeText(this, "Pick a date first", Toast.LENGTH_SHORT).show()
            return
        }

        val now = Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val value = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                val display = formatDisplayTime(hourOfDay, minute)

                if (isStart) {
                    selectedStartTime = value
                    etStartTime.setText(display)
                } else {
                    selectedEndTime = value
                    etEndTime.setText(display)
                }

                val start = selectedStartTime
                val end = selectedEndTime
                if (start != null && end != null && start < end) {
                    val conflict = activeSlotsForSelectedDate.firstOrNull { r ->
                        start < r.endTime && r.startTime < end
                    }
                    if (conflict != null) {
                        Toast.makeText(
                            this,
                            "Heads up: that overlaps an existing booking " +
                                    "(${formatTimeDisplay(conflict.startTime)}-${formatTimeDisplay(conflict.endTime)}).",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun formatDisplayTime(hourOfDay: Int, minute: Int): String {
        val amPm = if (hourOfDay < 12) "AM" else "PM"
        val hour12 = when {
            hourOfDay == 0 -> 12
            hourOfDay > 12 -> hourOfDay - 12
            else -> hourOfDay
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
    }

    private fun formatTimeDisplay(time24: String): String {
        val parts = time24.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        return formatDisplayTime(hour, minute)
    }

    private fun formatDateDisplay(dateIso: String): String {
        val parts = dateIso.split("-")
        return "${parts[1]}/${parts[2]}/${parts[0]}"
    }


    private fun submitReservation() {
        val date = selectedDate
        val startTime = selectedStartTime
        val endTime = selectedEndTime
        val purpose = etPurpose.text.toString()

        if (date == null || startTime == null || endTime == null) {
            Toast.makeText(this, "Please select a date, start time, and end time", Toast.LENGTH_SHORT).show()
            return
        }

        if (startTime >= endTime) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false

        lifecycleScope.launch {
            val existing = reservationRepository.getActiveReservationsForBuildingAndDate(buildingName, date)
            val conflict = existing.firstOrNull { r -> startTime < r.endTime && r.startTime < endTime }

            if (conflict != null) {
                Toast.makeText(
                    this@ReservationActivity,
                    "That time is already reserved (${formatTimeDisplay(conflict.startTime)}-" +
                            "${formatTimeDisplay(conflict.endTime)}). Pick another time.",
                    Toast.LENGTH_LONG
                ).show()
                btnSubmit.isEnabled = true
                return@launch
            }

            val loggedInUser = if (currentUsername.isNotBlank()) {
                userRepository.getUserByUsername(currentUsername)
            } else null

            reservationRepository.insertReservation(
                Reservation(
                    buildingName = buildingName,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    purpose = purpose,
                    reservedByUsername = currentUsername,
                    reservedByName = loggedInUser?.fullName?.ifBlank { currentUsername } ?: currentUsername
                )
            )

            Toast.makeText(this@ReservationActivity, "Reservation submitted successfully!", Toast.LENGTH_LONG).show()

            setResult(Activity.RESULT_OK, Intent().putExtra("BUILDING_NAME", buildingName))
            finish()
        }
    }


    private fun loadExistingSlotsForDate(date: String) {
        lifecycleScope.launch {
            val slots = reservationRepository.getActiveReservationsForBuildingAndDate(buildingName, date)
            activeSlotsForSelectedDate = slots

            llExistingSlots.removeAllViews()
            if (slots.isEmpty()) {
                tvExistingSlotsLabel.visibility = View.GONE
            } else {
                tvExistingSlotsLabel.visibility = View.VISIBLE
                for (slot in slots) {
                    val row = TextView(this@ReservationActivity)
                    val reserver = slot.reservedByName.ifBlank { "Unknown" }
                    row.text = "• ${formatTimeDisplay(slot.startTime)} - ${formatTimeDisplay(slot.endTime)} " +
                            "(Reserved by: $reserver)"
                    row.textSize = 14f
                    row.setTextColor(android.graphics.Color.parseColor("#F44336"))
                    llExistingSlots.addView(row)
                }
            }
        }
    }


    private fun loadManageableReservations() {
        lifecycleScope.launch {
            val bookings = reservationRepository.getActiveReservationsForBuilding(buildingName)

            llManageReservations.removeAllViews()
            tvNoBookings.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE

            for (booking in bookings) {
                llManageReservations.addView(buildBookingRow(booking))
            }
        }
    }

    private fun buildBookingRow(booking: Reservation): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(12, 12, 12, 12)
        row.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowParams.bottomMargin = 12
        row.layoutParams = rowParams

        val info = TextView(this)
        val dateAndTime = "${formatDateDisplay(booking.date)}  ${formatTimeDisplay(booking.startTime)}" +
                " - ${formatTimeDisplay(booking.endTime)}"
        val reserver = booking.reservedByName.ifBlank { "Unknown" }
        val lines = mutableListOf("$dateAndTime\nReserved by: $reserver")
        if (booking.purpose.isNotBlank()) lines.add(booking.purpose)
        info.text = lines.joinToString("\n")
        info.textSize = 14f
        info.setTextColor(android.graphics.Color.parseColor("#000000"))
        row.addView(info)

        val isOwnBooking = currentUsername.isNotBlank() && booking.reservedByUsername == currentUsername
        if (!isOwnBooking) {
            return row
        }

        val buttonRow = LinearLayout(this)
        buttonRow.orientation = LinearLayout.HORIZONTAL
        buttonRow.gravity = Gravity.END
        val buttonRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonRowParams.topMargin = 8
        buttonRow.layoutParams = buttonRowParams

        val btnDone = Button(this)
        btnDone.text = "Mark as Done"
        btnDone.textSize = 12f
        btnDone.setOnClickListener { confirmStatusChange(booking, Reservation.STATUS_DONE) }

        val btnCancelBooking = Button(this)
        btnCancelBooking.text = "Cancel"
        btnCancelBooking.textSize = 12f
        val cancelParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cancelParams.marginEnd = 8
        btnCancelBooking.layoutParams = cancelParams
        btnCancelBooking.setOnClickListener { confirmStatusChange(booking, Reservation.STATUS_CANCELLED) }

        buttonRow.addView(btnCancelBooking)
        buttonRow.addView(btnDone)
        row.addView(buttonRow)

        return row
    }

    private fun confirmStatusChange(booking: Reservation, newStatus: String) {
        val message = if (newStatus == Reservation.STATUS_DONE) {
            "Mark this booking as done? This frees up the room for the rest of its time " +
                    "slot (e.g. early dismissal)."
        } else {
            "Cancel this booking? The room will be available for that time again."
        }

        AlertDialog.Builder(this)
            .setTitle(if (newStatus == Reservation.STATUS_DONE) "Mark as Done" else "Cancel Booking")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> applyStatusChange(booking, newStatus) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun applyStatusChange(booking: Reservation, newStatus: String) {
        lifecycleScope.launch {
            reservationRepository.updateStatus(booking.id, newStatus)

            Toast.makeText(
                this@ReservationActivity,
                if (newStatus == Reservation.STATUS_DONE) "Booking marked as done." else "Booking cancelled.",
                Toast.LENGTH_SHORT
            ).show()

            loadManageableReservations()
            selectedDate?.let { loadExistingSlotsForDate(it) }

            setResult(Activity.RESULT_OK, Intent().putExtra("BUILDING_NAME", buildingName))
        }
    }
}