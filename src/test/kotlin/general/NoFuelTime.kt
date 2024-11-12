package general

import de.unisaarland.cs.se.selab.control.Simulation
import de.unisaarland.cs.se.selab.parser.Parser
import de.unisaarland.cs.se.selab.parser.SimulationData
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoFuelTime {
    private var simulationData = SimulationData()

    @BeforeEach
    fun setup() {
        val parser = Parser()
        val simulationDataRes = parser.parse(
            listOf(
                "src/test/resources/mapFiles/worldMap2.json",
                "src/test/resources/corporationFiles/smallCorp11.json",
                "src/test/resources/scenarioFiles/unload2.json"
            )
        )
        simulationData = simulationDataRes.getOrThrow()
    }

    @Test
    fun testRefuelingStationBehavior() {
        val oceanMap = simulationData.oceanMap
        assertNotNull(oceanMap)
        val corporation = simulationData.corporations[0]
        assertNotNull(corporation)

        val ship1 = corporation?.ships?.get(0)
        assertNotNull(ship1)
        val ship2 = corporation?.ships?.get(1)
        assertNotNull(ship2)
        val ship3 = corporation?.ships?.get(2)
        assertNotNull(ship3)

        simulationData.harbors[0]?.refuelingStation?.refuelTimes = 2

        Simulation(simulationData, 15)
    }

    @Test
    fun testTryingAgain() {
        simulationData.corporations[0]?.credits = 1000
        simulationData.harbors[0]?.refuelingStation?.refuelTimes = 1
        Simulation(simulationData, 15)
    }
}
