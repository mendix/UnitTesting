package testjavaimplementation;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import org.junit.Test;
import unittesting.TestManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HasSetupTeardownTest {
    private final IContext context = Core.createSystemContext();
    private final TestManager testManager = TestManager.instance();

    @Test
    public void hasSetupShouldReturnTrueForSuiteWithSetup() {
        assertTrue(testManager.hasMfSetup(testManager.findTestSuite(context,"SuiteWithSetup").get()));
        assertTrue(testManager.hasMfSetup(testManager.findTestSuite(context,"SuiteWithSetupAndTearDown").get()));
    }

    @Test
    public void hasSetupShouldReturnFalseForSuiteWithoutSetup() {
        assertFalse(testManager.hasMfSetup(testManager.findTestSuite(context,"SuiteWithTearDown").get()));
    }

    @Test
    public void hasTeardownShouldReturnTrueForSuiteWithTeardown() {
        assertTrue(testManager.hasMfTearDown(testManager.findTestSuite(context,"SuiteWithTearDown").get()));
        assertTrue(testManager.hasMfTearDown(testManager.findTestSuite(context,"SuiteWithSetupAndTearDown").get()));
    }

    @Test
    public void hasTeardownShouldReturnFalseForSuiteWithoutTeardown() {
        assertFalse(testManager.hasMfTearDown(testManager.findTestSuite(context,"SuiteWithSetup").get()));
    }
}
