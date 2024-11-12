package de.unisaarland.cs.se.selab.parser

import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.data.Harbor
import de.unisaarland.cs.se.selab.data.RefuelingStation
import de.unisaarland.cs.se.selab.data.ShipProp
import de.unisaarland.cs.se.selab.data.ShipyardStation
import de.unisaarland.cs.se.selab.data.UnloadingStation
import de.unisaarland.cs.se.selab.enums.GarbageType
import org.json.JSONArray
import org.json.JSONObject

/**
 * This class is reponsible for parsing the complete harbors
 * @constructor HarborParser(val simulationData: SimulationData)
 * @return HarborParser
 * */
class HarborParser(val simulationData: SimulationData) {

    private val parsedIDs: List<Int> = emptyList()
    private val parsedLocations: List<Int> = emptyList()

    /**
     * This main method parses the harbor JSON Array completely
     * @param JSONArray
     * @return MutableMap<Int, Harbor>
     * */
    public fun parseHarbors(harborsJSONArray: JSONArray): Result<Unit> {
        harborsJSONArray.forEach { harborJSON ->
            if (harborJSON is JSONObject) {
                val harborObject = validateAndCreateHarbor(harborJSON).getOrElse { return Result.failure(it) }

                simulationData.harbors[harborObject.id] = harborObject // updating the simulation data
                parsedIDs.plus(harborObject.id) // add the id to the parsedIDs
                parsedLocations.plus(harborObject.location) // add the location to the parsed Locations
            } else {
                return Result.failure(ParserException("Harbor is not a JSONObject."))
            }
        }
        return Result.success(Unit)
    }

    /**
     * This helper function validates and create each harbor
     * @param harbor as JSONObject
     * @return Result<Harbor>
     * */
    private fun validateAndCreateHarbor(harbor: JSONObject): Result<Harbor> {
        try {
            val id = harbor.getInt(JsonKeys.ID)
            // check if the id is unique
            require(id !in parsedIDs && id >= 0) {
                "Invalid or duplicate id in Harbor $id."
            }

            val location = harbor.getInt(JsonKeys.LOCATION)
            // check if the location isn't duplicate
            require(location !in parsedLocations && location >= 0) {
                "Duplicate or invalid locations of Harbor $id at Location $location."
            }

            // Checking if the harbor is actually present on that location
            require(simulationData.tiles.get(location)?.harborID == id) {
                "Harbor $id actually not present on that location!"
            }

            val corporationsJSONArray = harbor.getJSONArray(JsonKeys.CORPORATIONS)

            // Check that there should be at least one corp associated to the harbor
            require(corporationsJSONArray != null && !corporationsJSONArray.isEmpty) {
                "Harbor $id without a corp associated to it"
            }

            // Convert Corporation JSONArray to Array<Int>
            val corporationsIDArray = IntArray(corporationsJSONArray.length()) { i ->
                corporationsJSONArray.getInt(i)
            }

            // Validating and creating stations
            val shipyardStation = validateAndCreateShipyardStation(
                harbor.optJSONObject(JsonKeys.SHIPYARD_STATION)
            )?.getOrThrow()
            val refuelingStation = validatesAndCreateRefuelingStation(
                harbor.optJSONObject(JsonKeys.REFUEL_STATION)
            )?.getOrThrow()
            val unloadingStation = validatesAndCreateUnloadingStation(
                harbor.optJSONObject(JsonKeys.UNLOAD_STATION)
            )?.getOrThrow()

            // Ensure at most 2 stations and they are of different types
            val stations = listOfNotNull(shipyardStation, refuelingStation, unloadingStation)
            require(stations.size in Constants.MIN_STATIONS..Constants.MAX_STATIONS) {
                "Harbor $id has more than 2 stations."
            }

            // Check that stations are of different types
            val stationTypes = stations.map { it::class }.toSet()
            require(stationTypes.size == stations.size) {
                "Harbor $id has multiple stations of the same type."
            }

            return Result.success(
                Harbor(
                    id,
                    location,
                    corporationsIDArray,
                    shipyardStation,
                    refuelingStation,
                    unloadingStation
                )
            )
        } catch (e: IllegalArgumentException) {
            return Result.failure(ParserException(e.message ?: "Unknown error occurred in validateAndCreateHarbor."))
        }
    }

    /**
     * This helper function validates and creates the Shipyard Station
     * @param station as JSONObject
     * @return Result<ShipyardStation>
     * */
    private fun validateAndCreateShipyardStation(station: JSONObject?): Result<ShipyardStation>? {
        if (station == null) return null
        val repairCost = station.getInt(JsonKeys.REPAIR_COST)
        val shipCost = station.getInt(JsonKeys.SHIP_COST)
        val deliveryTime = station.getInt(JsonKeys.DELIVERY_TIME)
        val shipProp = validateAndCreateShipProp(station.getJSONObject(JsonKeys.SHIP_PROP)).getOrElse {
            return Result.failure(it)
        }

        try {
            require(repairCost in Constants.MIN_REPAIR_COST..Constants.MAX_REPAIR_COST) {
                "Invalid Repair cost of Shipyard Station"
            }
            require(shipCost in Constants.MIN_SHIP_COST..Constants.MAX_SHIP_COST) {
                "Invalid Refueling Ship cost of Shipyard Station"
            }
            require(deliveryTime in Constants.MIN_DELIVERY_TIME..Constants.MAX_DELIVERY_TIME) {
                "Invalid Delivery Time of Refueling Ship cost of Shipyard Station"
            }

            return Result.success(
                ShipyardStation(repairCost, shipCost, deliveryTime, shipProp)
            )
        } catch (e: IllegalArgumentException) {
            return Result.failure(ParserException(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * This helper function validates and creates Refueling Station
     * @param station as JSONObject
     * @return Result<RefuelingStation>
     * */
    private fun validatesAndCreateRefuelingStation(station: JSONObject?): Result<RefuelingStation>? {
        if (station == null) return null
        val refuelCost = station.getInt(JsonKeys.REFUEL_COST)
        val refuelTimes = station.getInt(JsonKeys.REFUEL_TIMES)

        if (refuelCost < Constants.MIN_REFUEL_COST || refuelCost > Constants.MAX_REFUEL_COST) {
            return Result.failure(
                ParserException("Invalid Refuel cost of the station $station")
            )
        }
        if (refuelTimes < Constants.MIN_REFUEL_TIMES || refuelTimes > Constants.MAX_REFUEL_TIMES) {
            return Result.failure(
                ParserException("Invalid Refuel times of the station $station")
            )
        }

        return Result.success(
            RefuelingStation(refuelCost, refuelTimes)
        )
    }

    /**
     * This helper function validates and create Unloading Station
     * @param station as JSONObject
     * @return Result<UnloadingStation>
     **/
    private fun validatesAndCreateUnloadingStation(station: JSONObject?): Result<UnloadingStation>? {
        if (station == null) return null
        val unloadReturn = station.getInt(JsonKeys.UNLOAD_RETURN)
        val garbagesTypesJSON = station.getJSONArray(JsonKeys.GARBAGE_TYPES)
        val garbagesTypes = garbagesTypesJSON.map { GarbageType.valueOf(it.toString()) }

        val allowedGarbages = listOf(GarbageType.PLASTIC, GarbageType.OIL, GarbageType.CHEMICALS)

        if (garbagesTypes.toSet().size != garbagesTypes.size) {
            return Result.failure(ParserException("GarbageType contains duplicates"))
        }

        for (garbage in garbagesTypes) {
            if (garbage !in allowedGarbages) return Result.failure(ParserException("Invalid Garbage $garbage"))
        }

        if (unloadReturn < Constants.MIN_UNLOAD_RETURN || unloadReturn > Constants.MAX_UNLOAD_RETURN) {
            return Result.failure(
                ParserException("Invalid Unload return of the station $station")
            )
        }

        return Result.success(
            UnloadingStation(unloadReturn, garbagesTypes)
        )
    }

    /**
     * This helper function validates and creates Refueling Ship Properties
     * @param refuelingShip as JSONObject
     * @return Result<ShipProp>
     * */
    private fun validateAndCreateShipProp(refuelShipJSON: JSONObject): Result<ShipProp> {
        // First checking if the JSON File is empty?
        if (refuelShipJSON.isEmpty) return Result.failure(ParserException("Ship Prop JSON file is empty!"))

        // Then extracting all the data from the JSON File
        val maxVel = refuelShipJSON.getInt(JsonKeys.MAX_VELOCITY)
        val acceleration = refuelShipJSON.getInt(JsonKeys.ACCELERATION)
        val fuelCap = refuelShipJSON.getInt(JsonKeys.FUEL_CAPACITY)
        val fuelConsum = refuelShipJSON.getInt(JsonKeys.FUEL_CONSUMPTION)
        val refuelCap = refuelShipJSON.getInt(JsonKeys.REFUEL_CAPACITY)
        val refuelTime = refuelShipJSON.getInt(JsonKeys.REFUEL_TIME)

        // Now comes the validation part of the properties
        validateShipProp(maxVel, acceleration, fuelCap, fuelConsum, refuelCap, refuelTime).onFailure {
            return Result.failure(it)
        }

        return Result.success(
            ShipProp(
                maxVel,
                acceleration,
                fuelCap,
                fuelConsum,
                refuelCap,
                refuelTime
            )
        )
    }

    /**
     * This helper function validates the Refueling Ship Properties
     * @param maxVelocity as Int
     * @param acceleration as Int
     * @param fuelCapacity as Int
     * @param fuelConsumption as Int
     * @param refuelCapacity as Int
     * @param refuelTime as Int
     * @return Result<Unit>
     * */
    private fun validateShipProp(
        maxVelocity: Int,
        acceleration: Int,
        fuelCapacity: Int,
        fuelConsumption: Int,
        refuelCapacity: Int,
        refuelTime: Int
    ): Result<Unit> {
        try {
            require(
                maxVelocity >= Constants.MIN_REFUEL_SHIP_VELOCITY &&
                    maxVelocity <= Constants.MAX_REFUEL_SHIP_VELOCITY
            ) {
                "Invalid Max velocity for the refueling ship"
            }
            require(
                acceleration >= Constants.MIN_REFUEL_SHIP_ACCELERATION &&
                    acceleration <= Constants.MAX_REFUEL_SHIP_ACCELERATION
            ) {
                "Invalid Acceleration for the refueling ship"
            }
            require(
                fuelCapacity >= Constants.MIN_REFUEL_SHIP_FUEL_CAP &&
                    fuelCapacity <= Constants.MAX_REFUEL_SHIP_FUEL_CAP
            ) {
                "Invalid FuelCapacity for the refueling ship"
            }
            require(
                fuelConsumption >= Constants.MIN_REFUEL_SHIP_FUEL_CONSUM &&
                    fuelConsumption <= Constants.MAX_REFUEL_SHIP_FUEL_CONSUM
            ) {
                "Invalid FuelConsumption for the refueling ship"
            }
            require(
                refuelCapacity >= Constants.MIN_REFUEL_SHIP_REFUEL_CAP &&
                    refuelCapacity <= Constants.MAX_REFUEL_SHIP_REFUEL_CAP
            ) {
                "Invalid Refueling capacity for the refueling ship"
            }
            require(
                refuelTime >= Constants.MIN_REFUEL_SHIP_REFUEL_TIME &&
                    refuelTime <= Constants.MAX_REFUEL_SHIP_REFUEL_TIME
            ) {
                "Invalid Refueling time for the refueling ship"
            }
        } catch (e: NumberFormatException) {
            return Result.failure(
                ParserException(e.message ?: "Unknown error in parsing the ShipProps of Refueling ship!")
            )
        }
        return Result.success(Unit)
    }
}
