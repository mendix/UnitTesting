package system;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.mendix.core.Core;
import com.mendix.core.component.LocalComponent;
import com.mendix.core.component.MxRuntime;

@Component(immediate = true, properties = {"event.topics:String=com/mendix/events/model/loaded"})
public class UserActionsRegistrar implements EventHandler
{
	private MxRuntime mxRuntime;
	private LocalComponent component;
	
	@Reference
	public void setMxRuntime(MxRuntime runtime)
	{
		mxRuntime = runtime;
		mxRuntime.bundleComponentLoaded();
	}
	
	@Override
	public void handleEvent(Event event)
	{
		if (event.getTopic().equals(com.mendix.core.event.EventConstants.ModelLoadedTopic()))        
		{
			component = mxRuntime.getMainComponent();
			Core.setComponent(component);   
			component.actionRegistry().registerUserAction(communitycommons.actions.acquireLock.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.Base64Decode.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.Base64DecodeToFile.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.Base64Encode.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.Base64EncodeFile.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.Clone.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.commitWithoutEvents.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.DateTimeToLong.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.DecryptString.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.DeepClone.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.Delay.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.deleteAll.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.DuplicateFileDocument.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.DuplicateImageDocument.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.encryptMemberIfChanged.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.EncryptString.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.EscapeHTML.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.executeMicroflowAsUser.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.executeMicroflowAsUser_1.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.executeMicroflowAsUser_2.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.executeMicroflowInBackground.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.executeMicroflowInBatches.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.GenerateHMAC_SHA256_hash.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.GenerateThumbnail.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.GetApplicationUrl.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.getCreatedByUser.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.getDTAPMode.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.getFileSize.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.getGUID.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.GetIntFromDateTime.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.getLastChangedByUser.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.getLockOwner.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.getOriginalValueAsString.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.GetRuntimeVersion.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.getTypeAsString.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.Hash.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.HTMLEncode.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.HTMLToPlainText.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.Log.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.LongToDateTime.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.memberHasChanged.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.objectHasChanged.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.RandomHash.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.RandomString.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.RandomStrongPassword.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.recommitInBatches.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.refreshClass.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.refreshClassByObject.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.RegexReplaceAll.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.RegexTest.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.releaseAllInactiveLocks.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.releaseLock.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.retrieveURL.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.RunMicroflowAsyncInQueue.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.SimpleLog.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.storeURLToFileDocument.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.StringFromFile.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.StringLeftPad.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.StringLength.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.StringRightPad.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.StringToFile.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.StringTrim.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.SubstituteTemplate.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.SubstituteTemplate2.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.ThrowException.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.ThrowWebserviceException.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.TimeMeasureEnd.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.TimeMeasureStart.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.waitForLock.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.XSSSanitize.class);
			component.actionRegistry().registerUserAction(communitycommons.actions.YearsBetween.class);
			component.actionRegistry().registerUserAction(system.actions.VerifyPassword.class);
			component.actionRegistry().registerUserAction(unittesting.actions.FindAllUnitTests.class);
			component.actionRegistry().registerUserAction(unittesting.actions.ReportStepJava.class);
			component.actionRegistry().registerUserAction(unittesting.actions.RunAllUnitTests.class);
			component.actionRegistry().registerUserAction(unittesting.actions.RunAllUnitTestsWrapper.class);
			component.actionRegistry().registerUserAction(unittesting.actions.RunUnitTest.class);
			component.actionRegistry().registerUserAction(unittesting.actions.StartRemoteApiServlet.class);
			component.actionRegistry().registerUserAction(unittesting.actions.ThrowAssertionFailed.class);
		}
	}
}