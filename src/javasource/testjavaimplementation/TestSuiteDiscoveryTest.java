package testjavaimplementation;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import org.junit.Test;
import unittesting.TestManager;

import static org.junit.Assert.assertTrue;

public class TestSuiteDiscoveryTest {
    private final IContext context = Core.createSystemContext();
    private final TestManager testManager = TestManager.instance();

    @Test
    public void findTestSuiteShouldNotFindModuleWithoutTests() {
        assertTrue(testManager.findTestSuite(context, "ModuleWithoutTests").isEmpty());
    }

    @Test
    public void findTestSuiteShouldFindModuleWithTests() {
        assertTrue(testManager.findTestSuite(context, "SuiteWithReturnValues").isPresent());
    }
}