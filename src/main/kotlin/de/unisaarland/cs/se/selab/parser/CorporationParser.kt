package de.unisaarland.cs.se.selab.parser

import com.github.erosb.jsonsKema.FormatValidationPolicy
import com.github.erosb.jsonsKema.JsonParser
import com.github.erosb.jsonsKema.JsonValue
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.ValidationFailure
import com.github.erosb.jsonsKema.Validator
import com.github.erosb.jsonsKema.ValidatorConfig
import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.control.PathFinder
import de.unisaarland.cs.se.selab.data.Corporation
import de.unisaarland.cs.se.selab.data.Harbor
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.GarbageType
import de.unisaarland.cs.se.selab.enums.ShipType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Parser for the corporation config file.
 */
class CorporationParser(private val jsonString: String, private var simulationData: SimulationData) {
    private val oceanMap = simulationData.oceanMap
        ?: throw ParserException("Ocean Map didn't exist when parsing corporations.") // map parser created it already
    private val ships = mutableMapOf<Int, Ship>() // map from shipID to ships
    private val shipLocations = mutableMapOf<Int, Int>() // map from shipID to tileID
    private val corporationsById = mutableMapOf<Int, Corporation>() // map form corpID to CurrentInfo
    private val garbageTypes = mutableMapOf<Int, MutableSet<GarbageType>>() // map from corp to garbage types
    private val corpToClaimedShips = mutableMapOf<Int, MutableSet<Int>>() // map from corpIDs to shipIDs that they claim
    private val shipNames = mutableSetOf<String>() // set of ship names, used for validation of uniqueness
    private val corpNames = mutableSetOf<String>() // set of corp names, used for validation of uniqueness
    private val claimedHarbors = mutableSetOf<Tile>() // set of claimed harbors, used for validation
    private val pathFinder = PathFinder(oceanMap)

    /**
     * Parses the config
     * @return if config is valid or not
     */
    internal fun parse(): Result<Unit> {
        val schema = SchemaLoader.forURL("classpath://schema/corporations.schema").load()

        // validate schema
        val validator = Validator.create(schema, ValidatorConfig(FormatValidationPolicy.ALWAYS))

        try {
            val jsonInstance: JsonValue = JsonParser(jsonString).parse()
            val failure: ValidationFailure? = validator.validate(jsonInstance)
            require(failure == null) { "Corp JSON doesn't match the schema" }
            /*if (failure != null) {
                return Result.failure(ParserException(failure.message))
            }*/
            val json = JSONObject(jsonString)

            // parse corps
            val corporationsJson = json.getJSONArray(JsonKeys.CORPORATIONS)
            val corporationsParsingRes = parseCorporations(corporationsJson)
            corporationsParsingRes.onFailure { return corporationsParsingRes }

            // add corporations to simulationData
            simulationData.corporations.putAll(corporationsById)

            // Cross Validation in Corporations and Harbors
            simulationData.harbors.values.forEach { harbor ->
                require(validateHarborUsage(harbor).isSuccess)
            }

            // parse ships
            val shipsJson = json.getJSONArray(JsonKeys.SHIPS)
            val shipParsingRes = parseShips(shipsJson)
            shipParsingRes.onFailure { return shipParsingRes }

            // add ships to oceanMap
            for ((shipID, ship) in ships) {
                val shipLoc = shipLocations.getValue(shipID)
                val tile = oceanMap.getTileByID(shipLoc).getOrNull()
                // ship locations are already validated and exist
                requireNotNull(tile) { "Ship's Tile isn't found in CorpParser" }
                oceanMap.addShip(ship, tile)
            }
            simulationData.ships.putAll(ships)
            Constants.MAX_SHIP_ID = ships.keys.max()

            // sort ships
            sortShips()

            // check if corp has necessary ships for its garbage types
            for (corp in corporationsById.keys) {
                val garbageTypeValidationRes = hasShipForGarbageTypes(corp)
                garbageTypeValidationRes.onFailure { return garbageTypeValidationRes }
            }

            require(areNamesUnique()) { "Names are not unique." }
            require(allHarborsClaimed()) { "Not all harbors were claimed." }

            return Result.success(Unit)
        } catch (e: IllegalArgumentException) {
            return Result.failure(ParserException(e.message ?: "Some unknown error occurred in main CorpParser."))
        }
    }

    private fun validateHarborUsage(harbor: Harbor): Result<Unit> {
        val corpIDs = harbor.corporations
        for (corpID in corpIDs) {
            val corpHarbors = simulationData.corpHarbors.get(corpID)
            if (corpHarbors == null) {
                return Result.failure(ParserException("Corporation $corpID has no harbors."))
            }
            for (corpHarbor in corpHarbors) {
                if (!corpHarbor.corporations.contains(corpID)) {
                    return Result.failure(ParserException("Cross Validation Problem"))
                }
            }
        }
        return Result.success(Unit)
    }

    /**
     * Parses the array of corporations.
     * @return result of basic validations
     */
    private fun parseCorporations(corpArray: JSONArray): Result<Unit> {
        for (corp in corpArray) {
            if (corp is JSONObject) {
                parseCorporation(corp).onFailure { return Result.failure(it) }
            } else {
                return Result.failure(ParserException("One of the corporations was not a JSONObject."))
            }
        }

        return Result.success(Unit)
    }

    /**
     * Parses the given corp JSONObject.
     * @return result of basic validations
     */
    private fun parseCorporation(corp: JSONObject): Result<Unit> {
        try {
            val id = corp.getInt(JsonKeys.ID)
            garbageTypes[id] = mutableSetOf()
            require(!corporationsById.contains(id)) { "More than one corporation specified ID $id." }

            corpNames.add(corp.getString(JsonKeys.NAME))

            val credits = corp.getInt(JsonKeys.CREDITS)
            require(credits >= 0) { "Credits must be positive." }

            // parse and validate the ship array of the corp
            val shipsJson = corp.getJSONArray(JsonKeys.SHIPS)
            val parseCorpShipsRes = parseCorpShips(id, shipsJson)
            parseCorpShipsRes.onFailure { return parseCorpShipsRes }

            // parse and validate the harbor array of the corp
            val harborsJson = corp.getJSONArray(JsonKeys.HOME_HARBORS)
            val parseCorpHarborsRes = parseCorpHarbors(harborsJson, id)
            val harbors = parseCorpHarborsRes.getOrElse { return Result.failure(it) }

            val garbageJson = corp.getJSONArray(JsonKeys.GARBAGE_TYPES)
            val parseCorpGarbageTypesRes = parseCorpGarbageTypes(id, garbageJson)
            parseCorpGarbageTypesRes.onFailure { return parseCorpGarbageTypesRes }
            val corporation = Corporation(id, harbors, garbageTypes.getValue(id).toList(), credits)
            corporationsById[id] = corporation

            return Result.success(Unit)
        } catch (e: JSONException) {
            return Result.failure(ParserException(e.message ?: "Unknown Error!"))
        }
    }

    /**
     * Parses the garbage types of a corporation.
     * @param id corporation ID
     * @return Failure in case the garbage type wasn't a string or not valid.
     */
    private fun parseCorpGarbageTypes(id: Int, garbageJson: JSONArray): Result<Unit> {
        for (garbage in garbageJson) {
            garbage.toString()
            try {
                val garbageType =
                    GarbageType.valueOf(
                        (garbage ?: return Result.failure(ParserException("JSON GarbageType was null."))) as String
                    )
                garbageTypes.getValue(id).add(garbageType)
            } catch (_: ClassCastException) {
                return Result.failure(ParserException("Corporation garbage type was null."))
            } catch (_: IllegalArgumentException) {
                return Result.failure(ParserException("Could not create GarbageType of string $garbage."))
            }
        }

        return Result.success(Unit)
    }

    /**
     * Helper method to parse and validate the array of harbors a corporation has.
     * @param harborsJsonArray as JSONArray
     * @param corpID as Int
     * @return result of basic validation
     */
    private fun parseCorpHarbors(harborsJsonArray: JSONArray, corpID: Int): Result<Set<Tile>> {
        val res = mutableSetOf<Tile>()
        val harbors = mutableListOf<Harbor>()
        for (harborJSON in harborsJsonArray) {
            try {
                val harborID = (harborJSON ?: return Result.failure(ParserException("JSON Harbor ID was null."))) as Int
                val harbor = simulationData.harbors[harborID]

                requireNotNull(harbor) {
                    "Parsed Harbor is null at Corporation $corpID"
                }

                // Checks if the harbor really belongs to this corporation
                require(harbor.corporations.contains(corpID))

                // get tile of claimed harbor
                val harborTile =
                    harbor.let {
                        oceanMap.getTileByID(it.location)
                            .getOrElse {
                                return Result.failure(ParserException("Claimed harbor $harborID is not a tile."))
                            }
                    }

                // checking that tile is actually a harbor
                if (!simulationData.harborTiles.contains(harborTile)) {
                    return Result.failure(ParserException("Claimed harbor $harborID is not a harbor."))
                }
                claimedHarbors.add(harborTile)
                harbors.add(harbor)

                // adding harbor to currentInfo of corp
                res.add(harborTile)
            } catch (_: ClassCastException) {
                return Result.failure(ParserException("Some harbor ID was not an Int."))
            }
        }
        simulationData.corpHarbors[corpID] = harbors

        return Result.success(res)
    }

    /**
     * Helper method to parse and validate the array of ships a corporation has.
     * @param id corporation ID
     * @return failure in case the array did not contain integers
     */
    private fun parseCorpShips(id: Int, shipsJson: JSONArray): Result<Unit> {
        // validate all claimed ship IDs
        // will not store this information anywhere
        try {
            for (ship in shipsJson) {
                val shipID = (ship ?: return Result.failure(ParserException("JSON Ship id was null."))) as Int
                corpToClaimedShips.getOrPut(id) { mutableSetOf() }.add(shipID)
            }
        } catch (_: ClassCastException) {
            return Result.failure(ParserException("Ship id was not an Int."))
        }

        return Result.success(Unit)
    }

    /**
     * Parses the array of ships.
     */
    private fun parseShips(shipArray: JSONArray): Result<Unit> {
        for (ship in shipArray) {
            if (ship is JSONObject) {
                val shipParsingRes = parseShip(ship)
                val shipObject = shipParsingRes
                    .getOrElse { return Result.failure(it) }
                // add ships to mappings
                ships[shipObject.id] = shipObject
                corporationsById[shipObject.corporation.id]?.ships?.add(shipObject)
                    ?: return Result.failure(
                        ParserException("Ship ${shipObject.id} wants to belong to non existing corporation $.")
                    )
            } else {
                return Result.failure(ParserException("One of the ships was not a JSONObject."))
            }
        }

        // check if each corporation got all their ships
        if (!didCorpsGetAllClaimedShips()) {
            return Result.failure(ParserException("Corps did not get all their ships."))
        }

        return Result.success(Unit)
    }

    /**
     * Parses one ship.
     * On success adds the ship to the location mapping and ship list
     */
    private fun parseShip(ship: JSONObject): Result<Ship> {
        // necessary fields
        val id = ship.getInt(JsonKeys.ID)
        if (ships.contains(id)) {
            return Result.failure(ParserException("Ship $id is already defined."))
        }

        shipNames.add(ship.getString(JsonKeys.NAME))
        val type = ShipType.valueOf(ship.getString(JsonKeys.TYPE))
        if (!validateNoExcessFields(ship, type)) {
            return Result.failure(ParserException("Ship $id has too many fields for type $type."))
        }
        val corporation = ship.getInt(JsonKeys.CORPORATION)
        val location = ship.getInt(JsonKeys.LOCATION)
        val maxVelocity = ship.getInt(JsonKeys.MAX_VELOCITY)
        val accel = ship.getInt(JsonKeys.ACCELERATION)
        val fuelCap = ship.getInt(JsonKeys.FUEL_CAPACITY)
        val fuelConsumption = ship.getInt(JsonKeys.FUEL_CONSUMPTION)

        // optional fields
        val visibilityRange: Int
        val capacity = mutableMapOf<GarbageType, Int>()

        when (type) {
            ShipType.SCOUTING -> {
                visibilityRange = ship.getInt(JsonKeys.VISIBILITY_RANGE)
            }

            ShipType.COORDINATING -> {
                visibilityRange = ship.getInt(JsonKeys.VISIBILITY_RANGE)
            }

            ShipType.COLLECTING -> {
                visibilityRange = 0
                val garbageType = GarbageType.valueOf(ship.getString(JsonKeys.GARBAGE_TYPE))
                val cap = ship.getInt(JsonKeys.CAPACITY)
                capacity[garbageType] = cap
            }

            ShipType.REFUELING -> {
                return Result.failure(ParserException("Refueling Ship can't be initialised itself"))
            }
        }

        val shipsCorporation = corporationsById[corporation] ?: return Result.failure(
            ParserException("Ship $id claims to belong to corporation $corporation which does not exist.")
        )
        val parsedShip = Ship(
            id,
            type,
            shipsCorporation,
            maxVelocity,
            accel,
            fuelCap,
            fuelConsumption,
            visibilityRange,
            capacity
        )

        ships[parsedShip.id] = parsedShip
        shipLocations[parsedShip.id] = location

        return validateShip(parsedShip, corporation, location)
    }

    /**
     * Validates ship parameters, location and garbage type.
     */
    private fun validateShip(
        ship: Ship,
        corporation: Int,
        location: Int,
    ): Result<Ship> {
        // check properties
        checkShipProperties(ship).onFailure { return Result.failure(it) }

        // check garbage capacity
        if (ship.type == ShipType.COLLECTING) {
            for ((gT, amount) in ship.maxGarbageCapacity) {
                if (amount > gT.maxCap || amount < gT.minCap) {
                    return Result.failure(ParserException("Garbage capacity $amount invalid for type $gT."))
                }
                if (!doesCollect(corporation, gT)) {
                    return Result.failure(
                        ParserException(
                            "Corporation $corporation does not collect garbage type $gT of ship ${ship.id}."
                        )
                    )
                }
            }
        }

        // check if ship can reach a harbor
        if (!validateShipLocation(location, corporation)) {
            return Result.failure(ParserException("Ship ${ship.id} cannot reach a harbor."))
        }

        // check if ship is actually being claimed by the corporation that it specifies
        return if (!doesCorpClaimShip(corporation, ship)) {
            Result.failure(ParserException("Ship ${ship.id} is not being claimed by it's corp $corporation."))
        } else {
            Result.success(ship)
        }
    }

    /**
     * Checks that the ship properties are within their bounds.
     */
    private fun checkShipProperties(ship: Ship): Result<Unit> {
        val sT = ship.type

        return if (ship.maxVelocity > sT.maxVelMax || ship.maxVelocity < sT.maxVelMin) {
            // violation to maxVel
            Result.failure(ParserException("Velocity ${ship.maxVelocity} invalid for type $sT."))
        } else if (ship.acceleration > sT.accelMax || ship.acceleration < sT.accelMin) {
            // violation to acceleration
            Result.failure(ParserException("Acceleration ${ship.acceleration} invalid for type $sT."))
        } else if (ship.maxFuel > sT.fuelCapMax || ship.maxFuel < sT.fuelCapMin) {
            // violation to fuel capacity
            Result.failure(ParserException("Fuel Capacity ${ship.maxFuel} invalid for type $sT."))
        } else if (ship.fuelConsumption > sT.fuelConsumpMax || ship.fuelConsumption < sT.fuelConsumpMin) {
            // violation to fuel consumption
            Result.failure(ParserException("Fuel Consumption ${ship.fuelConsumption} invalid for type $sT."))
        } else if (ship.visibilityRange > sT.visRangeMax || ship.visibilityRange < sT.visRangeMin) {
            // violation to fuel consumption
            Result.failure(ParserException("Visibility Range ${ship.visibilityRange} invalid for type $sT."))
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Checks if the given corporation collects the given garbage type.
     * Assumes Corporations have been parsed already.
     */
    private fun doesCollect(corp: Int, gT: GarbageType): Boolean {
        return garbageTypes[corp]?.contains(gT) ?: false
    }

    /**
     * Checks if a ship on the given location can reach a harbor of the given corp.
     */
    private fun validateShipLocation(shipLocation: Int, corporation: Int): Boolean {
        val harbors = corporationsById[corporation]?.harbors
        if (harbors.isNullOrEmpty()) {
            return false
        }
        val shipTile = oceanMap.getTileByID(shipLocation).getOrElse { return false }
        return pathFinder.isReachable(shipTile, harbors)
    }

    /**
     * Checks if the given corporation has at least one ship for each specified garbage type.
     * @param corporation ID of corporation
     */
    private fun hasShipForGarbageTypes(corporation: Int): Result<Unit> {
        for (gT in garbageTypes[corporation].orEmpty()) {
            val hasShip = corporationsById[corporation]?.ships
                ?.any { ship -> ship.maxGarbageCapacity[gT] != null } ?: false
            if (!hasShip) {
                return Result.failure(ParserException("Corp $corporation does not have a ship for $gT."))
            }
        }
        return Result.success(Unit)
    }

    /**
     * Checks if the ship is being claimed by corporation.
     */
    private fun doesCorpClaimShip(corporation: Int, ship: Ship): Boolean {
        return corpToClaimedShips[corporation]?.contains(ship.id) ?: false
    }

    /**
     * Checks if a corporation actually got all claimed ships.
     */
    private fun didCorpsGetAllClaimedShips(): Boolean {
        for ((corpID, shipsOfCorp) in corpToClaimedShips) {
            for (shipID in shipsOfCorp) {
                val ship = ships[shipID]?.corporation?.id == corpID
                if (!ship) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Checks uniqueness of corp and ship names.
     * Needs parsing to be done first.
     * Will compare the count of names to the count of IDs.
     */
    private fun areNamesUnique(): Boolean {
        return shipNames.size == ships.size && corpNames.size == corporationsById.size
    }

    /**
     * Checks if all harbors have been claimed by at least one corporation.
     */
    private fun allHarborsClaimed(): Boolean {
        return claimedHarbors == simulationData.harborTiles
    }

    /**
     * Validates that the given ship JSONObject does not contain fields it shouldn't have due to the ship type.
     * Returns true if no excess fields were found.
     */
    private fun validateNoExcessFields(shipJson: JSONObject, shipType: ShipType): Boolean {
        val correctJsonKeys = mutableSetOf(
            JsonKeys.ID, JsonKeys.NAME, JsonKeys.TYPE, JsonKeys.CORPORATION, JsonKeys.LOCATION, JsonKeys.MAX_VELOCITY,
            JsonKeys.ACCELERATION, JsonKeys.FUEL_CAPACITY, JsonKeys.FUEL_CONSUMPTION
        )
        correctJsonKeys.addAll(
            when (shipType) {
                ShipType.SCOUTING -> setOf(JsonKeys.VISIBILITY_RANGE)
                ShipType.COORDINATING -> setOf(JsonKeys.VISIBILITY_RANGE)
                ShipType.COLLECTING -> setOf(JsonKeys.GARBAGE_TYPE, JsonKeys.CAPACITY)
                ShipType.REFUELING -> return false // Since refueling ship shouldn't be present during the parsing
            }
        )

        return shipJson.keySet() == correctJsonKeys
    }

    /**
     * Sorts list ownShips in all corporations.
     */
    private fun sortShips() {
        for ((_, currentInfo) in corporationsById) {
            currentInfo.ships.sortBy { it.id }
        }
    }
}
