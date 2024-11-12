package de.unisaarland.cs.se.selab.task

import de.unisaarland.cs.se.selab.data.OceanMap
import de.unisaarland.cs.se.selab.data.Ship
import de.unisaarland.cs.se.selab.data.TaskData
import de.unisaarland.cs.se.selab.enums.Behaviour
import de.unisaarland.cs.se.selab.enums.TaskType

/**
 * class for collecting task
 */
class CollectingTask(taskData: TaskData) : Task(taskData) {

    override fun update(ship: Ship, oceanMap: OceanMap, movementPhase: Boolean) {
        finished = movementPhase && checkLocation(ship, oceanMap)
        if (finished) {
            ship.task = null
            ship.behaviour = Behaviour.DEFAULT
        }
    }

    /**
     * returns the type of the task
     */
    override fun getType(): TaskType {
        return TaskType.COLLECT
    }
}
