package unittesting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import unittesting.activities.AssertActivity;
import unittesting.activities.StartEndExecutionActivity;
import unittesting.activities.StepExecutionActivity;
import unittesting.activities.ExceptionExecutionActivity;

import unittesting.proxies.*;

public class TestExecutionContext {
    private final ArrayList<AssertActivity> assertions = new ArrayList<>();
    private final ArrayList<StepExecutionActivity> steps = new ArrayList<>();

    private UnitTestContext unitTestContext;
    private StartEndExecutionActivity startActivity;
    private StartEndExecutionActivity endActivity;
    private ExceptionExecutionActivity exceptionActivity;
    private int activitySequence = 0;

    private static final ILogNode LOG = ConfigurationManager.LOG;

    public void setUnitTestContext(UnitTestContext unitTestContext) {
        this.unitTestContext = unitTestContext;
    }

    public UnitTestContext getUnitTestContext() {
        return this.unitTestContext;
    }

    public void collectStart(boolean result, String message) {
        this.startActivity = new StartEndExecutionActivity(getNextSequence(), result, message);
    }

    public void collectEnd(boolean result, String message) {
        this.endActivity = new StartEndExecutionActivity(getNextSequence(), result, message);
    }

    public void collectException(Exception exception) {
    	this.exceptionActivity = new ExceptionExecutionActivity(getNextSequence(), exception);
    }

    public AssertActivity collectAssertion(String name, boolean result, String failureMessage) {
        AssertActivity assertion = new AssertActivity(getNextSequence(), name, result, failureMessage);
        this.assertions.add(assertion);

        return assertion;
    }

    public void collectStep(String message) {
        LOG.debug("Report step: " + message);
        this.steps.add(new StepExecutionActivity(getNextSequence(), message));
    }

    public StepExecutionActivity getLastStep() {
        return !this.steps.isEmpty() ? this.steps.get(this.steps.size() - 1) : null;
    }

    public boolean hasFailedAssertion() {
        return this.assertions.stream().anyMatch(AssertActivity::isFailed);
    }

    public ArrayList<String> getFailureReasons() {
        ArrayList<String> failures = new ArrayList<>();

        if (this.startActivity != null && !this.startActivity.getResult())
            failures.add("Failed to start test");

        if (this.hasFailedAssertion())
            failures.add("One or more assertions failed");

        if (this.endActivity != null && !this.endActivity.getResult())
            failures.add("Microflow return value is incorrect");

        if (this.exceptionActivity != null)
            failures.add("An uncaught exception occurred");

        return failures;
    }

    public String getResultSummary() {
        ArrayList<String> failureReasons = this.getFailureReasons();

        if (failureReasons.isEmpty()) {
            return "Microflow completed successfully";
        } else {
            StringBuilder message = new StringBuilder();

            for (int i = 0; i < failureReasons.size(); i++) {
                if (i > 0) message.append("\n");
                message.append(failureReasons.get(i));
            }

            return message.toString();
        }
    }

    public void persistTestActivities(UnitTest test) {
        IContext systemContext = Core.createSystemContext();
        systemContext.startTransaction();

        List<IMendixObject> activities = this.createTestActivities(systemContext, test);
        Core.commit(systemContext, activities);

        systemContext.endTransaction();
    }

    public List<IMendixObject> createTestActivities(IContext context, UnitTest test) {
        List<IMendixObject> activities = new ArrayList<>();

        if (this.startActivity != null)
            activities.add(createStartActivity(context, test));

        activities.addAll(createAssertionActivities(context, test));
        activities.addAll(createStepActivities(context, test));

        if (this.endActivity != null)
            activities.add(createEndActivity(context, test));

        if (this.exceptionActivity != null)
            activities.add(createExceptionActivity(context, test));

        return activities;
    }

    private List<IMendixObject> createAssertionActivities(IContext context, UnitTest test) {
        return this.assertions.stream()
                .map(assertion -> createAssertionActivity(context, test, assertion))
                .collect(Collectors.toList());
    }

    private List<IMendixObject> createStepActivities(IContext context, UnitTest test) {
        return this.steps.stream()
                .map(step -> createStepActivity(context, test, step))
                .collect(Collectors.toList());
    }

    private IMendixObject createStartActivity(IContext context, UnitTest test) {
        if (this.startActivity == null)
            throw new IllegalStateException("No start activity collected");

        StartActivity startActivity = new StartActivity(context);
        startActivity.setTestActivity_UnitTest(test);
        startActivity.setSequence(this.startActivity.getSequence());
        startActivity.setResult(this.startActivity.getResultEnum());
        startActivity.setMessage(this.startActivity.getMessage());

        return startActivity.getMendixObject();
    }

    private IMendixObject createEndActivity(IContext context, UnitTest test) {
        if (this.endActivity == null)
            throw new IllegalStateException("No end activity collected");

        EndActivity endActivity = new EndActivity(context);
        endActivity.setTestActivity_UnitTest(test);
        endActivity.setSequence(this.endActivity.getSequence());
        endActivity.setResult(this.endActivity.getResultEnum());
        endActivity.setMessage(this.endActivity.getMessage());

        return endActivity.getMendixObject();
    }

    private IMendixObject createExceptionActivity(IContext context, UnitTest test) {
    	if (this.exceptionActivity == null)
    		throw new IllegalStateException("No exception collected");

        Exception exception = this.exceptionActivity.getException();
		Throwable cause = ExceptionUtils.getRootCause(exception);

        ExceptionActivity exceptionActivity = new ExceptionActivity(context);
        exceptionActivity.setTestActivity_UnitTest(test);
        exceptionActivity.setSequence(this.exceptionActivity.getSequence());
        exceptionActivity.setMessage(cause.getMessage());
        exceptionActivity.setStackTrace(ExceptionUtils.getStackTrace(exception));

        return exceptionActivity.getMendixObject();
    }

    private IMendixObject createAssertionActivity(IContext context, UnitTest test, AssertActivity assertion) {
        AssertionActivity assertionActivity = new AssertionActivity(context);
        assertionActivity.setTestActivity_UnitTest(test);
        assertionActivity.setSequence(assertion.getSequence());
        assertionActivity.setName(assertion.getName());
        assertionActivity.setResult(assertion.getResultEnum());
        assertionActivity.setMessage(assertion.getMessage());

        return assertionActivity.getMendixObject();
    }

    private IMendixObject createStepActivity(IContext context, UnitTest test, StepExecutionActivity step) {
        StepActivity stepActivity = new StepActivity(context);
        stepActivity.setTestActivity_UnitTest(test);
        stepActivity.setSequence(step.getSequence());
        stepActivity.setMessage(step.getMessage());

        return stepActivity.getMendixObject();
    }

    public void clearTestActivities(UnitTest test) {
        IContext systemContext = Core.createSystemContext();

        StringBuilder query = new StringBuilder();
        query.append(String.format("//%s", TestActivity.entityName));
        query.append(String.format("[%s=$UnitTest]", TestActivity.MemberNames.TestActivity_UnitTest));

        List<IMendixObject> activities = Core.createXPathQuery(query.toString())
                .setVariable("UnitTest", test.getMendixObject().getId().toLong()).execute(systemContext);

        Core.delete(systemContext, activities);
    }

    private int getNextSequence() {
        return activitySequence++;
    }
}
