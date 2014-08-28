package unittesting;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.auth.InvalidCredentialsException;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.externalinterface.connector.RequestHandler;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;

public class RemoteApiServlet extends RequestHandler {

	private static final Object COMMAND_START = "start";
	private static final Object COMMAND_STATUS = "status";
	private static final String PARAM_PASSWORD = "password";
	
	private final String password;
	private boolean detectedUnitTests = false;
	
	private volatile boolean isRunning = false;
	
	private final ILogNode LOG = TestManager.LOG;
	
	public RemoteApiServlet(String password) {
		this.password = password;
	}

	@Override
	protected void processRequest(IMxRuntimeRequest req,
			IMxRuntimeResponse resp, String path) throws Exception {
		
		HttpServletRequest request = req.getHttpServletRequest();
		HttpServletResponse response = resp.getHttpServletResponse();
		
		try {
			if (!"GET".equals(request.getMethod()))
				response.setStatus(HttpStatus.SC_METHOD_NOT_ALLOWED);
			else if (COMMAND_START.equals(path))
				serveRunStart(request, response, path);
			else if (COMMAND_STATUS.equals(path))
				serveRunStatus(request, response, path);
			else
				response.setStatus(HttpStatus.SC_NOT_FOUND);
		}
		catch (IllegalArgumentException e) {
			response.setStatus(HttpStatus.SC_BAD_REQUEST);
			response.getWriter().write(e.getMessage());
		}
		catch (InvalidCredentialsException e) {
			response.setStatus(HttpStatus.SC_UNAUTHORIZED);
			response.getWriter().write("Invalid password provided");
		}
	}

	private synchronized void serveRunStatus(HttpServletRequest request,
			HttpServletResponse response, String path) throws Exception {
		JSONObject input = parseInput(request);
		verifyPassword(input);
		// TODO Auto-generated method stub
		
	}

	private synchronized void serveRunStart(HttpServletRequest request,
			HttpServletResponse response, String path) throws IOException, CoreException {
		JSONObject input = parseInput(request);
		verifyPassword(input);
	
		IContext context = Core.createSystemContext();
		if (!detectedUnitTests) {
			TestManager.instance().findAllTests(context);
			detectedUnitTests = true;
		}
		
		if (isRunning) {
			throw new IllegalArgumentException("Cannot start a test run while another test run is still running");
		}
		isRunning = true;			
		
		// TODO Auto-generated method stub
	}
		
	

	private void verifyPassword(JSONObject input) throws InvalidCredentialsException {
		if (!input.has(PARAM_PASSWORD))
			throw new IllegalArgumentException("No '" + PARAM_PASSWORD + "' attribute found in the JSON body. Please provide a password");
		
		if (!password.equals(input.getString(PARAM_PASSWORD)))
			throw new InvalidCredentialsException();
	}

	private JSONObject parseInput(HttpServletRequest request) throws IOException {
		String data = IOUtils.toString(request.getInputStream());
		return new JSONObject(data);
	}


}
