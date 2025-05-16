package unittesting;

import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import unittesting.activities.AssertActivity;
import unittesting.proxies.Assertion;
import unittesting.proxies.UnitTestContext;

public class UnitTestContextManager {
    private static final ILogNode LOG = ConfigurationManager.LOG;

    public static UnitTestContext createUnitTestContext(IContext context, String microflowName) {
        UnitTestContext unitTestContext = new UnitTestContext(context);
        unitTestContext.setTestName(microflowName);

        LOG.trace("Created unit test context for " + microflowName);

        return unitTestContext;
    }

    public static Assertion addAssertion(IContext context, UnitTestContext unitTestContext, AssertActivity assertActivity) {
        Assertion assertion = new Assertion(context);
        assertion.setAssertion_UnitTestContext(unitTestContext);
        assertion.setSequence(assertActivity.getSequence());
        assertion.setName(assertActivity.getName());
        assertion.setResult(assertActivity.getResultEnum());
        assertion.setMessage(assertActivity.getMessage());

        return assertion;
    }
}
