/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutFacetComponentRest;
import org.dspace.layout.CrisLayoutFacetComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link CrisLayoutSectionComponentConverter} for
 * {@link CrisLayoutFacetComponent}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class CrisLayoutFacetComponentConverter implements CrisLayoutSectionComponentConverter {

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutFacetComponent;
    }

    @Override
    public CrisLayoutFacetComponentRest convert(CrisLayoutSectionComponent component) {
        CrisLayoutFacetComponent topComponent = (CrisLayoutFacetComponent) component;
        CrisLayoutFacetComponentRest rest = new CrisLayoutFacetComponentRest();
        rest.setDiscoveryConfigurationName(topComponent.getDiscoveryConfigurationName());
        rest.setStyle(component.getStyle());
        rest.setFacetsPerRow(topComponent.getFacetsPerRow());
        return rest;
    }

}
