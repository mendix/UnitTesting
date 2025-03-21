package unittesting;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import org.junit.Test;
import org.junit.runners.JUnit4;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import unittesting.proxies.TestSuite;

public class JavaTestDiscovery {
    private static final Map<String, Class<?>[]> classCache = new HashMap<>();
    private static final ILogNode LOG = ConfigurationManager.LOG;

    public static List<String> findJUnitTests(TestSuite testSuite) {
        List<String> junitTests = new ArrayList<>();

        try {
            Class<?>[] classes = getUnitTestClasses(testSuite);

            if (classes != null && classes.length > 0) {
                for (Class<?> clazz : classes) {

                    // From
                    // https://github.com/KentBeck/junit/blob/master/src/main/java/org/junit/runners/BlockJUnit4ClassRunner.java
                    // method computeTestMethods
                    try {
                        List<FrameworkMethod> methods = new JUnit4(clazz).getTestClass()
                                .getAnnotatedMethods(Test.class);

                        if (methods != null && !methods.isEmpty())
                            for (FrameworkMethod method : methods)
                                junitTests.add(clazz.getName() + "/" + method.getName());
                    } catch (InitializationError e2) {
                        StringBuilder errors = new StringBuilder();

                        for (Throwable cause : e2.getCauses())
                            errors.append("\n").append(cause.getMessage());

                        LOG.error("Failed to recognize class '" + clazz + "' as unitTestClass: " + errors.toString());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Unable to find JUnit test classes or methods: " + e.getMessage(), e);
        }

        return junitTests;
    }

    public static Class<?>[] getUnitTestClasses(TestSuite testRun) throws IOException {
        if (!classCache.containsKey(testRun.getModule().toLowerCase())) {
            ArrayList<Class<?>> classList = getClassesForPackage(testRun.getModule());
            Class<?>[] classes = classList.toArray(new Class<?>[classList.size()]);
            classCache.put(testRun.getModule().toLowerCase(), classes);
        }

        return classCache.get(testRun.getModule().toLowerCase());
    }

    private static ArrayList<Class<?>> getClassesForPackage(String path) throws IOException {
        ArrayList<Class<?>> classes = new ArrayList<>();

        // Lowercased Mendix module names equals their package names
        String packageName = path.toLowerCase();

        // Get a File object containing the classes. This file is expected to be
        // located at [deploymentdir]/model/bundles/project.jar
        File projectJar = new File(Core.getConfiguration().getBasePath() + File.separator + "model"
                + File.separator + "bundles" + File.separator + "project.jar");

        processProjectJar(projectJar, packageName, classes);

        return classes;
    }

    /**
     * Find runnable classes
     * https://github.com/ddopson/java-class-enumerator/blob/master/src/pro/ddopson/ClassEnumerator.java
     */

    private static Class<?> loadClass(String className) {
        try {
            return TestManager.instance().getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unexpected ClassNotFoundException loading class '" + className + "'");
        }
    }

    private static void processProjectJar(File projectJar, String packageName, ArrayList<Class<?>> classes)
            throws IOException {
        ZipFile zipFile = new ZipFile(projectJar);

        // Get the list of the files contained in the package
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            String fileName = zipEntry.getName();

            String className = null;

            if (fileName.startsWith(packageName.concat("/")) && fileName.endsWith(".class")) {
                fileName = fileName.replace("/", ".");

                // Remove the .class extension
                className = fileName.substring(0, fileName.length() - 6);
            }

            if (className != null) {
                Class<?> clazz = loadClass(className);
                if (isProperUnitTest(clazz))
                    classes.add(clazz);
            }
        }

        zipFile.close();
    }

    private static boolean isProperUnitTest(Class<?> clazz) {
        for (Method m : clazz.getMethods())
            if (m.getAnnotation(org.junit.Test.class) != null)
                return true;

        return false;
    }
}
