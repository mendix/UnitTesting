package myfirstmodule.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FailingUnitTest {
	
	@Test
	public void testAssertTrueShouldFail() throws InterruptedException {
		assertTrue(false);
	}
}
