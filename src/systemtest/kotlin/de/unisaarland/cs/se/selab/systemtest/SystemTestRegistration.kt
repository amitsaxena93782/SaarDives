package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt0
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt1
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt2
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt3
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt4
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt5
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt6
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt7
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt8
import de.unisaarland.cs.se.selab.systemtest.eventDisrupt.EventDisrupt9
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping0
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping1
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping2
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping3
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping4
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping5
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping6
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping7
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping8
import de.unisaarland.cs.se.selab.systemtest.illegalDump.IllegalDumping9
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple0
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple1
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple2
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple3
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple4
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple5
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple6
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple7
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple8
import de.unisaarland.cs.se.selab.systemtest.refuelSimple.RefuelSimple9
import de.unisaarland.cs.se.selab.systemtest.runner.SystemTestManager
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest0
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest1
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest2
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest3
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest4
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest5
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest6
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest7
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest8
import de.unisaarland.cs.se.selab.systemtest.scamTests.ScamTest9
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario0
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario1
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario2
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario3
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario4
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario5
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario6
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario7
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario8
import de.unisaarland.cs.se.selab.systemtest.simpleScenario.SimpleScenario9
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple0
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple1
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple2
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple3
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple4
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple5
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple6
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple7
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple8
import de.unisaarland.cs.se.selab.systemtest.typhoonSimple.TyphoonSimple9
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple0
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple1
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple2
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple3
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple4
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple5
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple6
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple7
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple8
import de.unisaarland.cs.se.selab.systemtest.unloadSimple.UnloadSimple9

/**system test**/
object SystemTestRegistration {
    /**
     * Register your tests to run against the reference implementation!
     * This can also be used to debug our system test, or to see if we
     * understood something correctly or not (everything should work
     * the same as their reference implementation)
     * Should be exclusive with the other two methods!
     */
    fun registerSystemTestsReferenceImpl(manager: SystemTestManager) {
        // manager.registerTest(ExampleTest())
        manager.registerTest(SimpleTest())
        simpleScenarioTests(manager)
        unloadSimpleTests(manager)
        typhoonSimpleTests(manager)
        scamTests(manager)
        eventDisruptTests(manager)
        illegalDumpingTests(manager)
        manager.registerTest(AllStations())
        refuelSimpleTests(manager)
    }

    private fun simpleScenarioTests(manager: SystemTestManager) {
        manager.registerTest(SimpleScenario0())
        manager.registerTest(SimpleScenario1())
        manager.registerTest(SimpleScenario2())
        manager.registerTest(SimpleScenario3())
        manager.registerTest(SimpleScenario4())
        manager.registerTest(SimpleScenario5())
        manager.registerTest(SimpleScenario6())
        manager.registerTest(SimpleScenario7())
        manager.registerTest(SimpleScenario8())
        manager.registerTest(SimpleScenario9())
    }

    private fun typhoonSimpleTests(manager: SystemTestManager) {
        manager.registerTest(TyphoonSimple0())
        manager.registerTest(TyphoonSimple1())
        manager.registerTest(TyphoonSimple2())
        manager.registerTest(TyphoonSimple3())
        manager.registerTest(TyphoonSimple4())
        manager.registerTest(TyphoonSimple5())
        manager.registerTest(TyphoonSimple6())
        manager.registerTest(TyphoonSimple7())
        manager.registerTest(TyphoonSimple8())
        manager.registerTest(TyphoonSimple9())
    }

    private fun unloadSimpleTests(manager: SystemTestManager) {
        manager.registerTest(UnloadSimple0())
        manager.registerTest(UnloadSimple1())
        manager.registerTest(UnloadSimple2())
        manager.registerTest(UnloadSimple3())
        manager.registerTest(UnloadSimple4())
        manager.registerTest(UnloadSimple5())
        manager.registerTest(UnloadSimple6())
        manager.registerTest(UnloadSimple7())
        manager.registerTest(UnloadSimple8())
        manager.registerTest(UnloadSimple9())
    }

    private fun refuelSimpleTests(manager: SystemTestManager) {
        manager.registerTest(RefuelSimple0())
        manager.registerTest(RefuelSimple1())
        manager.registerTest(RefuelSimple2())
        manager.registerTest(RefuelSimple3())
        manager.registerTest(RefuelSimple4())
        manager.registerTest(RefuelSimple5())
        manager.registerTest(RefuelSimple6())
        manager.registerTest(RefuelSimple7())
        manager.registerTest(RefuelSimple8())
        manager.registerTest(RefuelSimple9())
    }

    private fun scamTests(manager: SystemTestManager) {
        manager.registerTest(ScamTest0())
        manager.registerTest(ScamTest1())
        manager.registerTest(ScamTest2())
        manager.registerTest(ScamTest3())
        manager.registerTest(ScamTest4())
        manager.registerTest(ScamTest5())
        manager.registerTest(ScamTest6())
        manager.registerTest(ScamTest7())
        manager.registerTest(ScamTest8())
        manager.registerTest(ScamTest9())
    }

    private fun eventDisruptTests(manager: SystemTestManager) {
        manager.registerTest(EventDisrupt0())
        manager.registerTest(EventDisrupt1())
        manager.registerTest(EventDisrupt2())
        manager.registerTest(EventDisrupt3())
        manager.registerTest(EventDisrupt4())
        manager.registerTest(EventDisrupt5())
        manager.registerTest(EventDisrupt6())
        manager.registerTest(EventDisrupt7())
        manager.registerTest(EventDisrupt8())
        manager.registerTest(EventDisrupt9())
    }
    private fun illegalDumpingTests(manager: SystemTestManager) {
        manager.registerTest(IllegalDumping0())
        manager.registerTest(IllegalDumping1())
        manager.registerTest(IllegalDumping2())
        manager.registerTest(IllegalDumping3())
        manager.registerTest(IllegalDumping4())
        manager.registerTest(IllegalDumping5())
        manager.registerTest(IllegalDumping6())
        manager.registerTest(IllegalDumping7())
        manager.registerTest(IllegalDumping8())
        manager.registerTest(IllegalDumping9())
    }

    /**
     * Register the tests you want to run against the validation mutants here!
     * The test only check validation, so they log messages will only possibly
     * be incorrect during the parsing/validation.
     * Everything after 'Simulation start' works correctly.
     * Should be exclusive with the other two methods!
     */
    fun registerSystemTestsMutantValidation(manager: SystemTestManager) {
        // manager.registerTest(ExampleTest())
        manager.registerTest(SimpleTest())
    }

    /**
     * The same as above, but the log message only (possibly) become incorrect
     * from the 'Simulation start' log onwards
     * Should be exclusive with the other two methods!
     */
    fun registerSystemTestsMutantSimulation(manager: SystemTestManager) {
        // manager.registerTest(ExampleTest())
        manager.registerTest(SimpleTest())
        manager.registerTest(SimpleScenario9())
        manager.registerTest(UnloadSimple9())
        manager.registerTest(TyphoonSimple9())
        manager.registerTest(RefuelSimple9())
        manager.registerTest(IllegalDumping9())
        manager.registerTest(AllStations())
        manager.registerTest(ScamTest9())
        manager.registerTest(EventDisrupt9())
    }
}
