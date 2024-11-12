package de.unisaarland.cs.se.selab.systemtest.typhoonSimple

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * Simple Scenario Test
 */
class TyphoonSimple7 : SystemTestExtension() {
    // short description of the test
    override val description = "Simple Typhoon Test till tick 7"

    // name of the test
    override val name = "Typhoon Test 7"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/worldMap2.json"
    override val corporations = "corporationFiles/smallCorp.json"
    override val scenario = "scenarioFiles/typhoon1.json"

    // max ticks parameter for the test execution
    override val maxTicks = 8

    override suspend fun run() {
        val filenames = listOf(map, corporations, scenario)

        filenames.forEach {
            assertNextLine(
                "Initialization Info: " +
                    "${it.split("/").last()} successfully parsed and validated."
            )
        }

        assertNextLine("Simulation Info: Simulation started.")

        for (i in 0..7) {
            assertNextLine("Simulation Info: Tick $i started.")
            manageCorpActions(i)
        }
        assertNextLine("Simulation Info: Simulation ended.")
        assertNextLine("Simulation Info: Simulation statistics are calculated.")
        assertNextLine("Simulation Statistics: Corporation 0 collected 1600 of garbage.")
        assertNextLine("Simulation Statistics: Total amount of plastic collected: 0.")
        assertNextLine("Simulation Statistics: Total amount of oil collected: 1600.")
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
        } else if (tick == 3) {
            assertNextLine("Garbage Collection: Ship 2 collected 800 of garbage OIL with 3.")
        }
        assertNextLine("Corporation Action: Corporation 0 is starting to cooperate with other corporations.")
        assertNextLine("Corporation Action: Corporation 0 is starting to refuel.")
        purchaseAndRepairShips(tick)
        assertNextLine("Corporation Action: Corporation 0 finished its actions.")
        if (tick == 2) {
            assertNextLine("Event: Event 1 of type TYPHOON happened.")
            assertNextLine("Event: Typhoon 1 at tile 28 with radius 2 affected ships: 0, 2, 4.")
        }
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
                assertNextLine("Ship Movement: Ship 0 moved with speed 21 to tile 48.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 30 to tile 15.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 11 to tile 38.")
            }
            4 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 40 to tile 59.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 30 to tile 12.")
                assertNextLine("Ship Movement: Ship 2 moved with speed 12 to tile 38.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 22 to tile 59.")
            }
            5 -> {
                assertNextLine("Ship Movement: Ship 1 moved with speed 30 to tile 42.")
                assertNextLine("Ship Movement: Ship 2 moved with speed 18 to tile 48.")
            }
            6 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 25 to tile 38.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 30 to tile 52.")
                assertNextLine("Ship Movement: Ship 2 moved with speed 20 to tile 59.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 15 to tile 48.")
            }
            7 -> {
                assertNextLine("Ship Movement: Ship 0 moved with speed 50 to tile 14.")
                assertNextLine("Ship Movement: Ship 1 moved with speed 10 to tile 42.")
                assertNextLine("Ship Movement: Ship 4 moved with speed 30 to tile 17.")
                assertNextLine("Ship Movement: Ship 5 moved with speed 27 to tile 33.")
            }
        }
    }

    private suspend fun purchaseAndRepairShips(tick: Int) {
        when (tick) {
            0 -> {
                assertNextLine("Purchase: Ship 1 ordered a refueling ship with id 5 at harbor 1 for 1234 credits.")
            }
            4 -> {
                assertNextLine("Repair: Ship 0 is being repaired at harbor 2 for 200 credits.")
                assertNextLine("Repair: Ship 4 is being repaired at harbor 2 for 200 credits.")
                assertNextLine("Purchase: Ship 5 delivered to corporation 0 at 52.")
            }
            5 -> {
                assertNextLine("Repair: Ship 0 is repaired.")
                assertNextLine("Repair: Ship 4 is repaired.")
            }
            6 -> {
                assertNextLine("Repair: Ship 2 is being repaired at harbor 2 for 200 credits.")
                assertNextLine("Purchase: Ship 1 ordered a refueling ship with id 6 at harbor 1 for 1234 credits.")
            }
            7 -> {
                assertNextLine("Repair: Ship 2 is repaired.")
            }
        }
    }
}
