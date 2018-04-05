package org.dspace.identifier.ezid;

import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class HandleToTarget implements Transform
{

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Override
    public String transform(String identifierURI) throws Exception
    {
        String cPrefix = configurationService
                .getProperty("handle.canonical.prefix");
        String hPrefix = configurationService.getProperty("handle.prefix");
        String prefix = cPrefix + hPrefix;
        if (identifierURI.startsWith(prefix))
        {
            return identifierURI;
        }
        else
        {
            throw new Exception();
        }
    }

    public void setConfigurationService(ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }

}
