package org.dspace.handle;

import no.bibsys.agora.handle.HandleServiceException;
import no.bibsys.services.handle.HandleResponse;
import no.bibsys.services.handle.HandleServiceBuilder;
import no.bibsys.services.handle.ResponseCode;
import org.apache.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;

import javax.naming.ConfigurationException;
import java.net.MalformedURLException;
import java.net.URL;

public class BibsysHandleService implements AutoCloseable {


    private static final Logger log = Logger.getLogger(BibsysHandleService.class);
    private static final String HANDLE_CONFIGURATION = "handleservice.configuration";
    private static no.bibsys.services.handle.HandleService agoraInstance;
    private static String instanceBaseUrl;

    private BibsysHandleService() {
    }

    //TODO. The singelton pattern is considered an antipattern. We should strive to come up with a better solution
    public static BibsysHandleService getService() {
        if (agoraInstance == null) {
            try {
                String configurationPath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(HANDLE_CONFIGURATION);
                log.info(String.format("Configuring %s using configuration property %s=%s",
                        BibsysHandleService.class.getSimpleName(), HANDLE_CONFIGURATION, configurationPath));
                agoraInstance = HandleServiceBuilder.newInstance()
                                .configuration(configurationPath)
                                .build();
            } catch (ConfigurationException e) {
                throw new HandleServiceException(
                        String.format("Failed to initialize %s using configuration property %s",
                                BibsysHandleService.class.getSimpleName(), HANDLE_CONFIGURATION), e);
            }
        }

        if (instanceBaseUrl == null) {
            instanceBaseUrl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.url") + "/handle/";
        }
        return new BibsysHandleService();
    }

    /**
     * Create a new handle id. The implementation uses an external handle server to create handles.
     * NB Requires that property dspace.url is set in build.properties
     *
     * @return A new handle id
     * @throws HandleServiceException If handle url is malformed
     */
    public String createHandleId() {
        return createHandleId(instanceBaseUrl);
    }

    /**
     * Create a new handle id. The implementation uses an external handle server to create handles.
     *
     * @param instanceBaseUrl the base url of this dspace instance ex. https://brage-alfa.bibsys.no/xmlui
     * @return A new handle id
     * @throws HandleServiceException If instanceBaseUrl is malformed
     */
    private String createHandleId(String instanceBaseUrl) {
        try {
            URL baseAddress;
            baseAddress = new URL(instanceBaseUrl);
            HandleResponse handleResponse = agoraInstance.createLinkAppendToEndpont(baseAddress);
            if (handleResponse.getCode() == ResponseCode.RC_SUCCESS) {
                return handleResponse.getHandle();
            } else {
                throw new HandleServiceException(handleResponse.getMessage());
            }

        } catch (MalformedURLException e) {
            log.error(e);
            throw new HandleServiceException(e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        if (agoraInstance != null) {
            agoraInstance.close();
            agoraInstance = null;
        }
    }
}