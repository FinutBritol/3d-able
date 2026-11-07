package com.example.dbtest.data

data class Reservation(
    var id: String = "",
    val buildingName: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val purpose: String = "",
    // Denormalized at creation time (not looked up later) so displaying a reservation
    // never needs a second query just to show who booked it.
    val reservedByUsername: String = "",
    val reservedByName: String = "",
    val status: String = STATUS_RESERVED
) {
    companion object {
        const val STATUS_RESERVED = "Reserved"
        const val STATUS_CANCELLED = "Cancelled"
        const val STATUS_DONE = "Done"
    }
}