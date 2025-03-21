package testjavaimplementation;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import unittesting.TestExecutionContext;

import static org.junit.Assert.*;

public class TestExecutionContextTest {
    private final IContext context = Core.createSystemContext();
    private TestExecutionContext testExecutionContext;

    @Before
    public void setup() {
        this.testExecutionContext = new TestExecutionContext(context, "ExampleMicroflow");
    }

    @After
    public void tearDown() {
        this.testExecutionContext.delete();
        this.testExecutionContext = null;
    }

    @Test
    public void testExecutionContextShouldInitializeUnitTestContext() {
        assertNotNull(this.testExecutionContext.getUnitTestContext());
    }

    @Test
    public void hasFailedAssertionShouldBeFalseByDefault() {
        assertFalse(this.testExecutionContext.hasFailedAssertion(context));
    }

    @Test
    public void hasFailedAssertionShouldBeFalseWhenAllAssertionsPassed() {
        this.testExecutionContext.collectAssertion(context, "Passed 1", true, null);
        this.testExecutionContext.collectAssertion(context, "Passed 2", true, null);

        assertFalse(this.testExecutionContext.hasFailedAssertion(context));
    }

    @Test
    public void hasFailedAssertionShouldBeTrueWhenCollectingFailedAssertion() {
        this.testExecutionContext.collectAssertion(context, "Passed", true, null);
        this.testExecutionContext.collectAssertion(context, "Failed", false, null);

        assertTrue(this.testExecutionContext.hasFailedAssertion(context));
    }

    @Test
    public void getLastStepShouldBeNullByDefault() {
        assertNull(this.testExecutionContext.getLastStep());
    }

    @Test
    public void getLastStepShouldReturnLastCollectedStep() {
        this.testExecutionContext.collectStep("Step 1");
        this.testExecutionContext.collectStep("Step 2");

        assertEquals("Step 2", this.testExecutionContext.getLastStep().getMessage());
    }

    @Test
    public void getFailureReasonsShouldBeNullByDefault() {
        assertEquals(0, this.testExecutionContext.getFailureReasons(context).size());
    }

    @Test
    public void getFailureReasonsShouldBeNullIfTestPassed() {
        this.testExecutionContext.collectStart(true, null);
        this.testExecutionContext.collectAssertion(context, "Passed 1", true, null);
        this.testExecutionContext.collectAssertion(context, "Passed 2", true, null);
        this.testExecutionContext.collectEnd(true, null);

        assertEquals(0, this.testExecutionContext.getFailureReasons(context).size());
    }

    @Test
    public void getFailureReasonsShouldIncludeFailedStart() {
        this.testExecutionContext.collectStart(false, "Start failed");

        assertEquals(1, this.testExecutionContext.getFailureReasons(context).size());
    }

    @Test
    public void getFailureReasonsShouldIncludeFailedEnd() {
        this.testExecutionContext.collectEnd(false, "End failed");

        assertEquals(1, this.testExecutionContext.getFailureReasons(context).size());
    }

    @Test
    public void getFailureReasonsShouldIncludeFailedAssertion() {
        this.testExecutionContext.collectAssertion(context, "Passed", true, null);
        this.testExecutionContext.collectAssertion(context, "Failed 1", false, null);
        this.testExecutionContext.collectAssertion(context, "Failed 2", false, null);

        assertEquals(1, this.testExecutionContext.getFailureReasons(context).size());
    }

    @Test
    public void getFailureReasonsShouldIncludeUncaughtException() {
        this.testExecutionContext.collectException(new Exception("Example Exception"));

        assertEquals(1, this.testExecutionContext.getFailureReasons(context).size());
    }

    @Test
    public void getFailureReasonsShouldIncludeAllUniqueFailureReasons() {
        this.testExecutionContext.collectStart(false, "Start failed");
        this.testExecutionContext.collectAssertion(context, "Failed 1", false, null);
        this.testExecutionContext.collectAssertion(context, "Failed 2", false, null);
        this.testExecutionContext.collectException(new Exception("Test Exception"));
        this.testExecutionContext.collectEnd(false, "End failed");

        assertEquals(4, this.testExecutionContext.getFailureReasons(context).size());
    }

    @Test
    public void createTestActivitiesShouldContainAllCollectedTestActivities() {
        this.testExecutionContext.collectStart(true, "Started test");

        this.testExecutionContext.collectAssertion(context, "Passed", true, null);
        this.testExecutionContext.collectAssertion(context, "Failed 1", false, null);
        this.testExecutionContext.collectAssertion(context, "Failed 2", false, null);

        this.testExecutionContext.collectStep("Step 1");
        this.testExecutionContext.collectStep("Step 2");

        this.testExecutionContext.collectException(new Exception("Example Exception"));

        this.testExecutionContext.collectEnd(true, "Test ended successfully");

        assertEquals(8, this.testExecutionContext.createTestActivities(context, null).size());
    }
}
