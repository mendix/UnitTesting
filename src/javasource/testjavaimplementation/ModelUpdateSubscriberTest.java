package testjavaimplementation;

import com.mendix.core.Core;
import org.junit.Before;
import org.junit.Test;
import unittesting.ModelUpdateSubscriber;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModelUpdateSubscriberTest {
    private ModelUpdateSubscriber subscriber;
    private static final String MODEL_UPDATE_MESSAGE = "Application model has been updated, application is now available.";

    @Before
    public void setup() {
        subscriber = ModelUpdateSubscriber.getInstance();
        subscriber.setRefreshRequired(false);
    }

    @Test
    public void refreshRequiredShouldBeFalseByDefault() {
        assertFalse(subscriber.getRefreshRequired());
    }

    @Test
    public void refreshRequiredShouldBeTrueAfterModelUpdate() throws InterruptedException {
        Core.getLogger("Core").info(MODEL_UPDATE_MESSAGE);
        Thread.sleep(100);

        assertTrue(subscriber.getRefreshRequired());
    }
}
