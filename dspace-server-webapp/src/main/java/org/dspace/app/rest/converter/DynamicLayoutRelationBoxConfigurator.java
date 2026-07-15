/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.DynamicLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.DynamicLayoutBoxRelationConfigurationRest;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBoxTypes;
import org.springframework.stereotype.Component;

/**
 * This is the configurator for relation layout box
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class DynamicLayoutRelationBoxConfigurator implements DynamicLayoutBoxConfigurator {

    @Override
    public boolean support(DynamicLayoutBox box) {
        return StringUtils.equals(box.getType(), DynamicLayoutBoxTypes.RELATION.name());
    }

    @Override
    public DynamicLayoutBoxConfigurationRest getConfiguration(DynamicLayoutBox box) {
        DynamicLayoutBoxRelationConfigurationRest rest = new DynamicLayoutBoxRelationConfigurationRest();
        StringBuilder discoveryConfiguration = new StringBuilder(DynamicLayoutBoxTypes.RELATION.name());
        discoveryConfiguration.append(".");
        discoveryConfiguration.append(box.getEntitytype().getLabel());
        discoveryConfiguration.append(".");
        discoveryConfiguration.append(box.getShortname());
        rest.setDiscoveryConfiguration(discoveryConfiguration.toString());
        return rest;
    }

    @Override
    public void configure(Context context, DynamicLayoutBox box, DynamicLayoutBoxConfigurationRest rest) {
        // Nothing to do
    }

}
