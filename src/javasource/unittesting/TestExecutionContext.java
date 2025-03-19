package unittesting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import unittesting.activities.StartEndExecutionActivity;
import unittesting.activities.StepExecutionActivity;
import unittesting.activities.ExceptionExecutionActivity;

import unittesting.proxies.*;

public class TestExecutionContext {
    private final UnitTestContext unitTestContext;
    private final ArrayList<StepExecutionActivity> steps = new ArrayList<>();

    private StartEndExecutionActivity startActivity;
    private StartEndExecutionActivity endActivity;
    private ExceptionExecutionActivity exceptionActivity;
    private int activitySequence = 0;

    private static final ILogNode LOG = ConfigurationManager.LOG;

    public TestExecutionContext(IContext context, String mf) {
        UnitTestContext testContext = new UnitTestContext(context);
        testContext.setTestName(mf);

        LOG.trace("Created unit test context for " + mf);
        this.unitTestContext = testContext;
    }

    public UnitTestContext getUnitTestContext() {
        return this.unitTestContext;
    }

    public void delete() {
        if (this.unitTestContext == null)
            return;

        this.unitTestContext.delete();
        LOG.trace("Deleted unit test context");
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

    public Assertion collectAssertion(IContext context, String name, boolean result, String failureMessage) {
        ENUM_UnitTestResult testResult = result ? ENUM_UnitTestResult._3_Success : ENUM_UnitTestResult._2_Failed;

        Assertion assertion = new Assertion(context);
        assertion.setAssertion_UnitTestContext(this.unitTestContext);
        assertion.setSequence(getNextSequence());
        assertion.setName(name);
        assertion.setResult(testResult);

        if (testResult.equals(ENUM_UnitTestResult._2_Failed))
            assertion.setMessage(failureMessage);

        return assertion;
    }

    public void collectStep(String message) {
        LOG.debug("Report step: " + message);
        this.steps.add(new StepExecutionActivity(getNextSequence(), message));
    }

    public StepExecutionActivity getLastStep() {
        return !this.steps.isEmpty() ? this.steps.get(this.steps.size() - 1) : null;
    }

    public boolean hasFailedAssertion(IContext context) {
        List<IMendixObject> assertionList = Core.retrieveByPath(context, this.unitTestContext.getMendixObject(),
                Assertion.MemberNames.Assertion_UnitTestContext.toString());

        return assertionList.stream().anyMatch(obj -> isFailedAssertion(context, obj));
    }

    private boolean isFailedAssertion(IContext context, IMendixObject object) {
        return ENUM_UnitTestResult._2_Failed.equals(Assertion.initialize(context, object).getResult());
    }

    public ArrayList<String> getFailureReasons(IContext context) {
        ArrayList<String> failures = new ArrayList<>();

        if (this.startActivity != null && !this.startActivity.getResult())
            failures.add("Failed to start test");

        if (this.hasFailedAssertion(context))
            failures.add("One or more assertions failed");

        if (this.endActivity != null && !this.endActivity.getResult())
            failures.add("Microflow return value is incorrect");

        if (this.exceptionActivity != null)
            failures.add("An uncaught exception occurred");

        return failures;
    }

    public String getResultSummary(IContext context) {
        ArrayList<String> failureReasons = this.getFailureReasons(context);

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

    public void persistTestActivities(IContext context, UnitTest test) {
        List<IMendixObject> activities = this.createTestActivities(context, test);
        Core.commit(Core.createSystemContext(), activities);
    }

    public List<IMendixObject> createTestActivities(IContext context, UnitTest test) {
        List<IMendixObject> activities = new ArrayList<>();

        if (this.startActivity != null)
            activities.add(createStartActivity(test));

        activities.addAll(createAssertionActivities(context, test));
        activities.addAll(createStepActivities(test));

        if (this.endActivity != null)
            activities.add(createEndActivity(test));

        if (this.exceptionActivity != null)
            activities.add(createExceptionActivity(test));

        return activities;
    }

    private List<IMendixObject> createAssertionActivities(IContext context, UnitTest test) {
        List<IMendixObject> assertionList = Core.retrieveByPath(context, this.unitTestContext.getMendixObject(),
                Assertion.MemberNames.Assertion_UnitTestContext.toString());

        return assertionList.stream()
                .map(assertion -> createAssertionActivity(test, assertion))
                .collect(Collectors.toList());
    }

    private List<IMendixObject> createStepActivities(UnitTest test) {
        return this.steps.stream()
                .map(step -> createStepActivity(test, step))
                .collect(Collectors.toList());
    }

    private IMendixObject createStartActivity(UnitTest test) {
        if (this.startActivity == null)
            throw new IllegalStateException("No start activity collected");

        StartActivity startActivity = new StartActivity(Core.createSystemContext());
        startActivity.setTestActivity_UnitTest(test);
        startActivity.setSequence(this.startActivity.getSequence());
        startActivity.setResult(this.startActivity.getResultEnum());
        startActivity.setMessage(this.startActivity.getMessage());

        return startActivity.getMendixObject();
    }

    private IMendixObject createEndActivity(UnitTest test) {
        if (this.endActivity == null)
            throw new IllegalStateException("No end activity collected");

        EndActivity endActivity = new EndActivity(Core.createSystemContext());
        endActivity.setTestActivity_UnitTest(test);
        endActivity.setSequence(this.endActivity.getSequence());
        endActivity.setResult(this.endActivity.getResultEnum());
        endActivity.setMessage(this.endActivity.getMessage());

        return endActivity.getMendixObject();
    }

    private IMendixObject createExceptionActivity(UnitTest test) {
    	if (this.exceptionActivity == null)
    		throw new IllegalStateException("No exception collected");

        Exception exception = this.exceptionActivity.getException();
		Throwable cause = ExceptionUtils.getRootCause(exception);

        ExceptionActivity exceptionActivity = new ExceptionActivity(Core.createSystemContext());
        exceptionActivity.setTestActivity_UnitTest(test);
        exceptionActivity.setSequence(this.exceptionActivity.getSequence());
        exceptionActivity.setMessage(cause.getMessage());
        exceptionActivity.setStackTrace(ExceptionUtils.getStackTrace(exception));

        return exceptionActivity.getMendixObject();
    }

    private IMendixObject createAssertionActivity(UnitTest test, IMendixObject assertionObj) {
        IContext systemContext = Core.createSystemContext();
        Assertion assertion = Assertion.initialize(systemContext, assertionObj);

        AssertionActivity assertionActivity = new AssertionActivity(systemContext);
        assertionActivity.setTestActivity_UnitTest(test);
        assertionActivity.setSequence(assertion.getSequence());
        assertionActivity.setName(assertion.getName());
        assertionActivity.setResult(assertion.getResult());
        assertionActivity.setMessage(assertion.getMessage());

        return assertionActivity.getMendixObject();
    }

    private IMendixObject createStepActivity(UnitTest test, StepExecutionActivity step) {
        StepActivity stepActivity = new StepActivity(Core.createSystemContext());
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
