package de.unisaarland.cs.se.selab.data

/**
 * Data class for logging in refueling
 * */
data class RefuelLogData(
    val shipID: Int,
    val harborID: Int,
    val amount: Int?,
    val refuelFromStation: Boolean,
    val refuelOwnCap: Boolean,
    val refuelStarted: Boolean,
    val otherShipID: Int?,
    val success: Boolean
)
