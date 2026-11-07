package com.example.dbtest

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.example.dbtest.data.ReservationRepository
import com.example.dbtest.map.Building
import com.example.dbtest.map.CampusBuildings
import com.example.dbtest.map.Pathway
import com.example.dbtest.map.SiteMapView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class CampusMapActivity : AppCompatActivity() {

    private val zoomScale = 1.8f
    private val animDuration = 300L

    private lateinit var siteMapView: SiteMapView
    private lateinit var slider: Slider
    private var currentBuilding: Building? = null
    private var currentFloor = 1

    private val reservedBuildings = mutableSetOf<String>()

    private val reservationRepository by lazy { ReservationRepository() }

    private val reservationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshReservedBuildingsAndSlider()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.campus_map)

        siteMapView = findViewById(R.id.siteMapView)
        slider = Slider(rootView = findViewById(R.id.slider_root))

        slider.onClose = { resetZoom() }

        val callback = object : OnBackPressedCallback(true /* enabled */) {
            override fun handleOnBackPressed() {
                if (slider.isOpen) {
                    slider.close()
                } else {
                    moveTaskToBack(true)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        siteMapView.designWidth = 1284f
        siteMapView.designHeight = 531f
        siteMapView.pathwayColor = Pathway.COLOR
        siteMapView.pathwayOutline = Pathway.OUTLINE
        siteMapView.pathwayHoles = Pathway.HOLES
        siteMapView.buildings = CampusBuildings.ALL
        siteMapView.activeFloor = currentFloor


        findViewById<com.google.android.material.button.MaterialButton>(R.id.floorToggleButton).setOnClickListener {
            currentFloor = if (currentFloor == 1) 2 else 1
            applyFloor(currentFloor)
        }

        val tiltSeek = findViewById<android.widget.SeekBar>(R.id.tiltSeek)
        tiltSeek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, p: Int, fromUser: Boolean) {
                siteMapView.pitchDeg = 25f + p // 25..90
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })

        val view3dButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.view3dButton)
        val floorButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.floorToggleButton)
        view3dButton.setOnClickListener {
            val on = !siteMapView.mode3D
            siteMapView.set3D(on)
            siteMapView.pitchDeg = 25f + tiltSeek.progress
            view3dButton.text = if (on) "2D" else "3D"
            tiltSeek.visibility = if (on) android.view.View.VISIBLE else android.view.View.GONE
            // All storeys are visible at once in 3D, so the per-floor toggle doesn't apply.
            floorButton.visibility = if (on) android.view.View.GONE else android.view.View.VISIBLE
            if (slider.isOpen) slider.close()
            currentBuilding = null
        }

        siteMapView.onBuildingClick = { building ->
            currentBuilding = building
            zoomToBuilding(building.id)


            slider.open(
                building.label,
                isReserved = reservedBuildings.contains(building.label),
                reservable = building.reservable
            )
        }

        siteMapView.onBuildingLongClick = { building ->
            if (building.reservable) {
                showRoomHoverStatusDialog(building)
            }
        }


        slider.onReserveClick = {
            val building = currentBuilding
            if (building != null && building.reservable) {
                val intent = Intent(this, ReservationActivity::class.java)
                intent.putExtra("BUILDING_NAME", building.label)
                reservationLauncher.launch(intent)
            }
        }


        findViewById<FloatingActionButton>(R.id.floatingActionButton).setOnClickListener {
            startActivity(Intent(this, Settings::class.java))
        }

        findViewById<ComposeView>(R.id.compassOverlay).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var mapRotation by remember { mutableFloatStateOf(0f) }
                DisposableEffect(Unit) {
                    siteMapView.onMapRotationChanged = { degrees -> mapRotation = degrees }
                    onDispose { siteMapView.onMapRotationChanged = null }
                }
                CompassOverlay(mapRotationDegrees = mapRotation)
            }
        }

        setupLegend()
    }

    private fun setupLegend() {
        val container = findViewById<LinearLayout>(R.id.legendLayout)
        val header = findViewById<TextView>(R.id.legendHeader)
        val dp = resources.displayMetrics.density

        val items = listOf(
            "IT - Gray" to Color.argb(170, 105, 105, 105),
            "HM - Orange" to Color.argb(170, 255, 121, 0),
            "TM - Yellow" to Color.argb(170, 255, 235, 59),
            "BSE - Blue" to Color.argb(170, 33, 150, 243),
            "Canteens - Green" to Color.argb(170, 76, 175, 80),
            "CR, SSC Building, Fishery, Open Court, Hostel, Library - Red" to Color.argb(170, 244, 67, 54),
            "Chapel, Training Center, Registrar, Gray gym, Ancient - Brown" to Color.argb(170, 121, 92, 52),
            "Parking Area, School Van Parking Area - Beige" to Color.rgb(235, 231, 191),
            "Guard House - Pink" to Color.argb(170, 255, 192, 203),
        )

        items.forEach { (text, color) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (2 * dp).toInt(), 0, (2 * dp).toInt())
            }
            val swatch = View(this).apply {
                layoutParams = LinearLayout.LayoutParams((12 * dp).toInt(), (12 * dp).toInt())
                    .apply { marginEnd = (6 * dp).toInt() }
                if (color != null) {
                    background = GradientDrawable().apply {
                        setColor(color)
                        setStroke((1 * dp).toInt(), 0x33000000)
                    }
                }
            }
            val label = TextView(this).apply {
                this.text = text
                textSize = 11f
                maxWidth = (220 * dp).toInt()
                setTextColor(Color.BLACK)
            }
            row.addView(swatch)
            row.addView(label)
            container.addView(row)
        }

        header.setOnClickListener {
            val show = container.visibility != View.VISIBLE
            container.visibility = if (show) View.VISIBLE else View.GONE
            header.text = if (show) "Map Key ▾" else "Map Key ▸"
        }
    }

    override fun onResume() {
        super.onResume()
        loadReservedBuildings()
    }

    private fun loadReservedBuildings() {
        lifecycleScope.launch {
            reservedBuildings.clear()
            reservedBuildings.addAll(fetchBuildingsReservedRightNow())
        }
    }

    private fun refreshReservedBuildingsAndSlider() {
        lifecycleScope.launch {
            reservedBuildings.clear()
            reservedBuildings.addAll(fetchBuildingsReservedRightNow())

            val label = currentBuilding?.label
            if (label != null) {
                slider.setReservationState(isReserved = reservedBuildings.contains(label))
            }
        }
    }

    private suspend fun fetchBuildingsReservedRightNow(): List<String> {
        val now = Calendar.getInstance()
        val today = String.format(
            Locale.US, "%04d-%02d-%02d",
            now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)
        )
        val nowTime = String.format(
            Locale.US, "%02d:%02d",
            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)
        )
        return reservationRepository.getBuildingsReservedRightNow(today, nowTime)
    }

    private fun applyFloor(floor: Int) {
        siteMapView.activeFloor = floor
        findViewById<com.google.android.material.button.MaterialButton>(R.id.floorToggleButton).text =
            "Floor $floor"
        if (slider.isOpen) {
            slider.close()
        }
        currentBuilding = null
    }

    private fun zoomToBuilding(id: String) {
        val center = siteMapView.getBuildingCenterOnScreen(id) ?: return
        siteMapView.pivotX = center.x
        siteMapView.pivotY = center.y
        siteMapView.animate()
            .scaleX(zoomScale)
            .scaleY(zoomScale)
            .setDuration(animDuration)
            .start()
    }

    private fun resetZoom() {
        siteMapView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(animDuration)
            .start()
    }

    private fun showRoomHoverStatusDialog(building: Building) {
        lifecycleScope.launch {
            val activeReservations = reservationRepository.getActiveReservationsForBuilding(building.label)

            val dialogView = layoutInflater.inflate(R.layout.room_status, null)
            val tvRoomName = dialogView.findViewById<TextView>(R.id.tvDialogRoomName)
            val tvReservedBy = dialogView.findViewById<TextView>(R.id.tvDialogReservedBy)
            val tvDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
            val tvTimeSlot = dialogView.findViewById<TextView>(R.id.tvDialogTimeSlot)
            val btnClose = dialogView.findViewById<android.widget.ImageButton>(R.id.btnCloseRoomStatus)

            tvRoomName.text = building.label

            if (activeReservations.isNotEmpty()) {
                val statusBuilder = StringBuilder()
                activeReservations.forEachIndexed { index, reservation ->
                    if (index > 0) {
                        statusBuilder.append("\n")
                    }
                    statusBuilder.append("Reserved By: ${reservation.reservedByName}\n")
                    statusBuilder.append("Date: ${reservation.date}\n")
                    statusBuilder.append("Time: ${reservation.startTime} - ${reservation.endTime}\n")
                }
                tvReservedBy.text = statusBuilder.toString().trimEnd()
                tvDate.text = ""
                tvTimeSlot.text = ""
            } else {
                tvReservedBy.text = "Status: Available"
                tvDate.text = "No upcoming reservations found."
                tvTimeSlot.text = ""
            }

            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
                this@CampusMapActivity,
                R.style.RoomStatusDialogTheme
            )
                .setView(dialogView)
                .show()

            btnClose.setOnClickListener {
                dialog.dismiss()
            }
        }
    }
}