/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.dspace.app.rest.model.DynamicLayoutBoxConfigurationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBoxConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity DynamicLayoutBoxConfiguration to the REST data
 * model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class DynamicLayoutBoxConfigurationConverter
        implements DSpaceConverter<DynamicLayoutBoxConfiguration, DynamicLayoutBoxConfigurationRest> {

    @Autowired
    private List<DynamicLayoutBoxConfigurator> configurators;

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.app.rest.converter.DSpaceConverter#convert (java.lang.Object,
     * org.dspace.app.rest.projection.Projection)
     */
    @Override
    public DynamicLayoutBoxConfigurationRest convert(DynamicLayoutBoxConfiguration mo, Projection projection) {
        DynamicLayoutBox box = mo.getLayoutBox();
        for (DynamicLayoutBoxConfigurator configurator : configurators) {
            if (configurator.support(box)) {
                return configurator.getConfiguration(box);
            }
        }
        return null;
    }

    /**
     * Applies the matching configurators to populate the given box configuration.
     *
     * @param context the DSpace context
     * @param box the layout box
     * @param rest the box configuration to populate
     */
    public void configure(Context context, DynamicLayoutBox box, DynamicLayoutBoxConfigurationRest rest) {
        for (DynamicLayoutBoxConfigurator configurator : configurators) {
            if (configurator.support(box)) {
                configurator.configure(context, box, rest);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<DynamicLayoutBoxConfiguration> getModelClass() {
        return DynamicLayoutBoxConfiguration.class;
    }
}
