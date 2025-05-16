package unittesting;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.logging.LogLevel;
import com.mendix.logging.LogMessage;
import com.mendix.logging.LogSubscriber;

public class ModelUpdateSubscriber extends LogSubscriber {
    private static ModelUpdateSubscriber instance = null;
    private boolean refreshRequired = false;

    private static final ILogNode LOG = ConfigurationManager.LOG;
    private static final String LOG_NODE = "Core";
    private static final LogLevel LOG_LEVEL = LogLevel.INFO;
    private static final String MODEL_UPDATE_MESSAGE = "Application model has been updated, application is now available.";

    public ModelUpdateSubscriber() {
        super(ModelUpdateSubscriber.class.getName(), LogLevel.NONE);
    }

    public static synchronized ModelUpdateSubscriber getInstance() {
        if (instance == null)
            instance = new ModelUpdateSubscriber();

        return instance;
    }

    public void start() {
        Core.getLogger(LOG_NODE).subscribe(this, LOG_LEVEL);
        LOG.debug("Subscribed to model updates");
    }

    public boolean getRefreshRequired() {
        return this.refreshRequired;
    }

    public void setRefreshRequired(boolean refreshRequired) {
        this.refreshRequired = refreshRequired;
    }

    @Override
    public void processMessage(LogMessage logMessage) {
        if (MODEL_UPDATE_MESSAGE.equals(logMessage.message)) {
            LOG.debug("Model has been updated; require refresh of unit tests");
            this.setRefreshRequired(true);
        }
    }
}
