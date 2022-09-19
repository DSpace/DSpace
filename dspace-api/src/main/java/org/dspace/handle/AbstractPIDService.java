/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Map;

import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.springframework.stereotype.Component;

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
/**
 * Abstract class for PID service which manages EPIC handles.
 * This class loads the parameters from configuration for calling EPIC API.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@Component
public abstract class AbstractPIDService {
    public String PIDServiceURL;
    public String PIDServiceUSER;
    public String PIDServicePASS;

    private ConfigurationService configurationService = new DSpace().getConfigurationService();

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
        URL,
        TITLE,
        REPOSITORY,
        SUBMITDATE,
        REPORTEMAIL,
        DATASETNAME,
        DATASETVERSION,
        QUERY
    }

    public PIDServiceAuthenticator authenticator = null;

    public AbstractPIDService() throws Exception {
        PIDServiceURL = configurationService.getProperty("lr.pid.service.url", "lr.pid.service.url");
        PIDServiceUSER = configurationService.getProperty("lr.pid.service.user", "lr.pid.service.user");
        PIDServicePASS = configurationService.getProperty("lr.pid.service.pass", "lr.pid.service.pass");
        if (PIDServiceURL == null || PIDServiceURL.length() == 0) {
            throw new Exception("PIDService URL not configured.");
        }
        authenticator = new PIDServiceAuthenticator();
        Authenticator.setDefault(authenticator);
    }

    public abstract String sendPIDCommand(HTTPMethod method, Map<String, Object> params) throws Exception;

    public abstract String resolvePID(String PID) throws Exception;

    public abstract String createPID(Map<String, String> handleFields, String prefix) throws Exception;

    public abstract String createCustomPID(Map<String, String> handleFields,
                                           String prefix, String suffix) throws Exception;

    public abstract String modifyPID(String PID, Map<String, String> handleFields) throws Exception;

    public abstract String deletePID(String PID) throws Exception;

    public abstract String findHandle(Map<String, String> handleFields, String prefix) throws Exception;

    public abstract boolean supportsCustomPIDs() throws Exception;

    public abstract String whoAmI(String encoding) throws Exception;

}
