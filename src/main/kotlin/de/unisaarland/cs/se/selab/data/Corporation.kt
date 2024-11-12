package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.GarbageType
import java.util.SortedSet

/**
 * Corporation class.
 */
data class Corporation(
    val id: Int,
    val harbors: Set<Tile>,
    val garbageTypes: List<GarbageType>,
    var credits: Int
) {
    val ships = mutableListOf<Ship>()
    val assignedShipsPerTile = mutableMapOf<Tile, MutableList<Ship>>()
    val trackedGarbage = mutableSetOf<Garbage>()
    val information = mutableMapOf<Tile, SortedSet<Garbage>>()
    var lastCooperated: Corporation? = null

    // Some additional fields
    var decidedToPurchase = false
    var ordered = false
    var newShipID = 0
    var shipDeliveryTime = 0
    var shipProp: ShipProp? = null
    var buyingLocation = 0
    var buyingShip: Ship? = null
    val shipsRefuelStationStarted: MutableList<Ship> = mutableListOf()
    val shipsRefuelStationEnded: MutableList<Ship> = mutableListOf()
    val shipStartedRepairing: MutableList<Ship> = emptyList<Ship>().toMutableList()
    val shipsDoneRepairing: MutableList<Ship> = emptyList<Ship>().toMutableList()
    val refuelingOtherShips: MutableMap<Ship, Ship> = mutableMapOf()
}
