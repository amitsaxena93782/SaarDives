package general

import de.unisaarland.cs.se.selab.parser.Parser
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParserTest {
    lateinit var parser: Parser

    @BeforeEach
    fun setup() {
        parser = Parser()
    }

    @Test
    fun testParserCorrect() {
        val filenames = listOf(
            "src/test/resources/mapFiles/smallMap.json",
            "src/test/resources/corporationFiles/corporations.json",
            "src/test/resources/scenarioFiles/scenario.json"
        )
        val simulationData = parser.parse(filenames).getOrThrow()
        assertNotNull(simulationData, "Simulation data should not be null")
        assertEquals(36, simulationData.tiles.size, "All Tiles aren't parsed yet")
        assertEquals(2, simulationData.harbors.size, "All Harbors aren't parsed yet")
        assertEquals(2, simulationData.corporations.size, "All Corporations aren't parsed yet")
        assertEquals(5, simulationData.ships.size, "All Ships aren't parsed yet")
    }

    @Test
    fun testParserScenarioIncorrect() {
        val filenames = listOf(
            "src/test/resources/mapFiles/smallMap.json",
            "src/test/resources/corporationFiles/corporations.json",
            "src/test/resources/scenarioFiles/missing1.json"
        )
        val parsingResult = parser.parse(filenames)
        assertTrue(parsingResult.isFailure, "Should have failed!")
    }

    @Test
    fun testParserCorpCreditsNegative() {
        val filenames = listOf(
            "src/test/resources/mapFiles/smallMap.json",
            "src/test/resources/corporationFiles/incorrectCorps/negativeCredits.json",
            "src/test/resources/scenarioFiles/scenario.json"
        )
        val parsingResult = parser.parse(filenames)
        assertTrue(parsingResult.isFailure, "Should have failed: Corp has negative credits!")
    }
}
