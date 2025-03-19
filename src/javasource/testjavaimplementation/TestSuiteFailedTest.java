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

public class TestSuiteFailedTest {
    private final IContext context = Core.createSystemContext();

    private TestSuite testSuite;

    private UnitTest unitTestFailed;
    private UnitTest unitTestPassed;
    private UnitTest unitTestNotRun;

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

        unitTestFailed = createUnitTest("UnitTestFailed", ENUM_UnitTestResult._2_Failed);
        unitTestPassed = createUnitTest("UnitTestSuccess", ENUM_UnitTestResult._3_Success);
        unitTestNotRun = createUnitTest("UnitTestNotRun", null);

        TestManager.instance().updateTestSuiteCountersAndResult(context, testSuite, true);
    }

    @After
    public void tearDown() {
        unitTestFailed.delete();
        unitTestPassed.delete();
        unitTestNotRun.delete();

        testSuite.delete();
    }

    @Test
    public void testSuiteFailedCountShouldBeOne() {
        assertEquals(1, (long) testSuite.getTestFailedCount());
    }

    @Test
    public void testSuitePassedCountShouldBeOne() {
        assertEquals(1, (long) testSuite.getTestPassedCount());
    }

    @Test
    public void testSuiteResultShouldBeFailed() {
        assertEquals(ENUM_UnitTestResult._2_Failed, testSuite.getResult());
    }
}
