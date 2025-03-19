package testjavaimplementation;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import unittesting.TestManager;
import unittesting.proxies.ENUM_UnitTestResult;
import unittesting.proxies.TestSuite;
import unittesting.proxies.UnitTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestSuiteChangedTest {
    private final IContext context = Core.createSystemContext();

    private TestSuite testSuite;

    private UnitTest unitTestFailed;
    private UnitTest unitTestPassed;

    private void createTestSuite() throws CoreException {
        testSuite = new TestSuite(context);
        testSuite.setModule("TestModule1");
        testSuite.commit();
    }

    private UnitTest createUnitTest(String name, ENUM_UnitTestResult result) throws CoreException {
        UnitTest unitTest = new UnitTest(context);
        unitTest.setUnitTest_TestSuite(testSuite);
        unitTest.setName(name);
        unitTest.setResult(result);
        unitTest.commit();

        return unitTest;
    }

    @Before
    public void setup() throws CoreException {
        createTestSuite();
    }

    @After
    public void tearDown() {
        if (unitTestFailed != null) unitTestFailed.delete();
        if (unitTestPassed != null) unitTestPassed.delete();

        testSuite.delete();
    }

    @Test
    public void testSuiteResultShouldBeEmptyByDefault() throws CoreException {
        TestManager.instance().updateTestSuiteCountersAndResult(context, testSuite, true);
        assertNull(testSuite.getResult());
    }

    @Test
    public void testSuiteResultShouldResetAfterRemovingAllFailed() throws CoreException {
        unitTestFailed = createUnitTest("UnitTestFailed", ENUM_UnitTestResult._2_Failed);
        TestManager.instance().updateTestSuiteCountersAndResult(context, testSuite, true);
        assertEquals(ENUM_UnitTestResult._2_Failed, testSuite.getResult());

        unitTestFailed.delete();

        TestManager.instance().updateTestSuiteCountersAndResult(context, testSuite, true);
        assertNull(testSuite.getResult());
    }

    @Test
    public void testSuiteResultShouldBeSuccessAfterRemovingAllFailed() throws CoreException {
        unitTestPassed = createUnitTest("UnitTestPassed", ENUM_UnitTestResult._3_Success);
        unitTestFailed = createUnitTest("UnitTestFailed", ENUM_UnitTestResult._2_Failed);
        TestManager.instance().updateTestSuiteCountersAndResult(context, testSuite, true);
        assertEquals(ENUM_UnitTestResult._2_Failed, testSuite.getResult());

        unitTestFailed.delete();

        TestManager.instance().updateTestSuiteCountersAndResult(context, testSuite, true);
        assertEquals(ENUM_UnitTestResult._3_Success, testSuite.getResult());

        unitTestPassed.delete();
    }
}
