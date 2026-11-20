/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DynamicLayoutSectionRest.DynamicLayoutSearchComponentRest;
import org.dspace.layout.DynamicLayoutSearchComponent;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DynamicLayoutSectionComponentConverter} for
 * {@link DynamicLayoutSearchComponent}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class DynamicLayoutSearchComponentConverter implements DynamicLayoutSectionComponentConverter {

    @Override
    public boolean support(DynamicLayoutSectionComponent component) {
        return component instanceof DynamicLayoutSearchComponent;
    }

    @Override
    public DynamicLayoutSearchComponentRest convert(DynamicLayoutSectionComponent component) {
        DynamicLayoutSearchComponent topComponent = (DynamicLayoutSearchComponent) component;
        DynamicLayoutSearchComponentRest rest = new DynamicLayoutSearchComponentRest();
        rest.setDiscoveryConfigurationName(topComponent.getDiscoveryConfigurationName());
        rest.setStyle(component.getStyle());
        rest.setSearchType(topComponent.getSearchType());
        rest.setInitialStatements(topComponent.getInitialStatements());
        rest.setDisplayTitle(topComponent.isDisplayTitle());
        return rest;
    }

}
