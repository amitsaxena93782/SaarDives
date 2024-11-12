package de.unisaarland.cs.se.selab.systemtest.simpleScenario

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * Simple Scenario Test
 */
class SimpleScenario1 : SystemTestExtension() {
    // short description of the test
    override val description = "Simple Scenario Test till tick 1"

    // name of the test
    override val name = "Simple Test 1"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/worldMap.json"
    override val corporations = "corporationFiles/smallCorp.json"
    override val scenario = "scenarioFiles/simpleScenario.json"

    // max ticks parameter for the test execution
    override val maxTicks = 2

    override suspend fun run() {
        val filenames = listOf(map, corporations, scenario)

        filenames.forEach {
            assertNextLine(
                "Initialization Info: " +
                    "${it.split("/").last()} successfully parsed and validated."
            )
        }

        assertNextLine("Simulation Info: Simulation started.")

        for (i in 0..1) {
            assertNextLine("Simulation Info: Tick $i started.")
            manageCorpActions(i)
        }
        assertNextLine("Simulation Info: Simulation ended.")
        assertNextLine("Simulation Info: Simulation statistics are calculated.")
        assertNextLine("Simulation Statistics: Corporation 0 collected 0 of garbage.")
        assertNextLine("Simulation Statistics: Total amount of plastic collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of oil collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of chemicals collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of garbage still in the ocean: 1100.")

        assertEnd()
    }

    private suspend fun manageCorpActions(tick: Int) {
        assertNextLine("Corporation Action: Corporation 0 is starting to move its ships.")
        moveShips(tick)
        assertNextLine("Corporation Action: Corporation 0 is starting to collect garbage.")
        assertNextLine("Corporation Action: Corporation 0 is starting to cooperate with other corporations.")
        assertNextLine("Corporation Action: Corporation 0 is starting to refuel.")
        purchaseShip(tick)
        assertNextLine("Corporation Action: Corporation 0 finished its actions.")
    }

    private suspend fun moveShips(tick: Int) {
        when (tick) {
            0 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 25 to tile 12.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 10 to tile 52.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 15 to tile 59.")
            }
            1 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 50 to tile 17.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 10 to tile 42.")
                assertNextLine("Ship Movement: Ship 2 moved with speed 10 to tile 26.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 30 to tile 28.")
            }
        }
    }

    private suspend fun purchaseShip(tick: Int) {
        when (tick) {
            0 -> {
                assertNextLine("Purchase: Ship 1 ordered a refueling ship with id 5 at harbor 1 for 1234 credits.")
            }
        }
    }
}
