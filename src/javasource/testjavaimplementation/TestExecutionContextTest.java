package testjavaimplementation;

import com.mendix.core.Core;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import unittesting.TestExecutionContext;
import unittesting.activities.AssertActivity;

import static org.junit.Assert.*;

public class TestExecutionContextTest {
    private static final String FAILURE_MESSAGE = "This is a failure message";
    private TestExecutionContext testExecutionContext;

    @Before
    public void setup() {
        this.testExecutionContext = new TestExecutionContext();
    }

    @After
    public void tearDown() {
        this.testExecutionContext = null;
    }

    @Test
    public void hasFailedAssertionShouldBeFalseByDefault() {
        assertFalse(this.testExecutionContext.hasFailedAssertion());
    }

    @Test
    public void hasFailedAssertionShouldBeFalseWhenAllAssertionsPassed() {
        this.testExecutionContext.collectAssertion("Passed 1", true, null);
        this.testExecutionContext.collectAssertion("Passed 2", true, null);

        assertFalse(this.testExecutionContext.hasFailedAssertion());
    }

    @Test
    public void hasFailedAssertionShouldBeTrueWhenCollectingFailedAssertion() {
        this.testExecutionContext.collectAssertion("Passed", true, null);
        this.testExecutionContext.collectAssertion("Failed", false, null);

        assertTrue(this.testExecutionContext.hasFailedAssertion());
    }

    @Test
    public void assertActivityShouldContainFailureMessageWhenCollectingFailedAssertion() {
        AssertActivity assertion = this.testExecutionContext.collectAssertion("Failed", false, FAILURE_MESSAGE);

        assertEquals(FAILURE_MESSAGE, assertion.getMessage());
    }

    @Test
    public void assertActivityShouldNotContainFailureMessageWhenCollectingPassedAssertion() {
        AssertActivity assertion = this.testExecutionContext.collectAssertion("Passed", true, FAILURE_MESSAGE);

        assertNull(assertion.getMessage());
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
        assertEquals(0, this.testExecutionContext.getFailureReasons().size());
    }

    @Test
    public void getFailureReasonsShouldBeNullIfTestPassed() {
        this.testExecutionContext.collectStart(true, null);
        this.testExecutionContext.collectAssertion("Passed 1", true, null);
        this.testExecutionContext.collectAssertion("Passed 2", true, null);
        this.testExecutionContext.collectEnd(true, null);

        assertEquals(0, this.testExecutionContext.getFailureReasons().size());
    }

    @Test
    public void getFailureReasonsShouldIncludeFailedStart() {
        this.testExecutionContext.collectStart(false, "Start failed");

        assertEquals(1, this.testExecutionContext.getFailureReasons().size());
    }

    @Test
    public void getFailureReasonsShouldIncludeFailedEnd() {
        this.testExecutionContext.collectEnd(false, "End failed");

        assertEquals(1, this.testExecutionContext.getFailureReasons().size());
    }

    @Test
    public void getFailureReasonsShouldIncludeFailedAssertion() {
        this.testExecutionContext.collectAssertion("Passed", true, null);
        this.testExecutionContext.collectAssertion("Failed 1", false, null);
        this.testExecutionContext.collectAssertion("Failed 2", false, null);

        assertEquals(1, this.testExecutionContext.getFailureReasons().size());
    }

    @Test
    public void getFailureReasonsShouldIncludeUncaughtException() {
        this.testExecutionContext.collectException(new Exception("Example Exception"));

        assertEquals(1, this.testExecutionContext.getFailureReasons().size());
    }

    @Test
    public void getFailureReasonsShouldIncludeAllUniqueFailureReasons() {
        this.testExecutionContext.collectStart(false, "Start failed");
        this.testExecutionContext.collectAssertion("Failed 1", false, null);
        this.testExecutionContext.collectAssertion("Failed 2", false, null);
        this.testExecutionContext.collectException(new Exception("Test Exception"));
        this.testExecutionContext.collectEnd(false, "End failed");

        assertEquals(4, this.testExecutionContext.getFailureReasons().size());
    }

    @Test
    public void createTestActivitiesShouldContainAllCollectedTestActivities() {
        this.testExecutionContext.collectStart(true, "Started test");

        this.testExecutionContext.collectAssertion("Passed", true, null);
        this.testExecutionContext.collectAssertion("Failed 1", false, null);
        this.testExecutionContext.collectAssertion("Failed 2", false, null);

        this.testExecutionContext.collectStep("Step 1");
        this.testExecutionContext.collectStep("Step 2");

        this.testExecutionContext.collectException(new Exception("Example Exception"));

        this.testExecutionContext.collectEnd(true, "Test ended successfully");

        assertEquals(8, this.testExecutionContext.createTestActivities(Core.createSystemContext(), null).size());
    }
}
