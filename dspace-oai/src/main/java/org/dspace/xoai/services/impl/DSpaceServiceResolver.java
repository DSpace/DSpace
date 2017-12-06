/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl;

import com.lyncode.builder.MapBuilder;
import com.lyncode.xoai.dataprovider.services.api.ResourceResolver;
import org.dspace.xoai.services.api.ServiceResolver;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.impl.config.DSpaceConfigurationService;
import org.dspace.xoai.services.impl.resources.DSpaceResourceResolver;

import java.util.Map;

public class DSpaceServiceResolver implements ServiceResolver {
    private Map<String, Object> services = new MapBuilder<String, Object>()
            .withPair(ConfigurationService.class.getName(), new DSpaceConfigurationService())
            .withPair(ResourceResolver.class.getName(), new DSpaceResourceResolver())
            .build();

    @Override
    public <T> T getService(Class<T> type) {
        return (T) services.get(type.getName());
    }
}
