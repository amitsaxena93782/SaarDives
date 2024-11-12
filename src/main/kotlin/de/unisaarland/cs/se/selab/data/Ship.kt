package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.RewardType
import de.unisaarland.cs.se.selab.enums.ShipType
import de.unisaarland.cs.se.selab.task.Task
import kotlin.math.min

/**
 * The Ship class
 */
class Ship(
    val id: Int,
    val type: ShipType,
    val corporation: Corporation,
    var maxVelocity: Int,
    var acceleration: Int,
    val maxFuel: Int,
    var fuelConsumption: Int,
    var visibilityRange: Int,
    val maxGarbageCapacity: MutableMap<GarbageType, Int>
) : Comparable<Ship> {
    // val fuelConsumptionPerTile = fuelConsumption * Constants.TILE_DISTANCE
    var velocity = 0
    val reward = mutableListOf<RewardType>() // Changed this field to var for damaging
    var fuel = maxFuel
    var task: Task? = null
    var behaviour = Behaviour.DEFAULT
    val garbageCapacity = maxGarbageCapacity.toMutableMap()
    var waitingAtHarbor = false

    // Some additional Props for normal ships
    var refuelingFromShip = false
    var refuelWaitingTime = -1
    var refuelingShip: Ship? = null // Now contains the ship to be refueled
    var refueledBy: Ship? = null // Contains the which from which it'S being refueled
    var goingToPurchase = false
    var refuelingFromStation = false // Will act as a switch for logging
    var targetedShip: Ship? = null // Ship which is being targeted to refuel
    var targetedBy: Ship? = null
    var refuelingOwnCap = false
    var refuelOtherStarted = false

    // var repairingStarted = false not required
    var purchasingStarted = false
    var targetHarbor: Harbor? = null // Stores the target harbor
    var isDamaged: Boolean = false
    var orginalAccel: Int? = null
    var originalMaxVel: Int? = null
    var usedVisibilityRange: Int = visibilityRange

    // Some additional attributes for Refueling Ship
    var refuelCap = 0
    var refuelTim = 0
    var serviceAvailable = true
    var refuelRefuelCap = false
    var refuelFuel = 0

    /**
     * Accelerates the ship and increases its current velocity
     */
    fun accelerate() {
        velocity = min(velocity + acceleration, maxVelocity)
    }

    /**
     * Checks if ship can attach tracker
     */
    fun canAttachTracker(): Boolean {
        return RewardType.TRACKER in reward && !isRefueling()
    }

    /**
     * Checks if ship can collect garbage
     */
    fun canCollect(): Boolean {
        return garbageCapacity.any { it.value > 0 } && !isRefueling()
    }

    /**
     * Checks if the ship can cooperate with another ship.
     */
    fun canCooperateWith(otherShip: Ship): Boolean {
        val canCooperate = type == ShipType.COORDINATING || reward.contains(RewardType.RADIO)
        val fromDifferentCorporation = otherShip.corporation != this.corporation
        val notLastCooperatedWith = otherShip.corporation != this.corporation.lastCooperated
        return canCooperate && fromDifferentCorporation && notLastCooperatedWith &&
            !refuelingFromStation && refueledBy == null
    }

    /**
     * Returns if the ship is refueling during this tick
     */
    fun isRefueling(): Boolean {
        return behaviour == Behaviour.REFUELING
    }

    /**
     * Returns if the ship is unloading during this tick
     */
    fun isUnloading(): Boolean {
        return waitingAtHarbor && behaviour == Behaviour.UNLOADING
    }

    /**
     * Returns if the ship is repairing during this tick
     */
    fun isRepairing(): Boolean {
        return behaviour == Behaviour.DAMAGED
    }

    /**
     * Returns if the ship is ready to purchase during this tick
     */
    fun isPurchasing(): Boolean {
        return behaviour == Behaviour.PURCHASING
    }

    /**
     * Number of tiles reachable with current velocity
     */
    fun getDistanceWithVelocity(velocity: Int = this.velocity): Int {
        return velocity / Constants.TILE_DISTANCE
    }

    /**
     * Number of tiles reachable with current fuel
     */
    fun getDistanceWithFuel(fuel: Int = this.fuel): Int {
        return fuel / (fuelConsumption * Constants.TILE_DISTANCE)
    }

    override fun compareTo(other: Ship): Int {
        return id.compareTo(other.id)
    }

    /**
     * Sent by the Refueling Ship to check whether it can refuels the given ship.
     * If yes, it couples itself with it.
     * @param ship which needs refueling
     * @return true if the ship can be refueled, false otherwise
     * */
    fun canRefuelShip(ship: Ship): Boolean {
        if (refuelFuel >= ship.maxFuel) {
            // ship.targetedBy = this // No need to target now
            targetedShip = ship
            // ship.corporation.refuelingOtherShips[this] = ship // might be needed
            return true
        }
        refuelingOwnCap = true
        behaviour = Behaviour.REFUELING
        return false
    }

    /**
     * Refueling Ship completes the refueling of the other ship
     * after the waiting time has been passed
     * @return Unit
     * */
    fun completeRequest() {
        // First reducing the refueling fuel from the refueling ship
        refuelingShip?.let {
            refuelFuel -= it.maxFuel
            it.fuel = it.maxFuel
            behaviour = Behaviour.DEFAULT
            it.behaviour = Behaviour.DEFAULT
        }
        // Finally decoupling it from the refueling ship
        decoupleShips()
    }

    /**
     * Decouples the ships in case of any ship drift or any events
     * @return Unit
     * */
    fun decoupleShips() {
        if (refueledBy != null) {
            refueledBy?.refuelingShip?.targetedShip = null
            refueledBy?.refuelingShip = null
            refueledBy = null
        } else {
            refuelingShip?.refueledBy = null
            refuelingShip?.refueledBy?.targetedBy = null
            refuelingShip = null
            targetedShip = null
        }
    }
}
