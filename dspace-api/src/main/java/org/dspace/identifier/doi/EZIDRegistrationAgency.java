/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.identifier.doi;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.Map;
import org.dspace.content.DSpaceObject;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author Mark H. Wood
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public abstract class EZIDRegistrationAgency extends RegistrationAgency {
    private static final Logger log = LoggerFactory.getLogger(DOIIdentifierProvider.class);
    
    protected ConfigurationService configurationService;
    protected DOIIdentifierProvider parentService;
    
    // Configuration property names
    static final String CFG_SHOULDER = "identifier.doi.ezid.shoulder";
    static final String CFG_USER = "identifier.doi.ezid.user";
    static final String CFG_PASSWORD = "identifier.doi.ezid.password";

    /** Factory for EZID requests. */
    private static EZIDRequestFactory requestFactory;
    
    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setParentService(DOIIdentifierProvider parentService) {
        this.parentService = parentService;
    }
    
    public boolean create(String identifier, Map<String,String> metadata) {
        EZIDResponse response;
        try {
            EZIDRequest request = requestFactory.getInstance(loadAuthority(),
                    loadUser(), loadPassword());
            response = request.create(identifier, metadata);
        } catch (IdentifierException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return false;
        } catch (IOException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return false;
        } catch (URISyntaxException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return false;
        }
        if (response.isSuccess() == true) {
            return true;
        } else {
            log.error("Identifier '{}' not registered -- EZID returned: {}",
                    identifier, response.getEZIDStatusValue());
        }
        return false;
    }
    
    public boolean reserve(String identifier, Map<String,String> metadata) throws IdentifierException {
        EZIDResponse response;
        try {    
            EZIDRequest request = requestFactory.getInstance(loadAuthority(),
                    loadUser(), loadPassword());
            metadata.put("_status", "reserved");
            response = request.create(identifier, metadata);
        } catch (IOException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return false;
        } catch (URISyntaxException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return false;
        }
        if (response.isSuccess() == true) {
            return true;
        } else {
            log.error("Identifier '{}' not registered -- EZID returned: {}",
                    identifier, response.getEZIDStatusValue());
        }
        return false;
    }
    
    public String mint(Map<String,String> metadata) throws IdentifierException {
        // Compose the request
        EZIDRequest request;
        try {
            request = requestFactory.getInstance(loadAuthority(), loadUser(), loadPassword());
        } catch (URISyntaxException ex) {
            log.error(ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        }

        // Send the request
        EZIDResponse response;
        try
        {
            response = request.mint(metadata);
        } catch (IOException ex) {
            log.error("Failed to send EZID request:  {}", ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        } catch (URISyntaxException ex) {
            log.error("Failed to send EZID request:  {}", ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        }

        // Good response?
        if (HttpURLConnection.HTTP_CREATED != response.getHttpStatusCode())
        {
            log.error("EZID server responded:  {} {}", response.getHttpStatusCode(),
                    response.getHttpReasonPhrase());
            throw new IdentifierException("DOI not created:  " + response.getHttpReasonPhrase());
        }
        

	// Extract the DOI from the content blob
        if (response.isSuccess())
        {
            String value = response.getEZIDStatusValue();
            int end = value.indexOf('|'); // Following pipe is "shadow ARK"
            if (end < 0)
                end = value.length();
            String doi = value.substring(0, end).trim();
            log.info("Created {}", doi);
            return doi;
        }
        else
        {
            log.error("EZID responded:  {}", response.getEZIDStatusValue());
            throw new IdentifierException("No DOI returned");
        }
    }
    
    @Override
    public boolean delete(String id) throws IdentifierException {
        EZIDResponse response;
        try {
            EZIDRequest request = requestFactory.getInstance(loadAuthority(),
                    loadUser(), loadPassword());
            response = request.delete(id);
        } catch (URISyntaxException e) {
            throw new IdentifierException(e);
        } catch (IOException e) {
            throw new IdentifierException(e);
        }
        if (!response.isSuccess()) {
            log.error("Unable to delete {} from DataCite:  {}", id,
                        response.getEZIDStatusValue());
            return false;
        }
        return true;
    }
    
    /**
     * Get configured value of EZID username.
     * @throws IdentifierException 
     */
    private String loadUser()
            throws IdentifierException
    {
        String user = configurationService.getProperty(CFG_USER);
        if (null != user)
            return user;
        else
            throw new IdentifierException("Unconfigured:  define " + CFG_USER);
    }

    /**
     * Get configured value of EZID password.
     * @throws IdentifierException 
     */
    private String loadPassword()
            throws IdentifierException
    {
        String password = configurationService.getProperty(CFG_PASSWORD);
        if (null != password)
            return password;
        else
            throw new IdentifierException("Unconfigured:  define " + CFG_PASSWORD);
    }

    /**
     * Get configured value of EZID "shoulder".
     * @throws IdentifierException 
     */
    private String loadAuthority()
            throws IdentifierException
    {
        String shoulder = configurationService.getProperty(CFG_SHOULDER);
        if (null != shoulder)
            return shoulder;
        else
            throw new IdentifierException("Unconfigured:  define " + CFG_SHOULDER);
    }
    
    @Required
    public static void setRequestFactory(EZIDRequestFactory aRequestFactory)
    {
        requestFactory = aRequestFactory;
    }

}
