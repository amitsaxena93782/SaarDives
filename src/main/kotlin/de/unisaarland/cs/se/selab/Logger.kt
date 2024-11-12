package de.unisaarland.cs.se.selab

import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType
import de.unisaarland.cs.se.selab.enums.TaskType
import java.io.PrintWriter

/**
 * The Logger object
 */
object Logger {
    private val collectedGarbage = mutableMapOf<Int, Int>()
    var printer = PrintWriter(System.out, true)
    private var plastic = 0
    private var oil = 0
    private var chemical = 0

    /**
     * Set path to output file, if specified. This will change output mode to "FILE".
     */

    fun setFilePath(filePathStr: String) {
        printer = PrintWriter(filePathStr)
    }

    /**
     * Log singular parsing result.
     */
    fun logParsingResult(success: Boolean, filename: String) {
        var outputLine = "Initialization Info:"
        outputLine += if (success) {
            " $filename successfully parsed and validated."
        } else {
            " $filename is invalid."
        }
        printer.println(outputLine)
        printer.flush()
    }

    /**
     * Initialize the Map for every corporation with amount 0
     */
    fun initializeCorpGarbage(corpIds: List<Int>) {
        for (corpId in corpIds) {
            collectedGarbage[corpId] = 0
        }
    }

    /**
     * Log start of simulation.
     */
    fun logSimStart() {
        printer.println("Simulation Info: Simulation started.")
        printer.flush()
    }

    /**
     * Log start of a tick.
     */
    fun logTickStart(tick: Int) {
        val outputLine = "Simulation Info: Tick $tick started."
        printer.println(outputLine)
        printer.flush()
    }

    /**
     * Log start of a move for corporation.
     */
    fun logStartMove(corporationId: Int) {
        printer.println("Corporation Action: Corporation $corporationId is starting to move its ships.")
        printer.flush()
    }

    /**
     * Log ship movement to certain tile.
     */
    fun logShipMove(shipId: Int, speed: Int, tileId: Int) {
        if (speed >= Constants.TILE_DISTANCE) {
            printer.println("Ship Movement: Ship $shipId moved with speed $speed to tile $tileId.")
            printer.flush()
        }
    }

    /**
     * Log start of garbage collection for a corporation
     */
    fun logCorpCollectGarbage(corporationId: Int) {
        printer.println("Corporation Action: Corporation $corporationId is starting to collect garbage.")
        printer.flush()
    }

    /**
     * Log use of tracker from a ship.
     */
    fun logAttachTracker(corporationId: Int, garbageId: Int, shipId: Int) {
        printer.println(
            "Corporation Action: Corporation $corporationId attached tracker to garbage $garbageId " +
                "with ship $shipId."
        )
        printer.flush()
    }

    /**
     * Log how much garbage collected by a ship.
     */
    fun logCollectGarbage(shipId: Int, amount: Int, garbageId: Int, garbageType: GarbageType, corporationId: Int) {
        when (garbageType) {
            GarbageType.PLASTIC -> plastic += amount
            GarbageType.OIL -> oil += amount
            GarbageType.CHEMICALS -> chemical += amount
        }
        collectedGarbage.compute(corporationId) { _, v -> v?.plus(amount) ?: amount }
        printer.println(
            "Garbage Collection: Ship $shipId collected $amount of garbage $garbageType " +
                "with $garbageId."
        )
        printer.flush()
    }

    /**
     * Log start of cooperation of a corporation
     */
    fun logCoopStart(corporationId: Int) {
        printer.println(
            "Corporation Action: Corporation $corporationId " +
                "is starting to cooperate with other corporations."
        )
        printer.flush()
    }

    /**
     * Log cooperation between two corporation according to their respective ship.
     */
    fun logCoopWith(corporationA: Int, corporationB: Int, shipA: Int, shipB: Int) {
        printer.println(
            "Cooperation: Corporation $corporationA cooperated with corporation $corporationB with ship $shipA " +
                "to ship $shipB."
        )
        printer.flush()
    }

    /**
     * Log start of refueling of a ship.
     */
    fun logRefuelStart(corporationId: Int) {
        printer.println("Corporation Action: Corporation $corporationId is starting to refuel.")
        printer.flush()
    }

    /**
     * Log start of refueling of a ship from the station.
     * @param shipId ShipID of ship to be refueled
     * @param harborId HarborID of the station
     * @param success whether the refueling was successful
     * @param cost the cost of refueling
     * @param refuelingShip if the ship wants to refuel its own refueling capacity, doesn't matter in case of failure
     * @return Unit
     */
    fun logRefuelingFromStationStarted(
        shipId: Int,
        harborId: Int,
        success: Boolean,
        cost: Int?,
        refuelingShip: Boolean
    ) {
        if (success && refuelingShip) {
            printer.println(
                "Refueling: Ship $shipId starts to fill " +
                    "its refuel capacity at harbor $harborId and paid ${cost ?: ""} credits."
            )
        } else if (success) {
            printer.println(
                "Refueling: Ship $shipId starts to refuel at harbor $harborId and paid ${cost ?: ""} credits."
            )
        } else {
            printer.println("Refueling: Ship $shipId cannot refuel at harbor $harborId.")
        }
        printer.flush()
    }

    /**
     * Logs when the refueling has been done
     * @param shipID Ship ID of the ship
     * @param harborID Harbor ID from where the ship is being refueled
     * @return Unit
     * */
    fun logRefuelingDone(shipID: Int, harborID: Int) {
        printer.println("Refueling: Ship $shipID refueled at harbor $harborID.")
        printer.flush()
    }

    /**
     * Logs the refueling of ship from other ship
     * @param shipID Refueling Ship ID
     * @param otherShipID Ship ID of the ship to be refueled
     * @param started true if the refueling started, false for completion
     * */
    fun logRefuelFromShip(shipID: Int, otherShipID: Int, started: Boolean) {
        if (started) {
            printer.println("Refueling: Ship $shipID started to refuel ship $otherShipID.")
        } else {
            printer.println("Refueling: Ship $shipID finished refueling ship $otherShipID.")
        }
        printer.flush()
    }

    /**
     * Log unloading of garbage from a ship.
     */
    fun logUnload(shipId: Int, amount: Int, garbageType: GarbageType, harborId: Int, returnAmount: Int) {
        printer.println(
            "Unload: Ship $shipId unloaded $amount of garbage " +
                "$garbageType at harbor $harborId and received $returnAmount credits."
        )
        printer.flush()
    }

    /**
     * Log the repairing of ship.
     * @param shipID Ship ID of the ship
     * @param harborID Harbor ID of the Shipyard Station
     * @param amount Repairing Cost
     * @param started true if started, false otherwise
     * @return Unit
     * */
    fun logRepair(shipID: Int, harborID: Int?, amount: Int?, started: Boolean) {
        if (started) {
            printer.println(
                "Repair: Ship $shipID is being repaired at harbor ${harborID ?: ""} for ${amount ?: ""} credits."
            )
        } else {
            printer.println("Repair: Ship $shipID is repaired.")
        }
        printer.flush()
    }

    /**
     * Logs the purchase of the refueling ship
     * @param shipID ShipID that places the order
     * @param refuelShipID ShipID of the new ordered ship
     * @param harborID HarborID of the station
     * @param amount Cost of the ship
     * @param corpID CorporationID of the corporation that ordered the ship
     * @param tileID TileID where the ship will be spawned
     * @param started True if purchased, false if delivered
     * @return Unit
     * */
    fun logShipPurchase(
        shipID: Int,
        refuelShipID: Int,
        harborID: Int,
        amount: Int,
        corpID: Int,
        tileID: Int,
        started: Boolean,
    ) {
        if (started) {
            printer.println(
                "Purchase: Ship $shipID ordered a " +
                    "refueling ship with id $refuelShipID at harbor $harborID for $amount credits."
            )
        } else {
            printer.println("Purchase: Ship $refuelShipID delivered to corporation $corpID at $tileID.")
        }
        printer.flush()
    }

    /**
     * Log end of corporation action for that tick.
     */
    fun logFinishAction(corporationId: Int) {
        printer.println("Corporation Action: Corporation $corporationId finished its actions.")
        printer.flush()
    }

    /**
     * Log garbage drifting from currents.
     */
    fun logGarbageDrift(
        garbageType: GarbageType,
        garbageId: Int,
        amount: Int,
        startTileId: Int,
        endTileId: Int
    ) {
        printer.println(
            "Current Drift: $garbageType $garbageId with amount $amount drifted from tile " +
                "$startTileId to tile $endTileId."
        )
        printer.flush()
    }

    /**
     * Log ship drifting from currents.
     */
    fun logShipDrift(shipId: Int, startTileId: Int, endTileId: Int) {
        printer.println("Current Drift: Ship $shipId drifted from tile $startTileId to tile $endTileId.")
        printer.flush()
    }

    /**
     * Log event trigger.
     */
    fun logEvent(eventId: Int, eventType: String) {
        printer.println("Event: Event $eventId of type $eventType happened.")
        printer.flush()
    }

    /**
     * Log task assignment.
     */
    fun logTask(taskId: Int, taskType: TaskType, shipId: Int, destinationId: Int) {
        printer.println(
            "Task: Task $taskId of type $taskType with ship $shipId is added with destination " +
                "$destinationId."
        )
        printer.flush()
    }

    /**
     * Log reward distribution.
     */
    fun logReward(taskId: Int, shipId: Int, reward: RewardType) {
        printer.println("Reward: Task $taskId: Ship $shipId received reward of type $reward.")
        printer.flush()
    }

    /**
     * Log end of simulation.
     */
    fun logSimEnd() {
        printer.println("Simulation Info: Simulation ended.")
        printer.flush()
    }

    /**
     * Log statistics.
     */
    fun logStatistics(leftoverGarbage: Int) {
        printer.println("Simulation Info: Simulation statistics are calculated.")
        printer.flush()
        for (corporationId in collectedGarbage.keys.sorted()) {
            printer.println(
                "Simulation Statistics: Corporation $corporationId " +
                    "collected ${collectedGarbage.getValue(corporationId)} of garbage."
            )
            printer.flush()
        }
        printer.flush()
        printer.println("Simulation Statistics: Total amount of plastic collected: $plastic.")
        printer.flush()
        printer.println("Simulation Statistics: Total amount of oil collected: $oil.")
        printer.flush()
        printer.println("Simulation Statistics: Total amount of chemicals collected: $chemical.")
        printer.flush()
        printer.println("Simulation Statistics: Total amount of garbage still in the ocean: $leftoverGarbage.")
        printer.flush()
    }

    // Some other additional methods
    /**
     * Log Refueling Ship refuels other ship
     * */
    fun logRefuelingOtherShips(refuelShipID: Int, otherShipID: Int, completed: Boolean) {
        if (completed) {
            printer.println("Refueling: Ship $refuelShipID started to refuel ship $otherShipID.")
            printer.flush()
        } else {
            printer.println("Refueling: Ship $refuelShipID finished refueling ship $otherShipID.")
            printer.flush()
        }
    }

    /**
     * Logs the damage by Typhoon
     * */
    fun logTyphoonDamage(eventID: Int, tileID: Int, radius: Int, shipIDs: List<Int>) {
        val shipIdsString = shipIDs.joinToString(", ")
        printer.println("Event: Typhoon $eventID at tile $tileID with radius $radius affected ships: $shipIdsString.")
        printer.flush()
    }
}
