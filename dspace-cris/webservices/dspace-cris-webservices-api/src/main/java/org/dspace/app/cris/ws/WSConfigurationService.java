/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;

import java.io.File;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.services.ConfigurationService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class WSConfigurationService
{
    private ConfigurationService configurationService;
    
    public void setConfigurationService(
            ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }
    
    public ConfigurationService getConfigurationService()
    {
        return configurationService;
    }
    
    public String getMainSchema()
    {
        String property = getXSDFolder();
        return "file:"+property+"crisrequest.xsd";
    }

    private String getXSDFolder()
    {
        String property = configurationService.getProperty("cris.webservices.xsd.path");
        if (!property.endsWith(File.separator))
        {
            property += File.separator;
        }
        return property;
    }
    
    public String getLocationUri()
    {
        return configurationService.getProperty("dspace.url")+"/webservices/";
    }
    
    public Resource getFileMainSchema()
    {        
        return new FileSystemResource(getXSDFolder()+"crisrequest.xsd");
    }
}
