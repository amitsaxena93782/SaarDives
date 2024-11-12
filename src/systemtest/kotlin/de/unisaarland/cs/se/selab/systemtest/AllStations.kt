package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.utils.SystemTestExtension

/**
 * Tests the simulation with harbor having all 3 stations
 * */
class AllStations : SystemTestExtension() {
    // short description of the test
    override val description = "All Stations at harbor"

    // name of the test
    override val name = "Validation Harbor"

    // path to the map, corporations and scenario files, relative from 'resources' folder
    override val map = "mapFiles/invalidMaps/allStations.json"
    override val corporations = "corporationFiles/smallCorp2.json"
    override val scenario = "scenarioFiles/simpleScenario.json"

    // max ticks parameter for the test execution
    override val maxTicks = 5

    override suspend fun run() {
        assertNextLine("Initialization Info: allStations.json is invalid.")
        assertEnd()
    }
}
