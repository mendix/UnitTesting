package unittesting;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JUnitExample2 extends AbstractUnitTest {

	@Before
	public void setup() throws InterruptedException {
		Thread.sleep(10);
	}

	@After
	public void tearDown() throws InterruptedException {
		Thread.sleep(10);
	}

	@Test
	public void testWithTimeMeasurement() {
		this.startTimeMeasure();

		this.reportStep(
				"By inheriting from AbstractUnitTest some utility methods are provided and time can be tracked in a more reliable way (without counting setup and teardown)");

		assertTrue(true);

		this.endTimeMeasure();
	}

}
