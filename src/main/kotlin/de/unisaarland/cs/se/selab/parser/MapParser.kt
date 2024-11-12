package de.unisaarland.cs.se.selab.parser

import com.github.erosb.jsonsKema.FormatValidationPolicy
import com.github.erosb.jsonsKema.JsonParser
import com.github.erosb.jsonsKema.JsonValue
import com.github.erosb.jsonsKema.SchemaLoader
import com.github.erosb.jsonsKema.ValidationFailure
import com.github.erosb.jsonsKema.Validator
import com.github.erosb.jsonsKema.ValidatorConfig
import de.unisaarland.cs.se.selab.Constants
import de.unisaarland.cs.se.selab.data.Coordinate
import de.unisaarland.cs.se.selab.data.Current
import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Tile
import de.unisaarland.cs.se.selab.enums.TileType
import org.json.JSONArray
import org.json.JSONObject

/**
 * The MapParser class is responsible for parsing the map from the JSON file.
 */
class MapParser(private var simulationData: SimulationData) {
    private val tilesMap: MutableMap<Int, Tile> = mutableMapOf()
    private val tileCoordinates: MutableMap<Coordinate, Tile> = mutableMapOf()
    private lateinit var harborParser: HarborParser
    private val parsedHarborID = emptyList<Int>()

    /**
     * Parses the map from the JSON file.
     */
    fun parse(jsonString: String): Result<Unit> {
        val schema = SchemaLoader.forURL("classpath://schema/map.schema").load()
        // Validate the JSON data with the schema
        val validator = Validator.create(schema, ValidatorConfig(FormatValidationPolicy.ALWAYS))
        val jsonInstance: JsonValue = JsonParser(jsonString).parse()
        val failure: ValidationFailure? = validator.validate(jsonInstance)
        if (failure != null) {
            return Result.failure(ParserException(failure.message))
        }

        val json = JSONObject(jsonString)
        val tiles = json.getJSONArray(JsonKeys.TILES)
        val harborsJSONArray = json.getJSONArray(JsonKeys.HARBORS)

        // Parsing all the tiles
        parseTiles(tiles).onFailure { return Result.failure(it) }

        // Updating the simulationData itself rather than fetching the data from parseTiles()
        // simulationData.tiles.putAll(tilesMap)

        // Initialising Harbor Parser and parsing harbors
        harborParser = HarborParser(simulationData)
        harborParser.parseHarbors(harborsJSONArray).onFailure { return Result.failure(it) }

        // Cross validation from map to harbor
        crossValidateMapHarbor()

        simulationData.oceanMap = OceanMap(tileCoordinates)
        for (harbor in simulationData.harbors.values) {
            simulationData.tileToHarborMap[harbor.location] = harbor
        }

        return Result.success(Unit)
    }

    /**
     * This helper function cross validates the harbor tiles from ["tiles"] field to ["harbors"] field
     * @param harborTiles as MutableSet<Tile>
     * @param harborMap as MutableMap<Int, Harbor>
     * @return result as success or failure
     * */
    private fun crossValidateMapHarbor(): Result<Unit> {
        simulationData.harborTiles.forEach { tile ->
            val harbor = simulationData.harbors[tile.harborID]
            if (harbor == null || tile.id != harbor.location || tile.harborID != harbor.id) {
                return Result.failure(ParserException("Harbor ${tile.harborID ?: ""} does not exist"))
            }
        }
        return Result.success(Unit)
    }

    /**
     * This helper function parses all the tiles and update the data in simulationData
     * @param tiles as JSONArray
     * @return Result<Unit>
     * */
    private fun parseTiles(tiles: JSONArray): Result<Unit> {
        tiles.forEach { tile ->
            if (tile is JSONObject) {
                val tileObject = validateAndCreateTiles(tile).getOrElse { return Result.failure(it) }
                // check if the tile id is already present
                if (tileObject.id in this.tilesMap.keys) {
                    return Result.failure(ParserException("Tile with id ${tileObject.id} already exists."))
                }
                tileCoordinates[tileObject.coordinate] = tileObject // add the tile to the coordinates map
                tilesMap[tileObject.id] = tileObject // adding the data to the tilesMap
                // simulationData is being updated automatically
                simulationData.tiles[tileObject.id] = tileObject
            } else {
                return Result.failure(ParserException("Tile is not a JSONObject."))
            }
        }
        // validate neighbours of the tiles in the map
        if (!validateAdjacentTiles()) {
            return Result.failure(ParserException("Invalid neighbors in the map."))
        }

        // add harbors to the simulation data
        for (t in tilesMap.values) {
            if (t.harbor) {
                simulationData.harborTiles.add(t)
            }
        }

        return Result.success(Unit)
    }

    /**
     * This helper method validates and parse each tile
     * @param tile as JSONObject
     * @return Result<Tile>
     * */
    private fun validateAndCreateTiles(tile: JSONObject): Result<Tile> {
        // check if the keys are valid
        if (!validateKeySet(tile)) {
            return Result.failure(ParserException("Invalid keys in tile $tile."))
        }

        // validate and instantiate coordinates if they are valid
        if (!validateCoordinates(tile.getJSONObject(JsonKeys.COORDINATES))) {
            return Result.failure(ParserException("Invalid coordinates in tile $tile."))
        }
        val coordinate =
            Coordinate(
                tile.getJSONObject(JsonKeys.COORDINATES).getInt(JsonKeys.X),
                tile.getJSONObject(JsonKeys.COORDINATES).getInt(JsonKeys.Y)
            )
        if (coordinate in tileCoordinates.keys) {
            return Result.failure(ParserException("Coordinate $coordinate already exists."))
        }

        val type = tile.getString(JsonKeys.CATEGORY)
        // get the drift of the tile
        val (harbor, drift) = validateSpecialTiles(tile).getOrElse { return Result.failure(it) }

        // Getting the harbor ID
        val harborID = if (harbor) {
            val id = tile.getInt(JsonKeys.HARBOR_ID)
            if (id < 0 || id in parsedHarborID) throw ParserException("Harbor ID must be positive or unique.")
            parsedHarborID.plus(id)
            id
        } else {
            null
        }
        // create tile
        return Result.success(
            Tile(
                tile.getInt(JsonKeys.ID),
                TileType.createTileType(type),
                drift,
                coordinate,
                harbor,
                harborID
            )
        )
    }

    /**
     * Validate the JSON key set of the tile.
     */
    private fun validateKeySet(tile: JSONObject): Boolean {
        if (!tile.keySet().contains(JsonKeys.CATEGORY)) {
            return false
        }
        val allowedKeys = when (tile.getString(JsonKeys.CATEGORY)) {
            JsonKeys.LAND, JsonKeys.SHALLOW_OCEAN -> {
                setOf(
                    JsonKeys.ID,
                    JsonKeys.COORDINATES,
                    JsonKeys.CATEGORY
                )
            }

            JsonKeys.SHORE -> {
                if (tile.getBoolean(JsonKeys.HARBOR)) {
                    setOf(
                        JsonKeys.ID,
                        JsonKeys.COORDINATES,
                        JsonKeys.CATEGORY,
                        JsonKeys.HARBOR,
                        JsonKeys.HARBOR_ID
                    )
                } else {
                    setOf(
                        JsonKeys.ID,
                        JsonKeys.COORDINATES,
                        JsonKeys.CATEGORY,
                        JsonKeys.HARBOR
                    )
                }
            }

            JsonKeys.DEEP_OCEAN -> {
                if (tile.getBoolean(JsonKeys.CURRENT)) {
                    setOf(
                        JsonKeys.ID,
                        JsonKeys.COORDINATES,
                        JsonKeys.DIRECTION,
                        JsonKeys.SPEED,
                        JsonKeys.INTENSITY,
                        JsonKeys.CURRENT,
                        JsonKeys.CATEGORY
                    )
                } else {
                    setOf(
                        JsonKeys.ID,
                        JsonKeys.COORDINATES,
                        JsonKeys.CURRENT,
                        JsonKeys.CATEGORY
                    )
                }
            }

            else -> { return false }
        }
        return tile.keySet() == allowedKeys
    }

    /**
     * Validate compatibility of adjacent tiles.
     */
    private fun validateAdjacentTiles(): Boolean {
        // go through the coordinates that we collected & check if the neighbours are valid
        for ((c, t) in tileCoordinates) {
            val neighborsCoord = c.getNeighbours()
            val neighborsTiles = neighborsCoord.mapNotNull { tileCoordinates[it] }
            if (!isCompatibleType(t.type, neighborsTiles)) {
                return false
            }
        }
        return true
    }

    /**
     * Check if tile type is compatible with other tile types.
     */
    private fun isCompatibleType(type: TileType, neighbors: List<Tile>): Boolean {
        return when (type) {
            TileType.LAND -> { neighbors.all { it.type == TileType.LAND || it.type == TileType.SHORE } }
            TileType.SHORE -> {
                neighbors.all {
                    it.type == TileType.LAND || it.type == TileType.SHORE ||
                        it.type == TileType.SHALLOW_OCEAN
                }
            }
            TileType.SHALLOW_OCEAN -> {
                neighbors.all {
                    it.type == TileType.SHORE || it.type == TileType.SHALLOW_OCEAN ||
                        it.type == TileType.DEEP_OCEAN
                }
            }
            TileType.DEEP_OCEAN -> {
                neighbors.all { it.type == TileType.SHALLOW_OCEAN || it.type == TileType.DEEP_OCEAN }
            }
        }
    }

    /**
     * Validate the JSON key set coordinates.
     */
    private fun validateCoordinates(coordinates: JSONObject): Boolean {
        return coordinates.keySet() == setOf(JsonKeys.X, JsonKeys.Y)
    }

    /**
     * Validate tiles with special properties (Deep Ocean with current or Shore with harbor)
     * @param tile the JSON object of the tile
     * @return true if the keys are valid, false otherwise
     */
    private fun validateSpecialTiles(tile: JSONObject): Result<Pair<Boolean, Current?>> {
        var drift: Current? = null
        var harbor = false
        val type = tile.getString(JsonKeys.CATEGORY)

        // get the drift of the tile
        if (type == JsonKeys.DEEP_OCEAN && tile.getBoolean(JsonKeys.CURRENT)) {
            val direction = if (tile.get(JsonKeys.DIRECTION) is Int) {
                tile.getInt(JsonKeys.DIRECTION)
            } else {
                return Result.failure(ParserException("Invalid direction."))
            }
            try {
                drift = Current(direction, tile.getInt(JsonKeys.SPEED), tile.getInt(JsonKeys.INTENSITY))
            } catch (_: IllegalArgumentException) {
                return Result.failure(ParserException("Invalid direction."))
            }
            if (
                drift.speed !in Constants.MIN_DRIFT_SPEED..Constants.MAX_DRIFT_SPEED ||
                drift.intensity !in Constants.MIN_DRIFT_INTENSITY..Constants.MAX_DRIFT_INTENSITY
            ) {
                return Result.failure(ParserException("Invalid current speed/intensity values in tile $tile."))
            }
        } else if (type == JsonKeys.SHORE && tile.getBoolean(JsonKeys.HARBOR)) {
            harbor = tile.getBoolean(JsonKeys.HARBOR)
        }
        return Result.success(Pair(harbor, drift))
    }
}
