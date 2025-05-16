package suitewithjavatests;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ClassWithUnitTests {
    @Test
    public void testAssertTrue() {
        assertTrue(true);
    }

    public void nonTestMethod() {
        // Do nothing
    }
}
