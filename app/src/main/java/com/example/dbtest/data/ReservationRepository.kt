package com.example.dbtest.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

/**
 * Replaces ReservationDao. Data lives under /reservations/{pushKey}.
 *
 * Realtime Database can only efficiently order/filter by ONE child key per
 * query (orderByChild + equalTo), so compound filters (building + date +
 * status) are done by fetching everything for that building and filtering
 * in Kotlin. Fine for a per-building reservation list; if this ever grows
 * to thousands of bookings per building, move to Firestore instead.
 */
class ReservationRepository {

    private val reservationsRef = FirebaseDatabase.getInstance().getReference("reservations")

    /**
     * True once this reservation's end time is in the past. Reservations only get a
     * STATUS_DONE/STATUS_CANCELLED when a user taps a button, so without this check
     * a booking would stay "Reserved" and keep blocking its slot forever after it ends.
     */
    private fun isExpired(reservation: Reservation): Boolean {
        val now = java.util.Calendar.getInstance()
        val today = String.format(
            java.util.Locale.US, "%04d-%02d-%02d",
            now.get(java.util.Calendar.YEAR),
            now.get(java.util.Calendar.MONTH) + 1,
            now.get(java.util.Calendar.DAY_OF_MONTH)
        )
        val nowTime = String.format(
            java.util.Locale.US, "%02d:%02d",
            now.get(java.util.Calendar.HOUR_OF_DAY),
            now.get(java.util.Calendar.MINUTE)
        )
        return reservation.date < today || (reservation.date == today && reservation.endTime <= nowTime)
    }

    suspend fun insertReservation(reservation: Reservation) {
        val ref = reservationsRef.push()
        ref.setValue(reservation.copy(id = "")).await()
    }

    suspend fun getReservationsForBuilding(buildingName: String): List<Reservation> {
        val snapshot = reservationsRef.orderByChild("buildingName").equalTo(buildingName).get().await()
        return snapshot.children.mapNotNull { child ->
            child.getValue(Reservation::class.java)?.copy(id = child.key ?: "")
        }
    }

    suspend fun getActiveReservationsForBuildingAndDate(buildingName: String, date: String): List<Reservation> {
        return getReservationsForBuilding(buildingName)
            .filter { it.date == date && it.status == Reservation.STATUS_RESERVED && !isExpired(it) }
            .sortedByDescending { it.id }
    }

    suspend fun getActiveReservationsForBuilding(buildingName: String): List<Reservation> {
        return getReservationsForBuilding(buildingName)
            .filter { it.status == Reservation.STATUS_RESERVED && !isExpired(it) }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    /** Buildings with an active booking covering this exact moment (for the map's status dot). */
    suspend fun getBuildingsReservedRightNow(today: String, nowTime: String): List<String> {
        val snapshot = reservationsRef.orderByChild("date").equalTo(today).get().await()
        return snapshot.children.mapNotNull { it.getValue(Reservation::class.java) }
            .filter { it.status == Reservation.STATUS_RESERVED && it.startTime <= nowTime && it.endTime > nowTime }
            .map { it.buildingName }
            .distinct()
    }

    suspend fun updateStatus(id: String, status: String) {
        reservationsRef.child(id).child("status").setValue(status).await()
    }

    /** Realtime replacement if a screen wants to observe ALL reservations live. */
    fun observeAllReservations(onChange: (List<Reservation>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    child.getValue(Reservation::class.java)?.copy(id = child.key ?: "")
                }
                onChange(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        reservationsRef.addValueEventListener(listener)
        return listener
    }

    fun removeListener(listener: ValueEventListener) {
        reservationsRef.removeEventListener(listener)
    }
}