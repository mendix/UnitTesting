package unittesting;

import org.apache.commons.lang3.SystemUtils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import unittesting.proxies.constants.Constants;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigurationManager {
	enum IsEnabled {
		UNDETERMINED, TRUE, FALSE
	}

	public static final ILogNode LOG = Core.getLogger("UnitTestRunner");

	private static IsEnabled isEnabled = IsEnabled.UNDETERMINED;
	private static boolean isInitialized = false;

	public static boolean isLocalDevelopment() {
		if (!(SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC_OSX))
			return false;

		String modelerPath = String.join(File.separator,
				new String[] { Core.getConfiguration().getRuntimePath().getParent(), "modeler" });

		if (!Files.exists(Paths.get(modelerPath))) {
			LOG.trace("Could not find modeler path: " + modelerPath);
			return false;
		}

		return true;
	}

	public static boolean verifyModuleIsEnabled() {
		if (isEnabled()) return true;

		LOG.warn("Unit testing is disabled on this environment. Read the module documentation for more details");
		return false;
	}

	public static boolean isEnabled() {
		if (isEnabled.equals(IsEnabled.UNDETERMINED)) {
			if (isLocalDevelopment()) {
				LOG.debug("Unit testing is enabled for local development on OS: " + SystemUtils.OS_NAME);
				isEnabled = IsEnabled.TRUE;
			} else if (Constants.getEnabled()) {
				LOG.debug("Unit testing is enabled for this environment on OS: " + SystemUtils.OS_NAME);
				isEnabled = IsEnabled.TRUE;
			} else {
				LOG.debug("Unit testing is disabled for this environment on OS: " + SystemUtils.OS_NAME);
				isEnabled = IsEnabled.FALSE;
			}
		}

		return isEnabled.equals(IsEnabled.TRUE);
	}

	public static void initialize() {
		isInitialized = true;
	}

	public static boolean isInitialized() {
		return isInitialized;
	}
}
