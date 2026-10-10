/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DynamicLayoutSectionRest.DynamicLayoutBrowseComponentRest;
import org.dspace.layout.DynamicLayoutBrowseComponent;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DynamicLayoutSectionComponentConverter} for
 * {@link DynamicLayoutBrowseComponent}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class DynamicLayoutBrowseComponentConverter implements DynamicLayoutSectionComponentConverter {

    @Override
    public boolean support(DynamicLayoutSectionComponent component) {
        return component instanceof DynamicLayoutBrowseComponent;
    }

    @Override
    public DynamicLayoutBrowseComponentRest convert(DynamicLayoutSectionComponent component) {
        DynamicLayoutBrowseComponent browseComponent = (DynamicLayoutBrowseComponent) component;
        DynamicLayoutBrowseComponentRest rest = new DynamicLayoutBrowseComponentRest();
        rest.setBrowseNames(browseComponent.getBrowseNames());
        rest.setStyle(component.getStyle());
        return rest;
    }

}
