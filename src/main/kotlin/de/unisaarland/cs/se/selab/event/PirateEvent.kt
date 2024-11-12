package de.unisaarland.cs.se.selab.event

import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.parser.JsonKeys

/**
 * This event deletes the specified ship from the simulation if the start tick is reached.
 */
class PirateEvent(
    private val shipId: Int,
    id: Int,
    tick: Int
) : Event(id, tick) {

    override fun execute(oceanMap: OceanMap) {
        val ship = oceanMap.shipToTile.keys.first { it.id == shipId }
        ship.task?.toBeRemoved = true
        ship.decoupleShips() // In case if the ship was refueling some ship
        oceanMap.removeShip(ship)
        Logger.logEvent(id, JsonKeys.PIRATE_ATTACK)
    }
}
