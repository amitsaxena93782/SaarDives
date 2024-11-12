package de.unisaarland.cs.se.selab.control

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.Logger
import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Garbage
import de.unisaarland.cs.se.selab.data.Harbor
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.ShipType

/**
 * Handles the movement logic for ships during the movement phase.
 * Manages ship behavior, such as when to refuel, default behaviour and escaping restrictions.
 */
class MovementHandler(
    private val pathFinder: PathFinder,
    private val oceanMap: OceanMap,
    private val visibilityHandler: VisibilityHandler
) {
    private var harbors = emptyMap<Int, Harbor>()

    /**
     * Handles the movement phase for the given corporation.
     */
    fun movementPhase(corporation: Corporation, harbors: MutableMap<Int, Harbor>) {
        this.harbors = harbors
        Logger.logStartMove(corporation.id)
        for (ship in corporation.ships) {
            val shipTile = oceanMap.getShipTile(ship)
            if (shipTile in ship.corporation.harbors) {
                ship.waitingAtHarbor = true
            }

            ship.accelerate()

            // In case the ship got drifted while refueling, it'll get cancelled
            ship.refueledBy?.let { refueledByShip ->
                val refuelShipLoc = oceanMap.getShipTile(refueledByShip).id
                if (shipTile.id != refuelShipLoc) ship.decoupleShips()
            }

            ship.refuelingShip?.let { refuelingShip ->
                val refueledShipLoc = oceanMap.getShipTile(refuelingShip).id
                if (shipTile.id != refueledShipLoc) ship.decoupleShips()
            }

            moveShip(ship, shipTile)

            if (ship.behaviour != Behaviour.UNLOADING &&
                ship.behaviour != Behaviour.DAMAGED &&
                ship.behaviour != Behaviour.REFUELING
            ) {
                ship.waitingAtHarbor = false
            }

            if (ship.purchasingStarted) {
                ship.waitingAtHarbor = true
            }

            ship.task?.update(ship, oceanMap, movementPhase = true)
        }
    }

    /**
     * Determines the behaviour and moves the ship for this tick.
     */
    private fun moveShip(ship: Ship, shipTile: Tile) {
        val onRestriction = shipTile.restricted
        val needsToRefuel = ship.behaviour == Behaviour.REFUELING
        val hasTask = ship.task != null
        val needsToUnload = ship.garbageCapacity.any { it.value == 0 }
        val isDamaged = ship.behaviour == Behaviour.DAMAGED || ship.isDamaged
        val goingToPurchase = ship.behaviour == Behaviour.PURCHASING
        val busyInRefueling = ship.refuelingShip != null || ship.refueledBy != null
        val otherRefuelingShips = oceanMap.getShipsOnTile(oceanMap.getShipTile(ship)).any {
            it.id != ship.id && it.type == ShipType.REFUELING &&
                it.refuelingShip == null
        } && ship.fuel < Constants.HALF * ship.maxFuel

        // In case of emergency, the purchasing decision of coordinating ship gets cancelled
        if (onRestriction || needsToUnload) {
            cancelPurchase(ship)
        } else if (needsToRefuel || isDamaged) {
            cancelPurchase(ship)
        }
        // Conditions are prioritized top to bottom,
        // with higher priority cases listed first.
        when {
            onRestriction -> {
                // If the ship was being repaired, repair would be cancelled
                ship.corporation.shipsDoneRepairing.remove(ship)
                ship.corporation.shipsRefuelStationEnded.remove(ship)
                ship.decoupleShips()
                escapeRestriction(ship)
            }
            otherRefuelingShips -> {
                ship.velocity = 0
                ship.task = null
                return
            }
            busyInRefueling -> {
                ship.velocity = 0
                ship.task = null
                return
            }
            needsToRefuel -> {
                // If the ship was being repaired, repair would be cancelled
                ship.corporation.shipsDoneRepairing.remove(ship)
                ship.task = null
                if (!ship.refuelingFromStation) {
                    moveToRefuel(ship)
                }
            }
            isDamaged -> {
                ship.behaviour = Behaviour.DAMAGED
                ship.task = null
                if (ship !in ship.corporation.shipsDoneRepairing) {
                    moveToRepair(ship)
                }
            }
            else -> {
                move2(ship, hasTask, needsToUnload, goingToPurchase)
            }
        }
    }

    private fun move2(ship: Ship, hasTask: Boolean, needsToUnload: Boolean, goingToPurchase: Boolean) {
        when {
            hasTask -> {
                // ship.corporation.decidedToPurchase = false // Purchase Decision cancelled
                ship.refuelingFromStation = false
                moveToTask(ship)
            }
            needsToUnload -> {
                moveToUnload(ship)
            }
            goingToPurchase -> {
                if (!ship.purchasingStarted) {
                    moveToPurchase(ship)
                }
            }
            else -> {
                moveShipDefault(ship)
            }
        }
    }

    /**
     * It cancels the purchase if  the coordinating ship is already going to purchase
     * @param ship as Ship
     * @return Unit
     * */
    private fun cancelPurchase(ship: Ship) {
        if (ship.goingToPurchase) {
            ship.goingToPurchase = false
            ship.corporation.decidedToPurchase = false
        }
    }

    /**
     * Moves the ship along the path by determining how far it can travel
     * and what the intermediate destination would be. Redirects to a harbor if refueling is needed.
     * Also logs the ship movement and adjusts velocity if the destination is reached or no path is found.
     */
    private fun moveAlongPath(ship: Ship, path: List<Tile>) {
        if (path.isEmpty()) {
            ship.velocity = 0
            return
        }
        // Determine how far we can go this tick and
        // what the intermediate destination on our path would be.
        val distance = minOf(
            ship.getDistanceWithVelocity(),
            ship.getDistanceWithFuel(),
            path.size - 1
        )
        val intermediateDestination = path[distance]
        val fuelConsumed = distance * ship.fuelConsumption * Constants.TILE_DISTANCE
        val fuelNextTick = ship.fuel - fuelConsumed
        val reachableDistance = ship.getDistanceWithFuel(fuelNextTick)
        // If we are not either on the way to refueling or escaping,
        // then we have to check if we can make it back from the intermediate destination
        // to the harbor in the next tick and if not we will go refueling instead.
        val refuelHarbor = harbors.values.filter { it.refuelingStation != null }
        val refuelTiles = refuelHarbor.map { oceanMap.getTileByID(it.location).getOrNull() ?: return }.toSet()
        if (ship.behaviour != Behaviour.REFUELING &&
            ship.behaviour != Behaviour.ESCAPING &&
            !pathFinder.isReachableWithinDistance(
                intermediateDestination,
                refuelTiles,
                reachableDistance
            )
        ) {
            return moveToRefuel(ship)
        }
        if (distance > 0) {
            ship.fuel = fuelNextTick
            oceanMap.moveShip(ship, intermediateDestination)
            Logger.logShipMove(ship.id, ship.velocity, intermediateDestination.id)
        }
        val reachedDestination = intermediateDestination == path.lastOrNull()
        if (reachedDestination && ship.behaviour != Behaviour.EXPLORING) {
            ship.velocity = 0
            // Now calculating the cost that needed to be paid
            val harborID = intermediateDestination.harborID
            ship.targetHarbor = harbors[harborID]
            if (ship.isRefueling()) {
                ship.corporation.shipsRefuelStationStarted.add(ship)
            } else if (ship.isRepairing()) {
                ship.corporation.shipStartedRepairing.add(ship)
            } else if (ship.isPurchasing()) {
                ship.purchasingStarted = true
                ship.corporation.buyingShip = ship
            }
            // In case of restriction, the refueling ship might lose it's target
            // Look again for any ship on the tile
            refuelingShipBehavior(ship)
        }
    }

    private fun refuelingShipBehavior(ship: Ship) {
        if (ship.targetedShip == null) {
            val otherShip = ship.corporation.ships.filter {
                it.id != ship.id && it.fuel < Constants.HALF * it.maxFuel &&
                    !it.isDamaged
            }.minByOrNull { it.id }
            if (otherShip != null) {
                ship.targetedShip = otherShip
            }
        }
        // Now the refueling ship checks for the target ship
        ship.targetedShip?.let { targetedShip ->
            if (!targetedShip.refuelingFromStation &&
                targetedShip.refueledBy == null &&
                ship.refuelFuel >= targetedShip.maxFuel
            ) {
                ship.corporation.shipsRefuelStationStarted.remove(targetedShip)
                if (ship.corporation.buyingShip == targetedShip) {
                    ship.corporation.buyingShip = null
                    ship.corporation.decidedToPurchase = false
                }
                ship.task = null
                targetedShip.task = null
                ship.refuelingShip = targetedShip
                targetedShip.refueledBy = ship

                ship.refuelOtherStarted = true
                ship.refuelWaitingTime = ship.refuelTim
            }
        }
    }

    /**
     * Moves the ship to the closest tile without restriction if possible.
     */
    private fun escapeRestriction(ship: Ship) {
        ship.behaviour = Behaviour.ESCAPING
        val shipTile = oceanMap.getShipTile(ship)
        val path = pathFinder.escapeRestriction(shipTile)
        moveAlongPath(ship, path)
    }

    /**
     * Moves the ship towards the closest reachable harbour and sets the behaviour to refueling.
     * If the ship has a task, it immediately fails and is removed from the ship.
     */
    private fun moveToRefuel(ship: Ship) {
        ship.behaviour = Behaviour.REFUELING
        ship.task = null
        // If the ship is not already being refueled by refueling ship
        val shipsTile = oceanMap.getShipsOnTile(oceanMap.getShipTile(ship)).filter {
            it.id != ship.id && it.type == ShipType.REFUELING &&
                it.refuelingShip == null && it.corporation.id == ship.corporation.id
        }
        // Ship won't move if there's a refueling ship on the same tile that can refuel it
        if (shipsTile.isNotEmpty()) {
            ship.velocity = 0
            // task = null
            return
        }

        if (ship.refueledBy == null) {
            moveToRefuelHarbour(ship)
        }
    }

    /**
     * Moves the ship to nearest Shipyard Station for repairing
     * @param ship as Ship
     * @return Unit
     * */
    private fun moveToRepair(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val shipyardTiles = harbors.values.filter {
            it.shipyardStation != null
        }.map {
            oceanMap.getTileByID(it.location).getOrNull() ?: return
        }.toSet()
        ship.task = null
        val path = pathFinder.getShortestPathToTileStation(shipTile, shipyardTiles)
        moveAlongPath(ship, path)
    }

    /**
     * Moves the ship towards the closest reachable harbour and sets the behaviour to unloading.
     */
    private fun moveToUnload(ship: Ship) {
        ship.behaviour = Behaviour.UNLOADING
        moveToUnloadHarbour(ship)
    }

    /**
     * Moves the ship towards the closest reachable Refueling Station.
     * @param ship Ship
     * @return Unit
     * */
    private fun moveToRefuelHarbour(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        // Extracting the harbors with refueling station
        val refuelHarbors = harbors.values.filter {
            it.refuelingStation != null && it.refuelingStation.refuelTimes > 0
        }
        // Now getting all the tiles from those harbors
        val refuelTiles = refuelHarbors.map { oceanMap.getTileByID(it.location).getOrNull() ?: return }.toSet()
        val path = pathFinder.getShortestPathToTileStation(shipTile, refuelTiles)
        // ship.corporation.harbors not needed
        moveAlongPath(ship, path)
    }

    /**
     * Moves the ship towards the closest reachable Unloading Station.
     * @param ship Ship
     * @return Unit
     */
    private fun moveToUnloadHarbour(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)

        // Extracting the Corp Harbors with unloading station
        val unloadHarbors = harbors.values.filter {
            it.unloadingStation != null &&
                it.corporations.contains(ship.corporation.id)
        }

        val allowedGarbages = unloadHarbors.map { it.unloadingStation?.garbagesType ?: return }.flatten().toSet()

        // Getting the garbage type to be unloaded first
        val garbagesToUnload = ship.garbageCapacity.filter { it.value == 0 }
        var firstGarbageType: GarbageType = GarbageType.PLASTIC
        for (garbageType in GarbageType.entries) {
            if (garbageType in garbagesToUnload.keys &&
                garbageType in allowedGarbages
            ) {
                firstGarbageType = garbageType
                break
            }
        }

        val priorityHarbors = unloadHarbors.filter {
            it.unloadingStation?.garbagesType?.contains(firstGarbageType) ?: return
        }

        // Now getting all the tiles from those harbors
        val unloadTiles = priorityHarbors.map { oceanMap.getTileByID(it.location).getOrNull() ?: return }.toSet()

        val path = pathFinder.getShortestPathToTileStation(shipTile, unloadTiles) // ship.corporation.harbors not needed
        moveAlongPath(ship, path)
    }

    /**
     * Moves the ship towards the target destination of its task.
     * @throws IllegalArgumentException If the ship has no task.
     */
    private fun moveToTask(ship: Ship) {
        val shipTile = oceanMap.getShipTile(ship)
        val shipTask = ship.task ?: throw IllegalArgumentException("Ship has no task")
        val path = pathFinder.getShortestPathToTile(shipTile, setOf(shipTask.destination))
        if (path.isEmpty()) {
            // This means that the task destination was not reachable.
            // In this case the task failed, and we remove it from the ship.
            ship.task = null
            // The ship will move according to its default behaviour instead.
            // Since this includes unloading, we call moveShip() instead of moveShipDefault().
            return moveShip(ship, shipTile)
        }
        moveAlongPath(ship, path)
    }

    /**
     * Moves the coordinating ship to purchase the refueling ship, if the corporation can afford it
     * */
    private fun moveToPurchase(ship: Ship) {
        val (shipCost, path) = findMinCostOfPurchase(ship)
        // We check if the corp can still afford the ship
        if (path.isNotEmpty() && ship.corporation.credits >= shipCost) {
            moveAlongPath(ship, path)
        } else {
            ship.corporation.decidedToPurchase = false
            ship.behaviour = Behaviour.DEFAULT
            ship.goingToPurchase = false
            moveShipDefault(ship)
        }
    }

    /**
     * It calculates the minimum cost of the ship at nearest shipyard station
     * @param ship Ship
     * @return Pair<Int, List<Tile>> returns the pair of the cost and path to that station
     * */
    private fun findMinCostOfPurchase(ship: Ship): Pair<Int, List<Tile>> {
        val shipTile = oceanMap.getShipTile(ship)
        // Making a list of Corporation harbors having the Shipyard Station
        val corpShipyardHarbors = harbors.values.filter {
            it.corporations.contains(ship.corporation.id) &&
                it.shipyardStation != null
        }

        // Finding min cost of Refueling Ship among all Shipyard Stations belonging to that Corporation
        val minCost = corpShipyardHarbors.map {
            it.shipyardStation?.shipCost ?: return Pair(0, emptyList())
        }.minOrNull() ?: 0 // Should not be zero

        // Lowest ID Harbor having that minimum ship cost
        val minCostHarbor = corpShipyardHarbors.filter {
            (it.shipyardStation?.shipCost ?: return Pair(0, emptyList())) == minCost
        }.minByOrNull { it.id }

        if (minCostHarbor != null) {
            val targetTile = setOf(
                oceanMap.getTileByID(minCostHarbor.location)
                    .getOrNull() ?: return Pair(0, emptyList())
            )
            val path = pathFinder.getShortestPathToTileStation(shipTile, targetTile)
            return Pair(minCost, path)
        }
        return Pair(0, emptyList())
    }

    /**
     * Moves the ship according to its default behaviour.
     */
    private fun moveShipDefault(ship: Ship) {
        ship.behaviour = Behaviour.DEFAULT
        val path = when (ship.type) {
            ShipType.SCOUTING -> getScoutingPathDefault(ship)
            ShipType.COORDINATING -> getCoordinatingPath(ship)
            ShipType.COLLECTING -> getCollectingPathDefault(ship)
            ShipType.REFUELING -> getRefuelingPathDefault(ship)
        }
        moveAlongPath(ship, path)
        // If a collecting ship actually moves towards the selected garbage
        // and does not go refueling instead, we assign it to the tile of the selected garbage.
        if (ship.type == ShipType.COLLECTING &&
            ship.behaviour == Behaviour.DEFAULT &&
            path.isNotEmpty()
        ) {
            // path.last() is the location of the selected garbage
            assignShipToGarbageTile(ship, path.last())
        }
    }

    /**
     * Gives the default path of the refueling ship.
     * In case no ship with < 50% fuel, it'll return just an empty list
     * @param ship The Refueling Ship
     * @return Unit
     * */
    private fun getRefuelingPathDefault(ship: Ship): List<Tile> {
        val shipTile = oceanMap.getShipTile(ship)
        val otherShips = ship.corporation.ships.filter {
            it.fuel < Constants.HALF * it.maxFuel &&
                it.id != ship.id && !it.refuelingFromStation &&
                it.refueledBy == null &&
                ship.refuelFuel >= it.maxFuel &&
                !it.isDamaged
        }

        // val destTiles = otherShips.map { oceanMap.getShipTile(it) }.toSet()
        //
        val data = mutableMapOf<Tile, Collection<Ship>>()
        for (otherShip in otherShips) {
            val otherShipTile = oceanMap.getShipTile(otherShip)
            if (data.contains(otherShipTile)) {
                data[otherShipTile]?.plus(otherShip)
            } else {
                data[otherShipTile] = listOf(otherShip)
            }
        }

        val path = pathFinder.getShortestPathToShip(shipTile, data)
        // val path = pathFinder.getShortestPathToTileShip(shipTile, destTiles, ship.corporation.id)
        // val path = pathFinder.getShortestPathToShip(shipTile, )
        val targetedTile = path.lastOrNull() ?: return emptyList()
        val targetedShip = oceanMap.getShipsOnTile(targetedTile).filter { it in otherShips }
            .minByOrNull { it.id } ?: return emptyList()

        return if (ship.canRefuelShip(targetedShip)) {
            path
        } else {
            emptyList()
        }
    }

    /**
     * Returns a path according to the default behaviour of the given scouting ship.
     */
    private fun getScoutingPathDefault(ship: Ship): List<Tile> {
        val shipTile = oceanMap.getShipTile(ship)
        // First look for garbage in the ship's visibility range and
        // return the shortest path to the closest reachable garbage if there is one.
        val garbageInShipVisibility = visibilityHandler.getGarbageInShipVisibility(ship)
        if (garbageInShipVisibility.isNotEmpty()) {
            val path = pathFinder.getShortestPathToGarbage(shipTile, garbageInShipVisibility)
            if (path.isNotEmpty()) return path
        }
        // Then look for garbage in corporation's information and
        // return the shortest path to the closest reachable garbage if there is one.
        val garbageInCorpInformation = visibilityHandler.getGarbageInCorpInformation(ship)
        if (garbageInCorpInformation.isNotEmpty()) {
            val path = pathFinder.getShortestPathToGarbage(shipTile, garbageInCorpInformation)
            if (path.isNotEmpty()) return path
        }
        // Otherwise return a path according to the ship's exploring behaviour.
        ship.behaviour = Behaviour.EXPLORING
        return pathFinder.explore(shipTile, ship.getDistanceWithVelocity())
    }

    /**
     * Checks first, if the coordinating ship continue the purchase journey
     * @param ship as Ship
     * @return path as List<Tile>
     * */
    private fun getCoordinatingPath(ship: Ship): List<Tile> {
        // If the ship has already made the decision to purchase, we'll won't send other ships for it
        if (ship.corporation.decidedToPurchase) {
            return getCoordinatingPathDefault(ship)
        }
        val (shipCost, path) = findMinCostOfPurchase(ship)

        // Checking the affordability
        if (ship.corporation.credits >= shipCost && path.isNotEmpty()) {
            ship.corporation.decidedToPurchase = true
            ship.behaviour = Behaviour.PURCHASING
            ship.goingToPurchase = true
            return path
        } else {
            return getCoordinatingPathDefault(ship)
        }
    }

    /**
     * Returns a path according to the default behaviour of the given coordinating ship.
     */
    private fun getCoordinatingPathDefault(ship: Ship): List<Tile> {
        val shipTile = oceanMap.getShipTile(ship)

        // Look for ships the given ship can cooperate with in the corporation's visibility range
        val relevantShips = visibilityHandler.getShipsInCorpVisibility(ship)
            .mapValues { it.value.filter { otherShip -> ship.canCooperateWith(otherShip) } }
            .filter { it.value.isNotEmpty() }

        if (relevantShips.isNotEmpty()) {
            val path = pathFinder.getShortestPathToShip(shipTile, relevantShips)
            if (path.isNotEmpty()) return path
        }

        // Default to exploring behavior
        ship.behaviour = Behaviour.EXPLORING
        return pathFinder.explore(shipTile, ship.getDistanceWithVelocity())
    }

    /**
     * Returns a path according to the default behaviour of the given collecting ship.
     */
    private fun getCollectingPathDefault(ship: Ship): List<Tile> {
        val shipTile = oceanMap.getShipTile(ship)
        // Look for garbage in the corporation's visibility range.
        val garbageInCorpVisibility = visibilityHandler.getGarbageInCorpVisibility(ship)
        // Filter for garbage types the given ship can collect.
        val garbageWithMatchingTypes = garbageInCorpVisibility
            .mapValues { it.value.filter { garbage -> garbage.type in ship.maxGarbageCapacity.keys } }
            .filter { it.value.isNotEmpty() }
        // Filter for tiles where the corporation hasn't already sent enough ships
        // to collect all the garbage on that tile.
        val assignedShipsPerTile = ship.corporation.assignedShipsPerTile
        val relevantGarbage = garbageWithMatchingTypes
            .filter { notEnoughShipsAssigned(it.value, assignedShipsPerTile[it.key].orEmpty()) }
        // If there is relevant garbage, return the shortest path to the closest reachable garbage.
        if (relevantGarbage.isNotEmpty()) {
            return pathFinder.getShortestPathToGarbage(shipTile, relevantGarbage)
        }
        // Otherwise don't move.
        return emptyList()
    }

    /**
     * Checks if corporation hasn't already sent enough ships
     * to collect all the garbage on the tile.
     */
    private fun notEnoughShipsAssigned(garbageOnTile: Collection<Garbage>, ships: Collection<Ship>): Boolean {
        // Sums of garbage amounts per garbage type on the tile
        val garbageSumsPerType = SumsPerTypeUtil.getGarbageSumsPerType(garbageOnTile)
        // Sums of ship capacities per garbage type on the tile
        val capacitySumsPerType = SumsPerTypeUtil.getCapacitySumsPerType(ships)
        // Return true if any garbage type doesn't already have enough ships assigned
        return garbageSumsPerType.any { (garbageType, garbageSum) ->
            garbageSum > (capacitySumsPerType[garbageType] ?: 0)
        }
    }

    /**
     * Assigns the ship to the given tile in the corporation's assignedShipsPerTile map.
     */
    private fun assignShipToGarbageTile(ship: Ship, tile: Tile) {
        ship.corporation.assignedShipsPerTile
            .getOrPut(tile) { mutableListOf() }
            .add(ship)
    }
}
