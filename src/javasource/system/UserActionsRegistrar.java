package system;

import com.mendix.core.actionmanagement.IActionRegistrator;

public class UserActionsRegistrar
{
  public void registerActions(IActionRegistrator registrator)
  {
    registrator.bundleComponentLoaded();
    registrator.registerUserAction(feedbackmodule.actions.ValidateEmail.class);
    registrator.registerUserAction(feedbackmodule.actions.XSS_Sanitizer.class);
    registrator.registerUserAction(system.actions.VerifyPassword.class);
    registrator.registerUserAction(unittesting.actions.AssertUsingExpression.class);
    registrator.registerUserAction(unittesting.actions.FindAllUnitTests.class);
    registrator.registerUserAction(unittesting.actions.Initialize.class);
    registrator.registerUserAction(unittesting.actions.IsEnabled.class);
    registrator.registerUserAction(unittesting.actions.IsInitialized.class);
    registrator.registerUserAction(unittesting.actions.RegisterModelUpdateSubscriber.class);
    registrator.registerUserAction(unittesting.actions.ReportStepJava.class);
    registrator.registerUserAction(unittesting.actions.RunAllUnitTestsWrapper.class);
    registrator.registerUserAction(unittesting.actions.RunUnitTest.class);
    registrator.registerUserAction(unittesting.actions.StartRemoteApiServlet.class);
    registrator.registerUserAction(unittesting.actions.StartRunAllSuites.class);
    registrator.registerUserAction(unittesting.actions.TestRefreshRequired.class);
    registrator.registerUserAction(unittesting.actions.ThrowAssertionFailed.class);
    registrator.registerUserAction(unittesting.actions.UpdateTestSuiteCountersAndResult.class);
  }
}
