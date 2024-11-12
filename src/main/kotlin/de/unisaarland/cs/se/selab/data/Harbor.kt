package de.unisaarland.cs.se.selab.data

/**
 * This is a Harbor class
 * */
data class Harbor(
    val id: Int,
    val location: Int,
    val corporations: IntArray,
    val shipyardStation: ShipyardStation?,
    val refuelingStation: RefuelingStation?,
    val unloadingStation: UnloadingStation?
)
