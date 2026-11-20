/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DynamicLayoutSectionRest.DynamicLayoutFacetComponentRest;
import org.dspace.layout.DynamicLayoutFacetComponent;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DynamicLayoutSectionComponentConverter} for
 * {@link DynamicLayoutFacetComponent}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class DynamicLayoutFacetComponentConverter implements DynamicLayoutSectionComponentConverter {

    @Override
    public boolean support(DynamicLayoutSectionComponent component) {
        return component instanceof DynamicLayoutFacetComponent;
    }

    @Override
    public DynamicLayoutFacetComponentRest convert(DynamicLayoutSectionComponent component) {
        DynamicLayoutFacetComponent topComponent = (DynamicLayoutFacetComponent) component;
        DynamicLayoutFacetComponentRest rest = new DynamicLayoutFacetComponentRest();
        rest.setDiscoveryConfigurationName(topComponent.getDiscoveryConfigurationName());
        rest.setStyle(component.getStyle());
        rest.setFacetsPerRow(topComponent.getFacetsPerRow());
        return rest;
    }

}
