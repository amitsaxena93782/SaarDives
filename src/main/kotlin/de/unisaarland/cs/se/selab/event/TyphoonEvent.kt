package de.unisaarland.cs.se.selab.event

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType
import de.unisaarland.cs.se.selab.enums.TileType
import de.unisaarland.cs.se.selab.parser.JsonKeys
import kotlin.math.max
import kotlin.math.min

/**
 * This class represents the Typhoon Event
 * */
class TyphoonEvent(
    id: Int,
    tick: Int,
    val location: Tile,
    val radius: Int,
    val strength: Int
) : Event(id, tick) {
    val createdGarbage = mutableListOf<Garbage>()
    var maxGarbageID = 0

    override fun execute(oceanMap: OceanMap) {
        // Logging the event
        Logger.logEvent(id, JsonKeys.TYPHOON)
        val affectedTiles = oceanMap.getTilesInRadius(location, radius)
        createdGarbage.clear()
        val affectedShips = mutableListOf<Ship>()
        maxGarbageID = oceanMap.getMaxGarbageId()

        affectedTiles.map { affectedShips.addAll(oceanMap.getShipsOnTile(it)) }

        for (ship in affectedShips) {
            when (strength) {
                Constants.ONE -> {
                    incFuelConsumed(ship)
                }
                Constants.TWO -> {
                    incFuelConsumed(ship)
                    destroyRewards(ship)
                }
                Constants.THREE -> {
                    incFuelConsumed(ship)
                    destroyRewards(ship)
                    dispose(ship, oceanMap)
                }
                Constants.FOUR -> {
                    incFuelConsumed(ship)
                    destroyRewards(ship)
                    dispose(ship, oceanMap)
                    reduceAccelAndMaxVel(ship)
                    // Ship will get damaged
                    ship.behaviour = Behaviour.DAMAGED
                    ship.isDamaged = true
                    ship.task = null
                }
            }
        }
        val shipIDs = affectedShips.map { it.id }.sorted()
        Logger.logTyphoonDamage(id, location.id, radius, shipIDs)
    }

    /**
     * Increases the fuel consumption by 100%
     * Also stores the original fuel Consumption for repairing
     * @param ship which got affected
     * @return Unit
     * */
    private fun incFuelConsumed(ship: Ship) {
        ship.fuelConsumption *= 2
    }

    /**
     * Destroys all telescopes and radios permanently
     * @param ship which got affected
     * @return Unit
     * */
    private fun destroyRewards(ship: Ship) {
        ship.reward.removeAll(listOf(RewardType.TELESCOPE, RewardType.RADIO))
        ship.usedVisibilityRange = ship.visibilityRange
    }

    /**
     * Disposes the garbages, by creating 3 piles per ship, assigning them the maxGarbageID everytime and
     * adding them to the createdGarbage.
     * @param ship which got affected
     * @return Unit
     * */
    private fun dispose(ship: Ship, oceanMap: OceanMap) {
        // Now starting to dispose
        for (garbageType in GarbageType.entries) {
            if (!ship.maxGarbageCapacity.keys.contains(garbageType)) continue
            val collectedAmt = ship.maxGarbageCapacity.getValue(garbageType) -
                ship.garbageCapacity.getValue(garbageType)
            if (collectedAmt > 0) {
                val newGarbage = Garbage(
                    ++maxGarbageID,
                    garbageType,
                    collectedAmt
                )
                createdGarbage.add(newGarbage)
                oceanMap.addGarbage(newGarbage, oceanMap.getShipTile(ship))

                // Removing the garbage again, if the garbage type is chemical on deep ocean tile
                if (garbageType == GarbageType.CHEMICALS &&
                    oceanMap.getShipTile(ship).type == TileType.DEEP_OCEAN
                ) {
                    oceanMap.removeGarbage(newGarbage)
                }
            }
            ship.garbageCapacity[garbageType] = ship.maxGarbageCapacity.getValue(garbageType)
        }
    }

    /**
     * Reduces the acceleration and maxVelocity
     * Also stores the original accel and the maxVel
     * @param ship which gets affected by the event
     * @return Unit
     * */
    private fun reduceAccelAndMaxVel(ship: Ship) {
        if (ship.orginalAccel == null) {
            ship.orginalAccel = ship.acceleration
        }
        if (ship.originalMaxVel == null) {
            ship.originalMaxVel = ship.maxVelocity
        }
        ship.acceleration = max(Constants.ONE, ship.acceleration - Constants.FOUR)
        ship.maxVelocity = min(Constants.FORTY, ship.maxVelocity)
        ship.velocity = min(ship.velocity, ship.maxVelocity)
    }
}
