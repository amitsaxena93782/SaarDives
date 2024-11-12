package de.unisaarland.cs.se.selab.data

/**
 * This class represents the model for refueling ship props
 */
data class ShipProp(
    val maxVel: Int,
    val maxAccel: Int,
    val fuelCap: Int,
    val fuelConsum: Int,
    val refuelingCap: Int,
    val refuelingTime: Int
)
