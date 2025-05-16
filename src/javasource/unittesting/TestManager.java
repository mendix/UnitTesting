package unittesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.core.actionmanagement.MicroflowCallBuilder;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IDataType;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import unittesting.proxies.ENUM_UnitTestResult;
import unittesting.proxies.TestSuite;
import unittesting.proxies.UnitTest;
import unittesting.proxies.UnitTestContext;

/**
 * @author mwe
 *
 */
public class TestManager {
	/**
	 * Test manager introduces its own exception, because the AssertionExceptions
	 * from JUnit are not picked up properly by the runtime in 4.1.1 and escape all
	 * exception handling defined inside microflows :-S
	 * 
	 * @author mwe
	 *
	 */
	public static class AssertionException extends Exception {
		private static final long serialVersionUID = -3115796226784699883L;

		public AssertionException(String message) {
			super(message);
		}
	}

	private static final ILogNode LOG = ConfigurationManager.LOG;
	private static final String TEST_CONTEXT_PARAM_NAME = "UnitTestContext";

	private static TestManager instance;

	private IContext setupContext;
	private TestExecutionContext executionContext;
	private String lastStep; // Only applicable for JUnit tests

	public static TestManager instance() {
		if (instance == null)
			instance = new TestManager();
		return instance;
	}

	public TestExecutionContext executionContext() {
		if (executionContext == null)
			throw new IllegalStateException("No execution context available");

		return executionContext;
	}

	public synchronized void runTest(IContext context, UnitTest unitTest) throws ClassNotFoundException, CoreException {
		if (!ConfigurationManager.verifyModuleIsEnabled()) return;

		TestSuite testSuite = unitTest.getUnitTest_TestSuite();

		/**
		 * Is Mf
		 */
		if (unitTest.getIsMf()) {
			try {
				runMfSetup(testSuite);
				runMicroflowTest(unitTest.getName(), unitTest);
			} finally {
				runMfTearDown(testSuite);
			}
		}

		/**
		 * Is java
		 */
		else {
			String[] parts = unitTest.getName().split("/");
			Request request;
			if (parts.length == 1) // class-scale test run
				request = Request.aClass(Class.forName(parts[0]));
			else if (parts.length == 2) // method-scale test run
				request = Request.method(Class.forName(parts[0]), parts[1]);
			else
				throw new CoreException("Invalid test specification: " + unitTest.getName()
						+ "\nTest method run should be defined in either form $testClass or $testClass/$testMethod.");

			JUnitCore junit = new JUnitCore();
			junit.addListener(new UnitTestRunListener(context, testSuite));

			junit.run(request);
		}

		updateTestSuiteCountersAndResult(context, testSuite, true);
	}

	private void runMfSetup(TestSuite testSuite) {
		if (hasMfSetup(testSuite)) {
			try {
				LOG.info("Running Setup microflow..");

				setupContext = Core.createSystemContext();
				setupContext.startTransaction();
				LOG.trace("Start transaction for setup");
				Core.microflowCall(testSuite.getModule() + ".Setup").execute(setupContext);
			} catch (Exception e) {
				LOG.error("Exception during Setup microflow: " + e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
	}

	private void runMfTearDown(TestSuite testSuite) {
		IContext tearDownContext = setupContext;

		if (hasMfTearDown(testSuite)) {
			try {
				LOG.info("Running TearDown microflow..");

				if (tearDownContext == null) {
					tearDownContext = Core.createSystemContext();
					tearDownContext.startTransaction();
					LOG.trace("Start transaction for teardown");
				}

				Core.microflowCall(testSuite.getModule() + ".TearDown").execute(tearDownContext);
			} catch (Exception e) {
				LOG.error("Severe: exception in unittest TearDown microflow '" + testSuite.getModule() + ".TearDown': "
						+ e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}

		// Rollback teardown and/or setup transaction
		if (tearDownContext != null && tearDownContext.isInTransaction()) {
			if (testSuite.getAutoRollbackMFs()) {
				LOG.trace("Rollback transaction for setup and/or teardown");
				tearDownContext.rollbackTransaction();
			} else {
				LOG.trace("End transaction for for setup and/or teardown");
				tearDownContext.endTransaction();
			}
		}

		// Make sure we clean setupContext after running this test/suite
		setupContext = null;
	}

	public synchronized void runTestSuites() throws CoreException {
		if (!ConfigurationManager.verifyModuleIsEnabled()) return;

		LOG.info("Starting testrun on all suites");

		// Context without transaction!
		IContext context = Core.createSystemContext();

		List<IMendixObject> testSuites = Core.createXPathQuery("//" + TestSuite.entityName).execute(context);

		for (IMendixObject suite : testSuites) {
			suite.setValue(context, TestSuite.MemberNames.Result.toString(), null);
		}
		Core.commit(context, testSuites);

		for (IMendixObject suite : testSuites) {
			runTestSuite(context, TestSuite.load(context, suite.getId()));
		}

		LOG.info("Finished testrun on all suites");
	}

	public synchronized void runTestSuite(IContext context, TestSuite testSuite) throws CoreException {
		if (!ConfigurationManager.verifyModuleIsEnabled()) return;

		LOG.info("Starting testrun on " + testSuite.getModule());

		/**
		 * Reset state
		 */
		testSuite.setLastRun(new Date());
		testSuite.setLastRunTime(0L);
		testSuite.setTestPassedCount(0L);
		testSuite.setTestFailedCount(0L);
		testSuite.setResult(ENUM_UnitTestResult._1_Running);
		testSuite.commit();

		StringBuilder query = new StringBuilder();
		query.append(String.format("//%s", UnitTest.entityName));
		query.append(String.format("[%s=$TestSuite]", UnitTest.MemberNames.UnitTest_TestSuite));

		List<IMendixObject> unitTests = Core.createXPathQuery(query.toString())
				.setVariable("TestSuite", testSuite.getMendixObject().getId().toLong()).execute(context);

		for (IMendixObject mxObject : unitTests) {
			UnitTest test = UnitTest.initialize(context, mxObject);
			test.setResult(null);
			test.commit();
		}

		long start = System.currentTimeMillis();

		/**
		 * Run java unit tests
		 */
		if (unittesting.proxies.constants.Constants.getFindJUnitTests()) {
			Class<?>[] classes = null;

			try {
				classes = JavaTestDiscovery.getUnitTestClasses(testSuite);
			} catch (Exception e) {
				LOG.error("Unable to find JUnit test classes or methods: " + e.getMessage(), e);
			}

			if (classes != null && classes.length > 0) {
				JUnitCore junit = new JUnitCore();
				junit.addListener(new UnitTestRunListener(context, testSuite));
				junit.run(classes);
			}
		}

		/**
		 * Run microflow tests
		 */
		try {
			runMfSetup(testSuite);

			List<String> mfnames = findMicroflowUnitTests(testSuite);

			for (String mf : mfnames) {
				if (runMicroflowTest(mf, getUnitTest(context, testSuite, mf, true), testSuite)) {
					testSuite.setTestPassedCount(testSuite.getTestPassedCount() + 1);
				} else {
					testSuite.setTestFailedCount(testSuite.getTestFailedCount() + 1);
				}
				testSuite.commit();
			}

		} finally {
			runMfTearDown(testSuite);
		}

		/**
		 * Aggregate
		 */
		testSuite.setLastRunTime((System.currentTimeMillis() - start) / 1000);
		testSuite.setResult(testSuite.getTestFailedCount() == 0L ? ENUM_UnitTestResult._3_Success
				: ENUM_UnitTestResult._2_Failed);
		testSuite.commit();

		LOG.info("Finished testrun on " + testSuite.getModule());
	}

	public List<String> findMicroflowUnitTests(TestSuite testRun) {
		List<String> microflowNames = new ArrayList<>();

		if (testRun.getPrefix1() == null) {
			testRun.setPrefix1("Test_");
		}
		if (testRun.getPrefix2() == null) {
			testRun.setPrefix2("UT_");
		}

		String basename1 = (testRun.getModule() + "." + testRun.getPrefix1()).toLowerCase();
		String basename2 = (testRun.getModule() + "." + testRun.getPrefix2()).toLowerCase();

		// Find microflow names
		for (String mf : Core.getMicroflowNames())
			if (mf.toLowerCase().startsWith(basename1) || mf.toLowerCase().startsWith(basename2))
				microflowNames.add(mf);

		// Sort microflow names
		Collections.sort(microflowNames);
		return microflowNames;
	}

	public boolean hasMfSetup(TestSuite testSuite) {
		return Core.getMicroflowNames().contains(testSuite.getModule() + ".Setup");
	}

	public boolean hasMfTearDown(TestSuite testSuite) {
		return Core.getMicroflowNames().contains(testSuite.getModule() + ".TearDown");
	}

	private boolean hasTestContextInputParameter(String mf) {
		if (!Core.getInputParameters(mf).containsKey(TEST_CONTEXT_PARAM_NAME))
			return false;

		IDataType dataType = Core.getInputParameters(mf).get(TEST_CONTEXT_PARAM_NAME);
		return dataType.isMendixObject()
				&& unittesting.proxies.UnitTestContext.getType().equals(dataType.getObjectType());
	}

	public boolean validateTestMicroflow(String mf) {
		if (Core.getReturnType(mf).getType() != IDataType.DataTypeEnum.Boolean
				&& Core.getReturnType(mf).getType() != IDataType.DataTypeEnum.String
				&& Core.getReturnType(mf).getType() != IDataType.DataTypeEnum.Nothing) {

			LOG.warn("Invalid test microflow " + mf
					+ ": Microflow should return either a boolean or a string or nothing at all");
			return false;
		}

		if (Core.getInputParameters(mf).size() == 1 && hasTestContextInputParameter(mf)) {
			LOG.trace("Identified parameter for unit test context in test microflow " + mf);
		} else if (!Core.getInputParameters(mf).isEmpty()) {
			LOG.warn("Invalid test microflow " + mf + ": Identified one or more invalid parameter(s)");
			return false;
		}

		return true;
	}

	private MicroflowCallBuilder buildMicroflowCall(String mf, UnitTestContext unitTestContext) {
		MicroflowCallBuilder builder = Core.microflowCall(mf);

		if (hasTestContextInputParameter(mf))
			builder = builder.withParam(TEST_CONTEXT_PARAM_NAME, unitTestContext.getMendixObject());

		return builder;
	}

	private boolean runMicroflowTest(String mf, UnitTest test) throws CoreException {
		/**
		 * Prepare...
		 */
		TestSuite testSuite = test.getUnitTest_TestSuite();

		return runMicroflowTest(mf, test, testSuite);
	}

	private boolean runMicroflowTest(String mf, UnitTest test, TestSuite testSuite) {
		/**
		 * Prepare...
		 */
		LOG.info("Starting unit test for microflow " + mf);

		test.setName(mf);
		test.setResultMessage("");
		test.setLastRun(new Date());

		IContext mfContext = getMicroflowTestContext(testSuite);

		mfContext.startTransaction();
		LOG.trace("Start transaction for unit test");

		executionContext = new TestExecutionContext();
		executionContext().clearTestActivities(test);

		long duration = 0L;
		long startTimestamp = System.currentTimeMillis();

		try {
			if (!validateTestMicroflow(mf)) {
				test.setResult(ENUM_UnitTestResult._2_Failed);
				commitSilent(test);

				executionContext().collectStart(false, "Unable to start test, invalid microflow");
				return false;
			}

			executionContext().collectStart(true, null);
			test.setResult(ENUM_UnitTestResult._1_Running);
			commitSilent(test);

			UnitTestContext unitTestContext = UnitTestContextManager.createUnitTestContext(mfContext, mf);
			executionContext().setUnitTestContext(unitTestContext);

			Object mfReturnValue = buildMicroflowCall(mf, unitTestContext).execute(mfContext);
			duration = System.currentTimeMillis() - startTimestamp;

			boolean returnValueResult = mfReturnValue == null || Boolean.TRUE.equals(mfReturnValue) || "".equals(mfReturnValue);

			if (returnValueResult) {
				executionContext().collectEnd(true, null);
			} else if (mfReturnValue instanceof String) {
				executionContext().collectEnd(false, "Microflow returned string: " + mfReturnValue);
			} else if (mfReturnValue instanceof Boolean) {
				executionContext().collectEnd(false, "Microflow returned false");
			}

			boolean testResult = returnValueResult && !executionContext().hasFailedAssertion();
			test.setResult(testResult ? ENUM_UnitTestResult._3_Success : ENUM_UnitTestResult._2_Failed);

			if (mfContext.isInTransaction()) {
				if (testSuite.getAutoRollbackMFs()) {
					LOG.trace("Rollback transaction for unit test");
					mfContext.rollbackTransaction();
				} else {
					LOG.trace("End transaction for unit test");
					mfContext.endTransaction();
				}
			}

			return testResult;
		} catch (Exception e) {
			duration = System.currentTimeMillis() - startTimestamp;
			test.setResult(ENUM_UnitTestResult._2_Failed);
			Throwable cause = ExceptionUtils.getRootCause(e);

			if (!(cause instanceof AssertionException)) {
				test.setStackTrace(ExceptionUtils.getStackTrace(e));
				executionContext().collectException(e);
			}

			return false;
		} finally {
			executionContext().persistTestActivities(test);

			test.setResultMessage(executionContext().getResultSummary());
			test.setReadableTime(formatAsReadableTime(duration));
			if (executionContext().getLastStep() != null)
				test.setLastStep(executionContext().getLastStep().getMessage());

			commitSilent(test);

			LOG.info("Finished unit test " + mf + ": " + test.getResult());
		}
	}

	private IContext getMicroflowTestContext(TestSuite testSuite) {
		if (Core.getMicroflowNames().contains(testSuite.getModule() + ".Setup")) {
			return setupContext.createClone();
		} else {
			return Core.createSystemContext();
		}
	}

	public static String formatAsReadableTime(long ms) {
		return (ms > 10000 ? Math.round(ms / 1000) + " seconds" : 
			(ms == 0 ?  "<1" : ms) + " milliseconds");
	}

	private long getTestSuiteCount(IContext context, TestSuite testSuite, String constraint) {
		StringBuilder query = new StringBuilder();
		query.append(String.format("//%s", UnitTest.entityName));
		query.append(String.format("[%s=%d]", UnitTest.MemberNames.UnitTest_TestSuite,
				testSuite.getMendixObject().getId().toLong()));
		if (constraint != null) query.append(constraint);

		return Core.createXPathQuery(String.format("COUNT(%s)", query)).executeAggregateLong(context);
	}

	public void updateTestSuiteCountersAndResult(IContext context, TestSuite testSuite, boolean commit)
			throws CoreException {
		long testCount = getTestSuiteCount(context, testSuite, null);
		long succeededCount = getTestSuiteCount(context, testSuite, "[Result = '_3_Success']");
		long failedCount = getTestSuiteCount(context, testSuite, "[Result = '_2_Failed']");
		long pendingCount = getTestSuiteCount(context, testSuite, "[Result = '_1_Running' or Result = empty]");

		testSuite.setTestCount(testCount);
		LOG.trace("Updated test count to " + succeededCount);

		testSuite.setTestPassedCount(succeededCount);
		LOG.trace("Updated test suite succeeded count to " + succeededCount);

		testSuite.setTestFailedCount(failedCount);
		LOG.trace("Updated test suite failed count to " + failedCount);

		if (failedCount > 0) {
			testSuite.setResult(ENUM_UnitTestResult._2_Failed);
			LOG.trace("Updated test suite result to 'Failed'");
		} else if (pendingCount > 0 || testCount == 0) {
			testSuite.setResult(null);
			LOG.trace("Updated test suite result to empty");
		} else {
			testSuite.setResult(ENUM_UnitTestResult._3_Success);
			LOG.trace("Updated test suite result to 'Success'");
		}

		if (commit) testSuite.commit();
	}

	private void commitSilent(UnitTest test) {
		try {
			test.commit();
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	UnitTest getJUnitTest(IContext context, TestSuite testSuite, Description description) {
		return getUnitTest(context, testSuite, description.getClassName() + "/" + description.getMethodName(), false);
	}

	private UnitTest getUnitTest(IContext context, TestSuite testSuite, String name, boolean isMF) {
		String displayName = name.substring(testSuite.getModule().length() + 1);

		StringBuilder query = new StringBuilder();
		query.append(String.format("//%s", UnitTest.entityName));
		query.append(String.format("[%s=$TestSuite]", UnitTest.MemberNames.UnitTest_TestSuite));
		query.append(String.format("[%s=$Name]", UnitTest.MemberNames.Name));
		query.append(String.format("[%s=$DisplayName]", UnitTest.MemberNames.DisplayName));
		query.append(String.format("[%s=$IsMicroflow]", UnitTest.MemberNames.IsMf));

		Optional<IMendixObject> mxObject = Core.createXPathQuery(query.toString())
				.setVariable("TestSuite", testSuite.getMendixObject().getId())
				.setVariable("Name", name)
				.setVariable("DisplayName", displayName)
				.setVariable("IsMicroflow", isMF)
				.execute(context).stream().findAny();

		if (mxObject.isPresent()) {
			return UnitTest.initialize(context, mxObject.get());
		} else {
			UnitTest newTest = new UnitTest(context);
			newTest.setName(name);
			newTest.setDisplayName(displayName);
			newTest.setUnitTest_TestSuite(testSuite);
			newTest.setIsMf(isMF);

			return newTest;
		}
	}

	public void reportStep(String message) {
		LOG.debug("Report step: " + message);
		lastStep = message;
	}

	public synchronized void findAllTests(IContext context) throws CoreException {
		if (!ConfigurationManager.verifyModuleIsEnabled()) return;

		/*
		 * Find modules
		 */
		Set<String> modules = new HashSet<String>();
		for (String name : Core.getMicroflowNames())
			modules.add(name.split("\\.")[0]);

		/*
		 * Update modules
		 */
		for (String module : modules) {
			TestSuite testSuite = findOrCreateTestSuite(context, module);
			updateUnitTestList(context, testSuite);
		}

		/*
		 * Remove all modules without tests
		 */
		deleteTestSuitesWithoutTest(context);

		/*
		 * Reset refresh required flag
		 */
		ModelUpdateSubscriber.getInstance().setRefreshRequired(false);
	}

	public synchronized Optional<TestSuite> findTestSuite(IContext context, String module) {
		StringBuilder query = new StringBuilder();
		query.append(String.format("//%s", TestSuite.entityName));
		query.append(String.format("[%s=$Module]", TestSuite.MemberNames.Module));

		Optional<IMendixObject> mxObject = Core.createXPathQuery(query.toString()).setVariable("Module", module)
				.execute(context).stream().findAny();

        return mxObject.map(obj -> TestSuite.initialize(context, obj));
	}

	private synchronized TestSuite findOrCreateTestSuite(IContext context, String module) throws CoreException {
		Optional<TestSuite> testSuite = findTestSuite(context, module);

		if (testSuite.isPresent()) {
			return testSuite.get();
		} else {
			TestSuite newSuite = new TestSuite(context);
			newSuite.setModule(module);
			newSuite.commit();

			return newSuite;
		}
	}

	private synchronized void deleteTestSuitesWithoutTest(IContext context) throws CoreException {
		StringBuilder query = new StringBuilder();
		query.append(String.format("//%s", TestSuite.entityName));
		query.append("[not(" + UnitTest.MemberNames.UnitTest_TestSuite + "/" + UnitTest.entityName + ")]");

		List<IMendixObject> testSuites = Core.createXPathQuery(query.toString()).execute(context);
		Core.delete(context, testSuites);
	}

	public synchronized void updateUnitTestList(IContext context, TestSuite testSuite) {
		try {
			/*
			 * Mark all dirty
			 */
			StringBuilder query = new StringBuilder();
			query.append(String.format("//%s", UnitTest.entityName));
			query.append(String.format("[%s=$TestSuite]", UnitTest.MemberNames.UnitTest_TestSuite));

			List<IMendixObject> unitTests = Core.createXPathQuery(query.toString())
					.setVariable("TestSuite", testSuite.getMendixObject().getId().toLong()).execute(context);

			for (IMendixObject mxObject : unitTests) {
				UnitTest test = UnitTest.initialize(context, mxObject);
				test.set_dirty(true);
				test.commit();
			}

			/*
			 * Find microflow tests
			 */
			for (String mf : findMicroflowUnitTests(testSuite)) {
				UnitTest test = getUnitTest(context, testSuite, mf, true);
				test.set_dirty(false);
				test.setUnitTest_TestSuite(testSuite);
				test.commit();
			}

			if (unittesting.proxies.constants.Constants.getFindJUnitTests()) {
				/*
				 * Find Junit tests
				 */
				for (String jtest : JavaTestDiscovery.findJUnitTests(testSuite)) {
					UnitTest test = getUnitTest(context, testSuite, jtest, false);
					test.set_dirty(false);
					test.setUnitTest_TestSuite(testSuite);
					test.commit();
				}
			}

			/*
			 * Delete dirty tests
			 */
			StringBuilder deleteQuery = new StringBuilder();
			deleteQuery.append(String.format("//%s", UnitTest.entityName));
			deleteQuery.append(String.format("[%s=true]", UnitTest.MemberNames._dirty));

			List<IMendixObject> dirtyTests = Core.createXPathQuery(deleteQuery.toString()).execute(context);
			Core.delete(context, dirtyTests);

			/*
			 * Update counters + result
			 */
			updateTestSuiteCountersAndResult(context, testSuite, false);

			/*
			 * Update setup/teardown
			 */
			testSuite.setHasSetup(hasMfSetup(testSuite));
			testSuite.setHasTearDown(hasMfTearDown(testSuite));
			testSuite.commit();

		} catch (Exception e) {
			LOG.error("Failed to update unit test list: " + e.getMessage(), e);
		}
	}

	public String getLastReportedStep() {
		// MWE: this system is problematic weird if used from multiple simultanously
		// used threads..
		return lastStep;
	}
}
