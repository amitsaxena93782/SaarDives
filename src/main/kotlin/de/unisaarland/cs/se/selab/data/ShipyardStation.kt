package de.unisaarland.cs.se.selab.data
/**
 * This class represents the Shipyard Station of a harbor
 * */
class ShipyardStation(
    val repairCost: Int,
    val shipCost: Int,
    val deliveryTime: Int,
    val shipProp: ShipProp
) : Station() {

    /** This method repairs the ship which arrives at the harbor
     * @param Ship
     * @return Result<Unit>
     * */
    public fun repairShip(ship: Ship): Result<Unit> {
        ship
        return Result.success(Unit)
    }

    /** This method used to purchase a refueling ship
     * @param Ship
     * @return Result<Unit>
     * */
    public fun purchase(ship: Ship): Result<Unit> {
        ship
        return Result.success(Unit)
    }
}
