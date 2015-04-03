/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Map;

import org.dspace.core.ConfigurationManager;

public abstract class AbstractPIDService {

	public String PIDServiceURL;
	public String PIDServiceUSER;
	public String PIDServicePASS;

	class PIDServiceAuthenticator extends Authenticator {
		public PasswordAuthentication getPasswordAuthentication() {
			return (new PasswordAuthentication(PIDServiceUSER,
					PIDServicePASS.toCharArray()));
		}
	}

	public enum HTTPMethod {
		GET, POST, PUT, DELETE
	}
	
	public enum PARAMS {
		PID, DATA, COMMAND, REGEX, HEADER
	}
	
	public enum HANDLE_FIELDS {
		URL
	}

	public PIDServiceAuthenticator authenticator = null;

	public AbstractPIDService() throws Exception {
		PIDServiceURL = ConfigurationManager.getProperty("lr", "lr.pid.service.url");
		PIDServiceUSER = ConfigurationManager.getProperty("lr", "lr.pid.service.user");
		PIDServicePASS = ConfigurationManager.getProperty("lr", "lr.pid.service.pass");
		if (PIDServiceURL == null || PIDServiceURL.length() == 0)
			throw new Exception("PIDService URL not configured.");
		authenticator = new PIDServiceAuthenticator();
		Authenticator.setDefault(authenticator);
	}

	public abstract String sendPIDCommand(HTTPMethod method, Map<String, Object> params) throws Exception;

	public abstract String resolvePID(String PID) throws Exception;

	public abstract String createPID(Map<String, String> handleFields, String prefix) throws Exception;

	public abstract String createCustomPID(Map<String, String> handleFields, String prefix, String suffix) throws Exception;

	public abstract String modifyPID(String PID, Map<String, String> handleFields) throws Exception;

	public abstract String deletePID(String PID) throws Exception;

	public abstract String findHandle(Map<String, String> handleFields, String prefix) throws Exception;

	public abstract boolean supportsCustomPIDs() throws Exception;

	public abstract String whoAmI(String encoding) throws Exception;
	
}
