package unittesting;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runners.JUnit4;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import unittesting.proxies.TestSuite;
import unittesting.proxies.UnitTest;
import unittesting.proxies.UnitTestResult;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IDataType;
import communitycommons.XPath;

/**
 * @author mwe
 *
 */
public class TestManager
{
	/** Test manager introduces its own exception, because the AssertionExceptions from JUnit are not picked up properly by
	 * the runtime in 4.1.1 and escape all exception handling defined inside microflows :-S
	 * @author mwe
	 *
	 */
	public static class AssertionException extends Exception
	{
		private static final long	serialVersionUID	= -3115796226784699883L;

		public AssertionException(String message)
		{
			super(message);
		}
	}


	private static final String	TEST_MICROFLOW_PREFIX_1	= "Test";
	private static final String	TEST_MICROFLOW_PREFIX_2	= "UT_";

	static final String	CLOUD_SECURITY_ERROR	= "Failed to find JUnit test classes or methods. Note that java unit tests cannot be run with default cloud security. \n\n";
	
	private static TestManager	instance;
	public static ILogNode LOG = Core.getLogger("UnitTestRunner");
	
	private static final Map<String, Object> emptyArguments = new HashMap<String, Object>();
	private static final Map<String, Class<?>[]> classCache = new HashMap<String, Class<?>[]>();

	private String	lastStep;
	
	

	public static TestManager instance()
	{
		if (instance == null)
			instance = new TestManager();
		return instance;
	}


	private static Class<?>[] getUnitTestClasses(TestSuite testRun) {
		if (!classCache.containsKey(testRun.getModule().toLowerCase())) {

			ArrayList<Class<?>> classlist = getClassesForPackage(testRun.getModule());
			Class<?>[] clazzez =  classlist.toArray(new Class<?>[classlist.size()]);
			classCache.put(testRun.getModule().toLowerCase(), clazzez);
		}

		return classCache.get(testRun.getModule().toLowerCase());
	}


	public synchronized void runTest(IContext context, UnitTest unitTest) throws ClassNotFoundException, CoreException
	{
		TestSuite testSuite = unitTest.getUnitTest_TestSuite();
		
		/**
		 * Is Mf
		 */
		if (unitTest.getIsMf()) {
			try {
				runMfSetup(testSuite);
				runMicroflowTest(unitTest.getName(), unitTest);
			}
			finally {
				runMfTearDown(testSuite);
			}
		}

		/**
		 * Is java
		 */
		else {

			Class<?> test = Class.forName(unitTest.getName().split("/")[0]);

			JUnitCore junit = new JUnitCore();
			junit.addListener(new UnitTestRunListener(context, testSuite));

			junit.run(test);
		}
	}

	private void runMfSetup(TestSuite testSuite)  
	{
		if (Core.getMicroflowNames().contains(testSuite.getModule() + ".Setup")) {
			try {
				LOG.info("Running Setup microflow..");
				Core.execute(Core.createSystemContext(), testSuite.getModule() + ".Setup", emptyArguments);
			}
			catch(Exception e) {
				LOG.error("Exception during SetUp microflow: " + e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
	}

	private void runMfTearDown(TestSuite testSuite) 
	{
		if (Core.getMicroflowNames().contains(testSuite.getModule() + ".TearDown")) {
			try
			{
				LOG.info("Running TearDown microflow..");
				Core.execute(Core.createSystemContext(), testSuite.getModule() + ".TearDown", emptyArguments);
			}
			catch (Exception e)
			{
				LOG.error("Severe: exception in unittest TearDown microflow '" + testSuite.getModule() + ".Setup': " +e.getMessage(), e);
				throw new RuntimeException(e);
			}
		}
	}
	
	public synchronized void runTestSuites() throws CoreException {
		LOG.info("Starting testrun on all suites");
		
		//Context without transaction!
		IContext context = Core.createSystemContext();
		
		for(TestSuite suite : XPath.create(context, TestSuite.class).all()) {
			suite.setResult(null);
			suite.commit();
		}

		for(TestSuite suite : XPath.create(context, TestSuite.class).all()) {
			runTestSuite(context, suite);
		}

		LOG.info("Finished testrun on all suites");
	}

	public synchronized boolean runTestSuite(IContext context, TestSuite testSuite) throws CoreException
	{
		LOG.info("Starting testrun on " + testSuite.getModule());

		/**
		 * Reset state
		 */
		testSuite.setLastRun(new Date());
		testSuite.setLastRunTime(0L);
		testSuite.setTestFailedCount(0L);
		testSuite.setResult(UnitTestResult._1_Running);
		testSuite.commit();
		
		for(UnitTest test : XPath.create(context, UnitTest.class).eq(UnitTest.MemberNames.UnitTest_TestSuite, testSuite).all()) {
			test.setResult(null);
			test.commit();
		}
		
		long start = System.currentTimeMillis();

		/**
		 * Run java unit tests
		 */
		Class<?>[] clazzez = null;
		try {
			clazzez = getUnitTestClasses(testSuite);
		}
		catch(Exception e) {
			LOG.error(CLOUD_SECURITY_ERROR + e.getMessage(), e);
		}

		if (clazzez != null && clazzez.length > 0) {
			JUnitCore junit = new JUnitCore();
			junit.addListener(new UnitTestRunListener(context, testSuite));

			junit.run(clazzez);
		}

		/** 
		 * Run microflow tests
		 * 
		 */

		try {
			runMfSetup(testSuite);
	
			List<String> mfnames = findMicroflowUnitTests(testSuite);
	
			for (String mf : mfnames){
				if (!runMicroflowTest(mf, getUnitTest(context, testSuite, mf, true))) {
					testSuite.setTestFailedCount(testSuite.getTestFailedCount() + 1);
					testSuite.commit();
				}
			}
	
		}
		finally {
			runMfTearDown(testSuite);
		}
		

		/** 
		 * Aggregate
		 */
		testSuite.setLastRunTime((System.currentTimeMillis() - start) / 1000);
		testSuite.setResult(testSuite.getTestFailedCount() == 0L ? UnitTestResult._3_Success : UnitTestResult._2_Failed);
		testSuite.commit();

		LOG.info("Finished testrun on " + testSuite.getModule());
		return true;
	}


	public List<String> findMicroflowUnitTests(TestSuite testRun)
	{
		List<String> mfnames = new ArrayList<String>();

		String basename1 = (testRun.getModule() + "." + TEST_MICROFLOW_PREFIX_1).toLowerCase();
		String basename2 = (testRun.getModule() + "." + TEST_MICROFLOW_PREFIX_2).toLowerCase();
		
		//Find microflownames
		for (String mf : Core.getMicroflowNames()) 
			if (mf.toLowerCase().startsWith(basename1) || mf.toLowerCase().startsWith(basename2)) 
				mfnames.add(mf);

		//Sort microflow names
		Collections.sort(mfnames);
		return mfnames;
	}


	private boolean runMicroflowTest(String mf, UnitTest test) throws CoreException
	{
		/** 
		 * Prepare...
		 */
		LOG.info("Starting unittest " + mf);
		TestSuite testSuite = test.getUnitTest_TestSuite();

		reportStep("Starting microflow test '" + mf + "'");
		
		test.setResult(UnitTestResult._1_Running);
		test.setName(mf);
		test.setResultMessage("");
		test.setLastRun(new Date());

		if (Core.getInputParameters(mf).size() != 0) {
			test.setResultMessage("Unable to start test '" +  mf + "', microflow has parameters");
			test.setResult(UnitTestResult._2_Failed);
		}
		else if (Core.getReturnType(mf).getType() != IDataType.DataTypeEnum.Boolean && 
						 Core.getReturnType(mf).getType() != IDataType.DataTypeEnum.String &&
						 Core.getReturnType(mf).getType() != IDataType.DataTypeEnum.Nothing) {
			
			test.setResultMessage("Unable to start test '" +  mf + "', microflow should return either a boolean or a string or nothing at all");
			
			test.setResult(UnitTestResult._2_Failed);
		}

		commitSilent(test);

		IContext mfContext = Core.createSystemContext();
		if (testSuite.getAutoRollbackMFs())
			mfContext.startTransaction();

		long start = System.currentTimeMillis();

		try {
			Object resultObject = Core.execute(mfContext, mf, emptyArguments);
			
			start = System.currentTimeMillis() - start;
			boolean res = 	resultObject == null || Boolean.TRUE.equals(resultObject) || "".equals(resultObject);
				
			test.setResult(res ? UnitTestResult._3_Success : UnitTestResult._2_Failed);
			test.setResultMessage((res ? "OK" : "FAILED") +  " in " + (start > 10000 ? Math.round(start / 1000) + " s." : start + " ms. Result: " + String.valueOf(resultObject)));
			return res;
		}
		catch(Exception e) {
			test.setResult(UnitTestResult._2_Failed);
			Throwable cause = ExceptionUtils.getRootCause(e);
			if (cause != null && cause instanceof AssertionException)
				test.setResultMessage(cause.getMessage());
			else
				test.setResultMessage("Exception: " + e.getMessage() + "\n\n" + ExceptionUtils.getFullStackTrace(e));
			return false;
			
		}
		finally {
			if (testSuite.getAutoRollbackMFs())
				mfContext.rollbackTransAction();
				
			test.setLastStep(lastStep);
			commitSilent(test);

			LOG.info("Finished unittest " + mf + ": " + test.getResult());
		}
	}


	private void commitSilent(UnitTest test)
	{
		try
		{
			test.commit();
		}
		catch (CoreException e)
		{
			throw new RuntimeException(e);
		}
	}

	UnitTest getUnitTest(IContext context, TestSuite testSuite, Description description, boolean isMF) {
		return getUnitTest(context, testSuite, description.getClassName() + "/" + description.getMethodName(), isMF);
	}

	private UnitTest getUnitTest(IContext context, TestSuite testSuite, String name, boolean isMF) {
		UnitTest res;
		try
		{
			res = XPath.create(context, UnitTest.class)
					.eq(UnitTest.MemberNames.UnitTest_TestSuite, testSuite)
					.and()
					.eq(UnitTest.MemberNames.Name, name)
					.and()
					.eq(UnitTest.MemberNames.IsMf, isMF)
					.first();
		}
		catch (CoreException e)
		{
			throw new RuntimeException(e);
		}

		if (res == null) {
			res = new UnitTest(context);
			res.setName(name);
			res.setUnitTest_TestSuite(testSuite);
			res.setIsMf(isMF);
		}

		return res;
	}


	/**
	 * 
	 * 
	 * Find runabble classes
	 * 
	 * https://github.com/ddopson/java-class-enumerator/blob/master/src/pro/ddopson/ClassEnumerator.java
	 * 
	 */

	private static Class<?> loadClass(String className) {
		try {
			return TestManager.instance().getClass().getClassLoader().loadClass(className);
		} 
		catch (ClassNotFoundException e) {
			throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
		}
	}

	private static void processDirectory(File directory, String pkgname, ArrayList<Class<?>> classes) {
		// Get the list of the files contained in the package
		String[] files = directory.list();
		if (files != null) 
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				String className = null;
				// we are only interested in .class files
				if (fileName.endsWith(".class")) {
					// removes the .class extension
					className = pkgname + '.' + fileName.substring(0, fileName.length() - 6);
				}
				if (className != null) {
					Class<?> clazz = loadClass(className);
					if (isProperUnitTest(clazz))
						classes.add(clazz);
				}
				File subdir = new File(directory, fileName);
				if (subdir.isDirectory()) {
					processDirectory(subdir, pkgname + '.' + fileName, classes);
				}
			}
	}

	private static boolean isProperUnitTest(Class<?> clazz)
	{
		for (Method m : clazz.getMethods()) 
			if (m.getAnnotation(org.junit.Test.class) != null)
				return true;

		return false;

	}


	public static ArrayList<Class<?>> getClassesForPackage(String path /*Package pkg*/) {
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

		//String pkgname = pkg.getName();
		//String relPath = pkgname.replace('.', '/');

		//Lowercased Mendix Module names equals their package names
		String pkgname = path.toLowerCase();

		// Get a File object for the package
		File basedir = new File(Core.getConfiguration().getBasePath() + File.separator + "run" +  File.separator + "bin" + File.separator + pkgname);
		processDirectory(basedir, pkgname, classes);

		return classes;
	}


	public void reportStep(String lastStep1)
	{
		lastStep = lastStep1;
		LOG.debug("UnitTest reportStep: '" + lastStep1 + "'");
	}

	public synchronized void findAllTests(IContext context) throws CoreException {
		/*
		 * Find modules
		 */
		Set<String> modules = new HashSet<String>();
		for(String name : Core.getMicroflowNames())
			modules.add(name.split("\\.")[0]);
		
		/*
		 * Update modules
		 */
		for(String module : modules) {
			TestSuite testSuite = XPath.create(context, TestSuite.class).findOrCreate(TestSuite.MemberNames.Module, module);
			updateUnitTestList(context, testSuite);
		}
		
		/*
		 * Remove all modules without tests
		 */
		XPath.create(context, TestSuite.class).not().hasReference(UnitTest.MemberNames.UnitTest_TestSuite, UnitTest.entityName).close().deleteAll();
	}

	public synchronized void updateUnitTestList(IContext context, TestSuite testSuite)
	{
		try {
			/*
			 * Mark all dirty
			 */
			for(UnitTest test : XPath.create(context, UnitTest.class)
					.eq(UnitTest.MemberNames.UnitTest_TestSuite, testSuite)
					.all()) {
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
			
			/*
			 * Find Junit tests
			 */
			for (String jtest : findJUnitTests(testSuite)) { 
				UnitTest test = getUnitTest(context, testSuite, jtest, false);
				test.set_dirty(false);
				test.setUnitTest_TestSuite(testSuite);
				test.commit();
			}
			
			/*
			 * Delete dirty tests
			 */
			for(UnitTest test : XPath.create(context, UnitTest.class)
					.eq(UnitTest.MemberNames._dirty, true)
					.all()) {
				test.delete();
			}
			
			/*
			 * Update count
			 */
			testSuite.setTestCount(XPath.create(context, UnitTest.class).eq(UnitTest.MemberNames.UnitTest_TestSuite, testSuite).count());
			testSuite.commit();
			
		}
		catch(Exception e) {
			LOG.error("Failed to update unit test list: " + e.getMessage(), e);
		}
		
	}


	public List<String> findJUnitTests(TestSuite testSuite)
	{
		List<String> junitTests = new ArrayList<String>(); 
		try {
			Class<?>[] clazzez = getUnitTestClasses(testSuite);

			if (clazzez != null && clazzez.length > 0) { 
				for (Class<?> clazz : clazzez) {
					
					//From https://github.com/KentBeck/junit/blob/master/src/main/java/org/junit/runners/BlockJUnit4ClassRunner.java method computeTestMethods
					try {
						List<FrameworkMethod> methods =	new JUnit4(clazz).getTestClass().getAnnotatedMethods(Test.class);
						
						if (methods != null && !methods.isEmpty()) 
							for (FrameworkMethod method: methods)
								junitTests.add(clazz.getName() + "/" + method.getName());
					}
					catch(InitializationError e2) {
						StringBuilder errors = new StringBuilder();
						
						for(Throwable cause : e2.getCauses())
							errors.append("\n").append(cause.getMessage());
						
						LOG.error("Failed to recognize class '" + clazz + "' as unitTestClass: " + errors.toString());
					}
				}
			}
		}
		catch(Exception e) {
			LOG.error(CLOUD_SECURITY_ERROR + e.getMessage(), e);
		}
		return junitTests;
	}


	public String getLastReportedStep() {
		//MWE: this system is problematic weird if used from multiple simultanously used threads..
		return lastStep;
	}


	
}