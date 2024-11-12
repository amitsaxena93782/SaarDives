package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.RefuelLogData
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.ShipType
import de.unisaarland.cs.se.selab.task.CooperationTask

/**
 * Handles various ship-related tasks such as attaching trackers, collecting garbage,
 * refueling, unloading, and cooperation between corporations.
 */
class ShipHandler(
    private val oceanMap: OceanMap,
    private val visibilityHandler: VisibilityHandler,
    private val corporations: List<Corporation>
) {

    /**
     * Handles the tracker attachment for the given corporation.
     */
    fun attachTracker(corporation: Corporation) {
        for (ship in corporation.ships) {
            // Here also checking, if the ship is refueling or being refueled by other ship
            if (ship.canAttachTracker() && !ship.refuelingFromShip && ship.serviceAvailable) {
                attachTracker(ship)
            }
        }
    }

    /**
     * Attaches tracker to all garbage piles that are on the tile of the given ship.
     */
    private fun attachTracker(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        for (garbage in oceanMap.getGarbageOnTile(shipTile)) {
            if (garbage !in ship.corporation.trackedGarbage) {
                ship.corporation.trackedGarbage.add(garbage)
                Logger.logAttachTracker(ship.corporation.id, garbage.id, ship.id)
            }
        }
    }

    /**
     * Handles the collection phase for the given corporation.
     */
    fun collectionPhase(corporation: Corporation) {
        Logger.logCorpCollectGarbage(corporation.id)
        for (ship in corporation.ships) {
            // Here also checking, if the ship is not refueling or being refueled by other ship
            if (ship.canCollect() && !ship.refuelingFromShip && ship.type != ShipType.REFUELING) {
                collectGarbage(ship)
            }
        }
    }

    /**
     * Collects garbage on the tile of the given ship. The garbage will be collected and logged
     * in the following order: first by garbage type (PLASTIC, then OIL, then CHEMICALS),
     * and second by garbage id.
     *
     * The ship will stop collecting if it encounters garbage that it could theoretically pick up
     * (because it is a collecting ship of that garbage type or has a container for it),
     * but is unable to do so due to capacity limitations or other conditions.
     *
     * @see canCollectPlastic
     */
    private fun collectGarbage(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val garbageOnTile = oceanMap.getGarbageOnTile(shipTile)
        val garbagePerType = garbageOnTile.groupBy { it.type }
        val canCollectPlastic = canCollectPlastic(ship)

        // Creating an ordered garbageType
        val orderedGarbage = listOf(GarbageType.PLASTIC, GarbageType.OIL, GarbageType.CHEMICALS)

        for (garbageType in orderedGarbage) {
            for (garbage in garbagePerType[garbageType].orEmpty()) {
                val couldCollect = collectGarbage(ship, garbage, canCollectPlastic)
                if (!couldCollect) break
            }
        }
    }

    /**
     * Checks if the ships on the tile of the given ship have a sufficient collective capacity
     * to pick up all plastic garbage on that tile.
     */
    private fun canCollectPlastic(ship: Ship): Boolean {
        val shipTile = oceanMap.getShipTile(ship)
        val garbageOnTile = oceanMap.getGarbageOnTile(shipTile)
        val shipsOnTile = oceanMap.getShipsOnTile(shipTile)
        val plasticGarbageSum = SumsPerTypeUtil.getGarbageSumsPerType(garbageOnTile)[GarbageType.PLASTIC] ?: 0
        val plasticCapacitySum = SumsPerTypeUtil.getCapacitySumsPerType(shipsOnTile)[GarbageType.PLASTIC] ?: 0
        return plasticCapacitySum >= plasticGarbageSum
    }

    /**
     * Collects and logs the given garbage with the given ship. Returns false if the ship could theoretically
     * pick up the garbage (because it is a collecting ship of that garbage type or has a container for it),
     * but is unable to do so due to capacity limitations or other conditions. Returns true otherwise.
     */
    private fun collectGarbage(ship: Ship, garbage: Garbage, canCollectPlastic: Boolean): Boolean {
        val shipCapacity = ship.garbageCapacity[garbage.type] ?: return true
        val isPlastic = garbage.type == GarbageType.PLASTIC
        if (shipCapacity == 0 || (isPlastic && !canCollectPlastic)) return false
        if (shipCapacity >= garbage.amount) {
            ship.garbageCapacity[garbage.type] = shipCapacity - garbage.amount
            oceanMap.removeGarbage(garbage)
            Logger.logCollectGarbage(
                ship.id,
                garbage.amount,
                garbage.id,
                garbage.type,
                ship.corporation.id
            )
        } else if (shipCapacity > 0) {
            ship.garbageCapacity[garbage.type] = 0
            garbage.amount -= shipCapacity
            Logger.logCollectGarbage(
                ship.id,
                shipCapacity,
                garbage.id,
                garbage.type,
                ship.corporation.id
            )
        }
        return true
    }

    /**
     * Handles the cooperation phase for the given corporation.
     */
    fun cooperationPhase(corporation: Corporation) {
        Logger.logCoopStart(corporation.id)

        for (ship in corporation.ships) {
            val shipTile = oceanMap.getShipTile(ship)
            for (otherShip in oceanMap.getShipsOnTile(shipTile)) {
                if (ship.canCooperateWith(otherShip)) {
                    visibilityHandler.updateCorpInformation(otherShip.corporation, corporation)
                    corporation.lastCooperated = otherShip.corporation
                    Logger.logCoopWith(corporation.id, otherShip.corporation.id, ship.id, otherShip.id)
                }
            }
            performCooperationTask(ship)
        }
    }

    /**
     * If the given ship has a cooperation task and waited at the target tile for one tick,
     * then it will now cooperate with every corporation that has a harbor at the target tile.
     */
    private fun performCooperationTask(ship: Ship) {
        val shipTask = ship.task ?: return
        if (shipTask is CooperationTask && shipTask.finished) {
            val shipTile = oceanMap.getShipTile(ship)
            // Get all corporations with harbors at the target tile.
            for (corpWithHarbour in corporations.filter { shipTile in it.harbors }) {
                visibilityHandler.updateCorpInformation(ship.corporation, corpWithHarbour)
            }
            // The task is now completed.
            ship.task = null
        }
    }

    /**
     * Handles the refueling phase for the given corporation.
     */
    fun refuelingPhase(corporation: Corporation) {
        Logger.logRefuelStart(corporation.id)

        val affordedShips = mutableListOf<Ship>()
        val logShips = mutableMapOf<Int, RefuelLogData>()
        // Finishing refueling of ships that arrived in the last tick
        for (ship in corporation.shipsRefuelStationEnded) {
            refuelShipEnd(ship, logShips)
        }
        // Starting refueling for those who arrived at harbors at this tick
        for (ship in corporation.shipsRefuelStationStarted) {
            refuelShipStart(ship, affordedShips, logShips)
        }
        corporation.shipsRefuelStationStarted.clear()
        corporation.shipsRefuelStationEnded.clear()
        corporation.shipsRefuelStationEnded.addAll(affordedShips)

        displayLogData(logShips)

        logShips.clear()

        // Now refueling ships from refueling ship
        for (ship in corporation.ships) {
            // Here checking the need of refueling, and it's being refueled by other ship
            if (ship.refuelingShip != null) {
                refuelOtherShip(ship, logShips)
                corporation.shipsRefuelStationStarted.remove(ship.refuelingShip)
            }
        }
        // Displaying
        displayLogData(logShips)
    }

    private fun displayLogData(logShips: MutableMap<Int, RefuelLogData>) {
        // Now sorting the log data
        val sortedData = logShips.toSortedMap()

        // Displaying it
        for (data in sortedData.values) {
            if (data.refuelFromStation && data.refuelStarted) {
                Logger.logRefuelingFromStationStarted(
                    data.shipID,
                    data.harborID,
                    data.success,
                    data.amount,
                    data.refuelOwnCap
                )
            } else if (data.refuelFromStation) {
                Logger.logRefuelingDone(data.shipID, data.harborID)
            } else {
                data.otherShipID?.let { Logger.logRefuelFromShip(data.shipID, it, data.refuelStarted) }
            }
        }
    }

    /**
     * Starts refueling the given ship.
     * And then adds it to the ending queue.
     * @param ship which needs to be refueled.
     * @return Unit
     */
    private fun refuelShipStart(
        ship: Ship,
        affordedShips: MutableList<Ship>,
        logShips: MutableMap<
            Int,
            RefuelLogData
            >
    ) {
        val shipTile = oceanMap.getShipTile(ship)

        // First we check if the corp can afford the refueling
        val refuelingCost = ship.targetHarbor?.refuelingStation?.refuelCost ?: 0
        if (ship.targetHarbor?.refuelingStation?.refuelTimes?.let { it < 1 } == true) {
            ship.refuelingFromStation = false
            return
        }

        if (ship.corporation.credits >= refuelingCost) {
            // ship.decoupleShips() // Necessary?
            ship.refuelingFromStation = true
            ship.corporation.credits -= refuelingCost // Withdrawing the money from corporation
            val logRefuelCap = ship.refuelingOwnCap
            // Logger.logRefuelingFromStationStarted(ship.id, shipTile.harborID ?: 0, true, refuelingCost, logRefuelCap)
            logShips[ship.id] = RefuelLogData(
                ship.id,
                shipTile.harborID ?: 0,
                refuelingCost,
                true,
                logRefuelCap,
                true,
                null,
                true
            )
            // Adding the ship to the refuel ending queue
            affordedShips.add(ship)
        } else {
            logShips[ship.id] = RefuelLogData(
                ship.id,
                shipTile.harborID ?: 0,
                refuelingCost,
                true,
                false,
                true,
                null,
                false
            )
            // Logger.logRefuelingFromStationStarted(ship.id, shipTile.harborID ?: 0, false, null, false)
        }
        ship.velocity = 0
    }

    /**
     * Ends the refueling process of the ships at the station
     * @param ship which needs to be refueled
     * @return Unit
     * */
    private fun refuelShipEnd(ship: Ship, logShips: MutableMap<Int, RefuelLogData>) {
        val shipTile = oceanMap.getShipTile(ship)
        ship.velocity = 0
        // First check if the refueling station can actually afford it
        if (ship.targetHarbor?.refuelingStation?.refuelTimes?.let { it < 1 } == true) {
            ship.refuelingFromStation = false
            return
        }

        // Now the refueling must have been completed
        ship.waitingAtHarbor = false
        val refuelingCost = ship.targetHarbor?.refuelingStation?.refuelCost ?: 0

        // Logger.logRefuelingDone(ship.id, shipTile.harborID ?: 0)
        logShips[ship.id] = RefuelLogData(
            ship.id,
            shipTile.harborID ?: 0,
            refuelingCost,
            true,
            false,
            false,
            null,
            true
        )
        ship.refuelingFromStation = false

        // Restoring the fuel of the ship
        if (ship.refuelingOwnCap) {
            ship.refuelFuel = ship.refuelCap
            ship.refuelingOwnCap = false
        } else {
            ship.fuel = ship.maxFuel
        }
        ship.targetHarbor?.refuelingStation?.let { station ->
            station.refuelTimes -= 1
        }
        ship.targetHarbor = null // Removing the target harbor of the ship
        ship.refuelingFromStation = false

        if (ship.garbageCapacity.any { it.value == 0 }) {
            // If we have a full garbage container,
            // go unloading in the next tick.
            ship.behaviour = Behaviour.UNLOADING
        } else {
            ship.behaviour = Behaviour.DEFAULT
        }
    }

    /**
     * Refuels the ship from a refueling ship
     * @param ship Ship needs to be refueled
     * @return Unit
     * */
    private fun refuelOtherShip(ship: Ship, logShips: MutableMap<Int, RefuelLogData>) {
        // First the ship finds a refueling ship
        val shipTile = oceanMap.getShipTile(ship)
        if (ship.refuelOtherStarted) {
            ship.refuelOtherStarted = false
            ship.refuelingShip?.let { refuelShip ->
                logShips[refuelShip.id] = RefuelLogData(
                    ship.id,
                    shipTile.harborID ?: 0,
                    0,
                    false,
                    false,
                    true,
                    refuelShip.id,
                    true
                )
            }

            ship.refuelWaitingTime-- // Timer started instantaneously
            ship.velocity = 0
            ship.refuelingShip?.velocity = 0
        } else if (ship.refuelWaitingTime > 0) {
            // After that we'll wait till the ship is being refueled
            ship.refuelWaitingTime--
            ship.velocity = 0
            ship.refuelingShip?.velocity = 0
        } else {
            // Now the refueling must have been completed
            ship.refuelingShip?.let { refuelShip ->
                logShips[refuelShip.id] = RefuelLogData(
                    ship.id,
                    shipTile.harborID ?: 0,
                    0,
                    false,
                    false,
                    false,
                    refuelShip.id,
                    true
                )
            }
            ship.velocity = 0
            ship.refuelingShip?.velocity = 0
            ship.completeRequest()
        }
    }

    /**
     * Handles the unloading phase for the given corporation.
     */
    fun unloadingPhase(corporation: Corporation) {
        for (ship in corporation.ships) {
            if (ship.isUnloading()) unloadShip(ship) // Integrated the check condition here only
        }
    }

    /**
     * Unload the given ship.
     */
    private fun unloadShip(ship: Ship) {
        // val harborID = oceanMap.getShipTile(ship).harborID ?: 0 // Although we'll always get a harbor
        val harborID = ship.targetHarbor?.id ?: 0
        // Creating an ordered garbage type list
        val orderedGarbageType = listOf(GarbageType.PLASTIC, GarbageType.OIL, GarbageType.CHEMICALS)
        // Checking for allowed garbages
        val allowedGarbages = ship.targetHarbor?.unloadingStation?.garbagesType.orEmpty()

        val allowedGarbagesInOrder = orderedGarbageType.filter { it in allowedGarbages }

        for (garbageType in allowedGarbagesInOrder) {
            if (ship.garbageCapacity[garbageType] == 0) {
                val garbageTypeCapacity = ship.maxGarbageCapacity.getValue(garbageType)
                ship.garbageCapacity[garbageType] = garbageTypeCapacity
                val returnValue = ship.targetHarbor?.unloadingStation?.unloadReturn?.times(garbageTypeCapacity) ?: 0
                Logger.logUnload(ship.id, garbageTypeCapacity, garbageType, harborID, returnValue)
                ship.corporation.credits += returnValue // Adding credits to the corporation
            }
        }
        ship.behaviour = Behaviour.DEFAULT
        ship.waitingAtHarbor = false
    }

    /**
     * Repairing phase of the corporation
     * @param corporation Corporation
     * @return Unit
     * */
    fun repairingPhase(corporation: Corporation) {
        val shipAffordedRepair = mutableListOf<Ship>()
        val logShips = mutableMapOf<Int, Pair<Int, Int>?>()
        // First those ships which have to start repairing
        for (ship in corporation.shipStartedRepairing) {
            repairShipStart(ship, shipAffordedRepair, logShips)
        }
        corporation.shipStartedRepairing.clear()
        // Now those ships which have completed their repairing
        for (ship in corporation.shipsDoneRepairing) {
            repairShipDone(ship, logShips)
        }
        corporation.shipsDoneRepairing.clear()
        corporation.shipsDoneRepairing.addAll(shipAffordedRepair)

        // Now logging
        val sortedLogShips = logShips.toSortedMap()
        sortedLogShips.map {
            val (harborId, repairCost) = it.value ?: Pair(null, null)
            if (harborId == null || repairCost == null) {
                Logger.logRepair(it.key, null, null, false)
            } else {
                Logger.logRepair(it.key, harborId, repairCost, true)
            }
        }
    }

    /**
     * Starts repairing the ship
     * @param ship Ship that needs to be repaired
     * @return true if the ship started repairing, false otherwise
     * */
    private fun repairShipStart(
        ship: Ship,
        shipAffordedRepair: MutableList<Ship>,
        logShips: MutableMap<Int, Pair<Int, Int>?>
    ) {
        val harborID = ship.targetHarbor?.id ?: 0
        val repairCost = ship.targetHarbor?.shipyardStation?.repairCost ?: 0 // Should never be 0

        // If the corp afford the repair
        if (ship.corporation.credits >= repairCost) {
            // Logger.logRepair(ship.id, harborID, repairCost, true)
            logShips[ship.id] = Pair(harborID, repairCost)
            ship.corporation.credits -= repairCost
            ship.decoupleShips() // In case this ship is about to refuel some ship
            shipAffordedRepair.add(ship)
        } else {
            // In case the corp can't afford the repair,
            // the ship will return to it's default behavior
            ship.behaviour = Behaviour.DEFAULT
        }
    }

    /**
     * Repairs the ship, restoring it's default properties
     * @param ship which needs to be repaired
     * @return Unit
     * */
    private fun repairShip(ship: Ship) {
        if (ship.originalMaxVel != null) {
            ship.maxVelocity = ship.originalMaxVel ?: 0
            ship.originalMaxVel = null
        }
        if (ship.orginalAccel != null) {
            ship.acceleration = ship.orginalAccel ?: 0
            ship.orginalAccel = null
        }
        ship.isDamaged = false
    }

    private fun repairShipDone(ship: Ship, logShips: MutableMap<Int, Pair<Int, Int>?>) {
        ship.waitingAtHarbor = false
        repairShip(ship)
        ship.velocity = 0
        logShips[ship.id] = null
        // Logger.logRepair(ship.id, null, null, false)
        ship.behaviour = Behaviour.DEFAULT
    }

    /**
     * Purchasing phase of the corporation
     * @param corporation Corporation
     * @return Unit
     * */
    fun purchasingPhase(corporation: Corporation) {
        val purchasingShip = corporation.buyingShip // Since there'll be just one ship
        // Check first, if the ship reached the station, order will be placed then
        if (purchasingShip != null) {
            purchaseShip(purchasingShip)
            corporation.buyingShip = null
        }
        // Taking the delivery of the ship
        if (corporation.ordered) {
            recieveDeliveryShip(corporation)
        }
    }

    private fun purchaseShip(ship: Ship) {
        val harborID = ship.targetHarbor?.id ?: 0
        val shipCost = ship.targetHarbor?.shipyardStation?.shipCost ?: 0 // Should never be 0
        // Check if the corporation can afford the ship
        if (ship.corporation.credits >= shipCost) {
            ship.corporation.credits -= shipCost
            // Saving the new ship props in the corporation
            ship.corporation.shipProp = ship.targetHarbor?.shipyardStation?.shipProp
            ship.corporation.shipDeliveryTime = ship.targetHarbor?.shipyardStation?.deliveryTime ?: 0
            // ship.corporation.shipDeliveryTime-- // Counter started
            ship.corporation.newShipID = ++Constants.MAX_SHIP_ID
            ship.corporation.ordered = true
            ship.corporation.buyingLocation = ship.targetHarbor?.location ?: 0
            Logger.logShipPurchase(ship.id, ship.corporation.newShipID, harborID, shipCost, 0, 0, true)
            // Setting the ship free
        } else {
            ship.corporation.decidedToPurchase = false
        }
        ship.purchasingStarted = false
        ship.behaviour = Behaviour.DEFAULT
        ship.goingToPurchase = false
    }

    /**
     * Receives the delivery of the ship when delivery time is over
     * @param corporation that has ordered the ship
     * @return Unit
     * */
    private fun recieveDeliveryShip(corporation: Corporation) {
        if (corporation.shipDeliveryTime > 0) { // Waiting
            corporation.shipDeliveryTime--
        } else {
            val shipProp = corporation.shipProp
            // Now new ship is being created with following properties
            val newShip = shipProp?.let {
                Ship(
                    corporation.newShipID,
                    ShipType.REFUELING,
                    corporation,
                    it.maxVel,
                    shipProp.maxAccel,
                    shipProp.fuelCap,
                    shipProp.fuelConsum,
                    0,
                    mutableMapOf()
                )
            }
            val buyingTile = oceanMap.getTileByID(corporation.buyingLocation).getOrNull()
            if (newShip != null && buyingTile != null) {
                newShip.refuelCap = shipProp.refuelingCap
                newShip.refuelFuel = shipProp.refuelingCap
                newShip.refuelTim = shipProp.refuelingTime
                corporation.ships.add(newShip)
                val tileID = corporation.buyingLocation // Should not be 0
                Logger.logShipPurchase(0, newShip.id, 0, 0, corporation.id, tileID, false)
                // Adding the ship on the tile
                oceanMap.addShip(newShip, buyingTile)
            }
            corporation.decidedToPurchase = false
            corporation.ordered = false
        }
    }
}
