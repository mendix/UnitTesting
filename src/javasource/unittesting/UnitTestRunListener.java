package unittesting;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import com.mendix.logging.ILogNode;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.mendix.systemwideinterfaces.core.IContext;

import unittesting.proxies.TestSuite;
import unittesting.proxies.UnitTest;
import unittesting.proxies.ENUM_UnitTestResult;

public class UnitTestRunListener extends RunListener {
	private static final ILogNode LOG = ConfigurationManager.LOG;

	private IContext context;
	private TestSuite testSuite;

	public UnitTestRunListener(IContext context, TestSuite testSuite) {
		this.context = context;
		this.testSuite = testSuite;
	}

	@Override
	public void testRunStarted(Description description) {
		LOG.info("Starting test run");
	}

	@Override
	public void testRunFinished(Result result) {
		LOG.info("Test run finished");
	}

	@Override
	public void testStarted(Description description) throws Exception {
		String message = "Starting JUnit test " + description.getClassName() + "." + description.getMethodName();
		LOG.info(message);
		TestManager.instance().reportStep(message);

		UnitTest t = getUnitTest(description);
		t.setResult(ENUM_UnitTestResult._1_Running);
		t.setResultMessage("");
		t.setLastRun(new Date());
		t.commit();
	}

	private UnitTest getUnitTest(Description description) {
		return TestManager.instance().getJUnitTest(context, testSuite, description);
	}

	@Override
	public void testFinished(Description description) throws Exception {
		LOG.info("Finished test " + description.getClassName() + "." + description.getMethodName());

		UnitTest t = getUnitTest(description);

		if (t.getResult() == ENUM_UnitTestResult._1_Running) {
			t.setResult(ENUM_UnitTestResult._3_Success);
			t.setResultMessage("JUnit test completed successfully");
			t.setReadableTime(getReadableTime(description, t));

			testSuite.setTestPassedCount(testSuite.getTestPassedCount() + 1);
			testSuite.commit();
		}

		t.setLastStep(TestManager.instance().getLastReportedStep());
		t.commit();
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		UnitTest t = getUnitTest(failure.getDescription());

		/**
		 * Test itself failed
		 */
		LOG.error("Failed test (at step '" + TestManager.instance().getLastReportedStep() + "') "
				+ failure.getDescription().getClassName() + "." + failure.getDescription().getMethodName() + " : "
				+ failure.getMessage(), failure.getException());

		testSuite.setTestFailedCount(testSuite.getTestFailedCount() + 1);
		testSuite.commit();

		t.setResult(ENUM_UnitTestResult._2_Failed);
		t.setResultMessage(getFailureMessage(failure));
		t.setStackTrace(failure.getTrace());
		t.setReadableTime(getReadableTime(failure.getDescription(), t));
		t.setLastStep(TestManager.instance().getLastReportedStep());
		t.setLastRun(new Date());
		t.commit();
	}

	private String getFailureMessage(Failure failure) {
		String message = String.format("JUnit test failed at %s", findProperExceptionLine(failure.getTrace()));
		return failure.getMessage() != null ? message + ": " + failure.getMessage() : message;
	}

	private String getReadableTime(Description description, UnitTest t) throws Exception {
		long delta = getUnitTestInnerTime(description, t);
		return TestManager.formatAsReadableTime(delta);
	}

	private long getUnitTestInnerTime(Description description, UnitTest t) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		long delta = System.currentTimeMillis() - t.getLastRun().getTime();

		if (AbstractUnitTest.class.isAssignableFrom(description.getTestClass()))
			delta = (Long) description.getTestClass().getMethod("getTestRunTime").invoke(null);
		return delta;
	}

	private String findProperExceptionLine(String trace) {
		String[] lines = trace.split("\n");

		if (lines.length > 2) {
			for (int i = 1; i < lines.length; i++) {
				String line = lines[i].trim();
				if (!line.startsWith("at org.junit") && line.contains("("))
					return line.substring(line.indexOf('(') + 1, line.indexOf(')')).replace(":", " line ");
			}
		}

		return "";
	}
}
