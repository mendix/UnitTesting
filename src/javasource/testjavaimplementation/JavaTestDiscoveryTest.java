package testjavaimplementation;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import org.junit.Before;
import org.junit.Test;
import suitewithjavatests.ClassWithUnitTests;
import suitewithjavatests.ClassWithoutUnitTests;
import suitewithjavatests.tests.ClassInSubpackageWithUnitTests;
import unittesting.JavaTestDiscovery;
import unittesting.TestManager;
import unittesting.proxies.TestSuite;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaTestDiscoveryTest {
    private final IContext context = Core.createSystemContext();
    private final TestManager testManager = TestManager.instance();

    private TestSuite testSuite;
    private List<Class<?>> testClassList;

    @Before
    public void setup() throws IOException {
        testSuite = testManager.findTestSuite(context, "SuiteWithJavaTests").get();
        testClassList = Arrays.asList(JavaTestDiscovery.getUnitTestClasses(testSuite));
    }

    @Test
    public void getUnitTestClassesShouldFindClassWithUnitTests() {
        assertTrue(testClassList.contains(ClassWithUnitTests.class));
    }

    @Test
    public void getUnitTestClassesShouldFindTestClassInSubpackage() {
        assertTrue(testClassList.contains(ClassInSubpackageWithUnitTests.class));
    }

    @Test
    public void getUnitTestClassesShouldNotFindClassWithoutUnitTests() {
        assertFalse(testClassList.contains(ClassWithoutUnitTests.class));
    }

    @Test
    public void findJUnitTestsShouldFindTestMethod() {
        assertTrue(JavaTestDiscovery.findJUnitTests(testSuite).contains("suitewithjavatests.ClassWithUnitTests/testAssertTrue"));
    }

    @Test
    public void findJUnitTestsShouldNotFindNonTestMethod() {
        assertFalse(JavaTestDiscovery.findJUnitTests(testSuite).contains("suitewithjavatests.ClassWithUnitTests/nonTestMethod"));
    }
}
