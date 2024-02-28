package unittesting;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class UnittestingUnitTest1
{
	private boolean	state;

	@Before
	public void setup() throws InterruptedException {
		this.state = false;
		Thread.sleep(10);
	}
	
	@Test
	public void testOfOneMsSecondSetupAndOneMsSecundRun() throws InterruptedException {
		this.state = true;
		TestManager.instance().reportStep("Sleeping a while");
		Thread.sleep(10);
		TestManager.instance().reportStep("Sleeping done!");
		assertTrue(state);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testThatThrowsException() {
		TestManager.instance().reportStep("Going to throw exception");
		throw new IllegalStateException("This exception was to be expected..");
	}
}
