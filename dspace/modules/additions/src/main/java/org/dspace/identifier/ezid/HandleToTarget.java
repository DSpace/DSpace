package org.dspace.identifier.ezid;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class HandleToTarget implements Transform
{

    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

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

}
