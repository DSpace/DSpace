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

    public static BibsysHandleService getService() {
        if (handleServiceClient == null) {
            String configurationPath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(HANDLE_CONFIGURATION);
            try {
                Properties handleProperties = readProperties(configurationPath);
                handleServiceClient = new HandleServiceClient(
                        handleProperties.getProperty("handlePrefix"),
                        handleProperties.getProperty("databaseServerName"),
                        handleProperties.getProperty("databaseName"),
                        handleProperties.getProperty("portNumber"),
                        handleProperties.getProperty("user"),
                        handleProperties.getProperty("password"),
                        handleProperties.getProperty("HANDLESERVICE_LOCAL_RESOLVER")
                );
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }
        return new BibsysHandleService();
    }


    public static Properties readProperties(String configurationPath) throws ConfigurationException {
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
            handle = handleServiceClient.createHandleAppendToEndpoint(instanceBaseUrl);
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