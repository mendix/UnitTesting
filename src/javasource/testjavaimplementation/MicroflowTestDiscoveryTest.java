package testjavaimplementation;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import org.junit.Before;
import org.junit.Test;
import unittesting.TestManager;
import unittesting.proxies.TestSuite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MicroflowTestDiscoveryTest {
    private final IContext context = Core.createSystemContext();
    private final TestManager testManager = TestManager.instance();
    private TestSuite testSuite;

    @Before
    public void setup() {
        testSuite = testManager.findTestSuite(context, "UnitTesting").get();
    }

    @Test
    public void findMicroflowUnitTestsShouldFindMicroflowWithTestPrefix() {
        assertTrue(testManager.findMicroflowUnitTests(testSuite).stream().anyMatch(t -> t.startsWith("UnitTesting.TEST_")));
    }

    @Test
    public void findMicroflowUnitTestsShouldFindMicroflowWithUTPrefix() {
        assertTrue(testManager.findMicroflowUnitTests(testSuite).stream().anyMatch(t -> t.startsWith("UnitTesting.UT_")));
    }

    @Test
    public void findMicroflowUnitTestsShouldNotFindNonTestMicroflow() {
        String nonTestMicroflow = "UnitTesting.ReportStep";

        assertTrue(Core.getMicroflowNames().contains(nonTestMicroflow));
        assertFalse(testManager.findMicroflowUnitTests(testSuite).contains(nonTestMicroflow));
    }

    @Test
    public void findMicroflowUnitTestsShouldAllStartWithValidPrefix() {
        assertTrue(testManager.findMicroflowUnitTests(testSuite).stream()
                .allMatch(mf -> mf.startsWith("UnitTesting.UT_") || mf.startsWith("UnitTesting.TEST_")));
    }
}
