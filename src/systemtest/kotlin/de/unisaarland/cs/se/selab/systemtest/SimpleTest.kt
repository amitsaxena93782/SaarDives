package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * Example System test. Here we introduce the usage of the default SystemTest implementation
 * as well as the use of the utility methods. See utility method documentation for more details.
 */
class SimpleTest : SystemTestExtension() {
    // short description of the test
    override val description = "Simple Test for parsing"

    // name of the test
    override val name = "Simple Test"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/smallMap.json"
    override val corporations = "corporationFiles/corporations.json"
    override val scenario = "scenarioFiles/scenario.json"

    // max ticks parameter for the test execution
    override val maxTicks = 1

    override suspend fun run() {
        val filenames = listOf(map, corporations, scenario)

        filenames.forEach {
            assertNextLine(
                "Initialization Info: " +
                    "${it.split("/").last()} successfully parsed and validated."
            )
        }

        assertNextLine("Simulation Info: Simulation started.")
        assertNextLine("Simulation Info: Tick 0 started.")
        manageCorpActions(0)
        manageCorpActions(1)

        assertNextLine("Simulation Info: Simulation ended.")
        assertNextLine("Simulation Info: Simulation statistics are calculated.")
        assertNextLine("Simulation Statistics: Corporation 0 collected 0 of garbage.")
        assertNextLine("Simulation Statistics: Corporation 1 collected 0 of garbage.")
        assertNextLine("Simulation Statistics: Total amount of plastic collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of oil collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of chemicals collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of garbage still in the ocean: 200.")

        assertEnd()
    }

    private suspend fun manageCorpActions(id: Int) {
        assertNextLine("Corporation Action: Corporation $id is starting to move its ships.")
        if (id == 0) {
            assertNextLine("Ship Movement: Ship 0 moved with speed 25 to tile 4.")
        } else {
            assertNextLine("Ship Movement: Ship 4 moved with speed 15 to tile 5.")
        }
        assertNextLine("Corporation Action: Corporation $id is starting to collect garbage.")
        assertNextLine("Corporation Action: Corporation $id is starting to cooperate with other corporations.")
        assertNextLine("Corporation Action: Corporation $id is starting to refuel.")
        assertNextLine("Corporation Action: Corporation $id finished its actions.")
    }
}
