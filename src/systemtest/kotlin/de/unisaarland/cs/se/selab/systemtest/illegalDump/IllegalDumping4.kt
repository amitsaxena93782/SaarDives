package de.unisaarland.cs.se.selab.systemtest.illegalDump

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * Simple Scenario Test
 */
class IllegalDumping4 : SystemTestExtension() {
    // short description of the test
    override val description = "Illegal Dumping Test till tick 4"

    // name of the test
    override val name = "Illegal Dumping 4"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/worldMap3.json"
    override val corporations = "corporationFiles/smallCorp2.json"
    override val scenario = "scenarioFiles/unload1.json"

    // max ticks parameter for the test execution
    override val maxTicks = 5

    override suspend fun run() {
        val filenames = listOf(map, corporations, scenario)

        filenames.forEach {
            assertNextLine(
                "Initialization Info: " +
                    "${it.split("/").last()} successfully parsed and validated."
            )
        }

        assertNextLine("Simulation Info: Simulation started.")

        for (i in 0..4) {
            assertNextLine("Simulation Info: Tick $i started.")
            manageCorpActions(i)
        }
        assertNextLine("Simulation Info: Simulation ended.")
        assertNextLine("Simulation Info: Simulation statistics are calculated.")
        assertNextLine("Simulation Statistics: Corporation 0 collected 1000 of garbage.")
        assertNextLine("Simulation Statistics: Total amount of plastic collected: 1000.")
        assertNextLine("Simulation Statistics: Total amount of oil collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of chemicals collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of garbage still in the ocean: 300.")

        assertEnd()
    }

    private suspend fun manageCorpActions(tick: Int) {
        assertNextLine("Corporation Action: Corporation 0 is starting to move its ships.")
        moveShips(tick)
        assertNextLine("Corporation Action: Corporation 0 is starting to collect garbage.")
        if (tick == 2) {
            assertNextLine("Garbage Collection: Ship 2 collected 1000 of garbage PLASTIC with 1.")
        }
        assertNextLine("Corporation Action: Corporation 0 is starting to cooperate with other corporations.")
        assertNextLine("Corporation Action: Corporation 0 is starting to refuel.")
        purchaseAndRefuelShips(tick)
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
            2 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 75 to tile 28.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 20 to tile 22.")
                assertNextLine("Ship Movement: Ship 2 moved with speed 20 to tile 28.")
            }
            3 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 25 to tile 17.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 30 to tile 15.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 15 to tile 18.")
            }
            4 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 50 to tile 12.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 30 to tile 12.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 30 to tile 15.")
            }
        }
    }

    private suspend fun purchaseAndRefuelShips(tick: Int) {
        when (tick) {
            0 -> {
                assertNextLine("Purchase: Ship 1 ordered a refueling ship with id 5 at harbor 1 for 1234 credits.")
            }
            4 -> {
                assertNextLine("Purchase: Ship 5 delivered to corporation 0 at 52.")
            }
        }
    }
}
