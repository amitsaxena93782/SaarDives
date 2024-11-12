package de.unisaarland.cs.se.selab.systemtest.refuelSimple

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * Simple Refueling Test
 */
class RefuelSimple6 : SystemTestExtension() {
    // short description of the test
    override val description = "Simple Refueling Test till tick 6"

    // name of the test
    override val name = "Refueling Test 6"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/worldMap2.json"
    override val corporations = "corporationFiles/smallCorp3.json"
    override val scenario = "scenarioFiles/simpleScenario.json"

    // max ticks parameter for the test execution
    override val maxTicks = 7

    override suspend fun run() {
        val filenames = listOf(map, corporations, scenario)

        filenames.forEach {
            assertNextLine(
                "Initialization Info: " +
                    "${it.split("/").last()} successfully parsed and validated."
            )
        }

        assertNextLine("Simulation Info: Simulation started.")

        for (i in 0..6) {
            assertNextLine("Simulation Info: Tick $i started.")
            manageCorpActions(i)
        }
        assertNextLine("Simulation Info: Simulation ended.")
        assertNextLine("Simulation Info: Simulation statistics are calculated.")
        assertNextLine("Simulation Statistics: Corporation 0 collected 800 of garbage.")
        assertNextLine("Simulation Statistics: Total amount of plastic collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of oil collected: 800.")
        assertNextLine("Simulation Statistics: Total amount of chemicals collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of garbage still in the ocean: 300.")

        assertEnd()
    }

    private suspend fun manageCorpActions(tick: Int) {
        assertNextLine("Corporation Action: Corporation 0 is starting to move its ships.")
        moveShips(tick)
        assertNextLine("Corporation Action: Corporation 0 is starting to collect garbage.")
        if (tick == 2) {
            assertNextLine("Garbage Collection: Ship 2 collected 800 of garbage OIL with 1.")
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
            5 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 75 to tile 19.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 30 to tile 42.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 45 to tile 19.")
                assertNextLine("Ship Movement: Ship 5 moved with speed 27 to tile 33.")
            }
            6 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 100 to tile 14.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 30 to tile 52.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 60 to tile 13.")
                assertNextLine("Ship Movement: Ship 5 moved with speed 54 to tile 14.")
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
            6 -> {
                assertNextLine("Refueling: Ship 5 started to refuel ship 0.")
                assertNextLine("Purchase: Ship 1 ordered a refueling ship with id 6 at harbor 1 for 1234 credits.")
            }
        }
    }
}
