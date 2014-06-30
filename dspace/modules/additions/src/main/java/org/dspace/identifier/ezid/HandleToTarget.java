package org.dspace.identifier.ezid;

import org.dspace.core.ConfigurationManager;

public class HandleToTarget implements Transform
{

    @Override
    public String transform(String identifierURI) throws Exception
    {
        String cPrefix = ConfigurationManager
                .getProperty("handle.canonical.prefix");
        String hPrefix = ConfigurationManager.getProperty("handle.prefix");
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
