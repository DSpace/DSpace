/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.helpers.stubs;

import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.exceptions.ConfigurationException;
import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.Configuration;
import org.dspace.xoai.services.api.config.XOAIManagerResolver;
import org.dspace.xoai.services.api.config.XOAIManagerResolverException;
import org.dspace.xoai.services.api.xoai.DSpaceFilterResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class StubbedXOAIManagerResolver implements XOAIManagerResolver {
    @Autowired ResourceResolver resourceResolver;
    @Autowired
    DSpaceFilterResolver filterResolver;

    private Configuration builder = new Configuration();

    public Configuration configuration () {
        return builder;
    }

    @Override
    public XOAIManager getManager() throws XOAIManagerResolverException {
        try {
            return new XOAIManager(filterResolver, resourceResolver, builder);
        } catch (ConfigurationException e) {
            throw new XOAIManagerResolverException(e);
        }
    }
}
