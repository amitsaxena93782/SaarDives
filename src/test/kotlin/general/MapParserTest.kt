package general

import de.unisaarland.cs.se.selab.parser.MapParser
import de.unisaarland.cs.se.selab.parser.SimulationData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapParserTest {
    private lateinit var simulationData: SimulationData

    @BeforeEach
    fun setup() {
        simulationData = SimulationData()
    }

    @Test
    fun testMapParserCorrect() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/smallMap.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isSuccess, "Parsing failed for correct map config!")
        assertEquals(
            36,
            simulationData.tiles.size,
            "Parses ${simulationData.tiles.size} " +
                "tiles instead of 36!"
        )
    }

    @Test
    fun testMapParserIncorrectCoordinate() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/incorrect1.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for an incorrect coordinate map config!")
    }

    @Test
    fun testMapParserMissingCoordinate() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/incorrect2.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for an missing coordinate map config!")
    }

    @Test
    fun testMapParserInvalidCategory() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/invalidCategory.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for an incorrect category value!")
    }

    @Test
    fun testMapParserMissingCategory() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/missingCategory.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for an missing category map!")
    }

    @Test
    fun testMapParserHarborOnWrongCategory() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/wrongHarbor.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for harbor on a wrong  category!")
    }

    @Test
    fun testMapParserDuplicateIDs() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/duplicateID.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for duplicate IDs in map!")
    }

    @Test
    fun testMapParserDuplicateCoordinates() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/duplicateCoord.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for duplicate coordinates in map!")
    }

    @Test
    fun testMapParserHarborIDMissing() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/harborIDMissing.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for missing harbor ID in map!")
    }

    @Test
    fun testMapParserHarborExplicit() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/harborIDExplicit.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for extra harbor ID in map!")
    }

    @Test
    fun testMapParserHarborIDUnknown() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/harborIDUnknown.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for unknown harbor ID in map!")
    }

    @Test
    fun testMapParserHarborInvalidGarbage() {
        val mapParser = MapParser(simulationData)
        val jsonString = File("src/test/resources/mapFiles/invalidMaps/invalidGarbage.json").readText()
        val parsingResult = mapParser.parse(jsonString)

        assertTrue(parsingResult.isFailure, "Parsing passes for invalid garbage type in Unloading Station!")
    }
}
