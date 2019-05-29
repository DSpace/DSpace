package org.dspace.handle;

import no.bibsys.dlr.integrations.no.bibsys.handle.HandleServiceClient;
import org.apache.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;

import javax.naming.ConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BibsysHandleService implements AutoCloseable {


    private static final Logger log = Logger.getLogger(BibsysHandleService.class);
    private static final String HANDLE_CONFIGURATION = "handleservice.configuration";
    private static String instanceBaseUrl;
    private static HandleServiceClient handleServiceClient;

    private BibsysHandleService() {
    }

    //TODO. The singelton pattern is considered an antipattern. We should strive to come up with a better solution
    public HandleServiceClient getHandleServiceClient() {
        if (handleServiceClient == null) {
            String configurationPath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(HANDLE_CONFIGURATION);
            try {
                Properties handleProperties = readProperties(configurationPath);
                String databaseConnectURI = handleProperties.getProperty("databaseServerName") + ":" + handleProperties.getProperty("portNumber") + "/" + handleProperties.getProperty("databaseName");
                handleServiceClient = new HandleServiceClient(
                        handleProperties.getProperty("handlePrefix"),
                        databaseConnectURI,
                        handleProperties.getProperty("user"),
                        handleProperties.getProperty("password"),
                        handleProperties.getProperty("HANDLESERVICE_LOCAL_RESOLVER")
                );
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }
        return handleServiceClient;
    }


    public Properties readProperties(String configurationPath) throws ConfigurationException {
        Properties handleProperties = new Properties();
        try {
            handleProperties.load(new FileInputStream(configurationPath));
        } catch (IOException | NullPointerException e) {
            throw new ConfigurationException(
                    configurationPath == null ?
                            "configurationPath is null" :
                            "Failed to load configuration from " + configurationPath);
        }
        return handleProperties;
    }


    public String createHandleId() {
        if (instanceBaseUrl == null) {
            instanceBaseUrl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.url") + "/handle/";
        }
        return createHandleId(instanceBaseUrl);
    }

    private String createHandleId(String instanceBaseUrl) {
        String handle = "";
        try {
            handle = getHandleServiceClient().createHandleAppendToEndpont(instanceBaseUrl);
        } catch (IOException e) {
            log.error(e);
        }
        return handle;
    }

    @Override
    public void close() throws Exception {
        if (handleServiceClient != null) {
            handleServiceClient.close();
            handleServiceClient = null;
        }
    }
}