/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisLayoutBoxConfiguration to the REST data
 * model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutBoxConfigurationConverter
        implements DSpaceConverter<CrisLayoutBoxConfiguration, CrisLayoutBoxConfigurationRest> {

    @Autowired
    private List<CrisLayoutBoxConfigurator> configurators;

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.app.rest.converter.DSpaceConverter#convert (java.lang.Object,
     * org.dspace.app.rest.projection.Projection)
     */
    @Override
    public CrisLayoutBoxConfigurationRest convert(CrisLayoutBoxConfiguration mo, Projection projection) {
        CrisLayoutBox box = mo.getLayoutBox();
        for (CrisLayoutBoxConfigurator configurator : configurators) {
            if (configurator.support(box)) {
                return configurator.getConfiguration(box);
            }
        }
        return null;
    }

    public void configure(Context context, CrisLayoutBox box, CrisLayoutBoxConfigurationRest rest) {
        for (CrisLayoutBoxConfigurator configurator : configurators) {
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
    public Class<CrisLayoutBoxConfiguration> getModelClass() {
        return CrisLayoutBoxConfiguration.class;
    }
}
