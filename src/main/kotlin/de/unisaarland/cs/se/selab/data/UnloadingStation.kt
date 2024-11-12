package de.unisaarland.cs.se.selab.data

import de.unisaarland.cs.se.selab.enums.GarbageType
/**
 * This class represents the Unloading Station of a harbor
 * */
class UnloadingStation(
    val unloadReturn: Int,
    val garbagesType: List<GarbageType>
) : Station()
