package system;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.mendix.core.Core;
import com.mendix.core.component.LocalComponent;
import com.mendix.core.component.MxRuntime;
import com.mendix.integration.Integration;

@Component(immediate = true, properties = {"event.topics:String=com/mendix/events/model/loaded"})
public class UserActionsRegistrar implements EventHandler
{
	private MxRuntime mxRuntime;
	private LocalComponent component;
	private Integration integration;
	
	@Reference
	public void setMxRuntime(MxRuntime runtime)
	{
		mxRuntime = runtime;
		mxRuntime.bundleComponentLoaded();
	}
	
	@Reference
	public void setIntegration(Integration integration)
	{
		this.integration = integration;
	}
	
	@Override
	public void handleEvent(Event event)
	{
		if (event.getTopic().equals(com.mendix.core.event.EventConstants.ModelLoadedTopic()))        
		{
			component = mxRuntime.getMainComponent();
			Core.initialize(component, integration);   
			component.actionRegistry().registerUserAction(objecthandling.actions.clone.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.commitInSeparateDatabaseTransaction.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.copyAttributes.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.createObjectListFromObject.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.deepClone.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.deleteAll.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.deleteInSeparateTransaction.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.deleteWithoutEvents.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.EndTransaction.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.getCreatedByUser.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.getGUID.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.getLastChangedByUser.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.getOriginalValueAsString.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.getTypeAsString.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.memberHasChanged.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.objectHasChanged.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.refreshClassByObject.class);
			component.actionRegistry().registerUserAction(objecthandling.actions.StartTransaction.class);
			component.actionRegistry().registerUserAction(system.actions.VerifyPassword.class);
			component.actionRegistry().registerUserAction(unittesting.actions.FindAllUnitTests.class);
			component.actionRegistry().registerUserAction(unittesting.actions.ReportStepJava.class);
			component.actionRegistry().registerUserAction(unittesting.actions.RunAllUnitTestsWrapper.class);
			component.actionRegistry().registerUserAction(unittesting.actions.RunUnitTest.class);
			component.actionRegistry().registerUserAction(unittesting.actions.StartRemoteApiServlet.class);
			component.actionRegistry().registerUserAction(unittesting.actions.StartRunAllSuites.class);
			component.actionRegistry().registerUserAction(unittesting.actions.ThrowAssertionFailed.class);
		}
	}
}