package de.unisaarland.cs.se.selab.data
/**
 * This class represents the Refueling Station of a harbor
 * */
data class RefuelingStation(
    val refuelCost: Int,
    var refuelTimes: Int,
) : Station()
