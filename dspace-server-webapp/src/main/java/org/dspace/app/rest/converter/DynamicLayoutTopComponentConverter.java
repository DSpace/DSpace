/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Objects;

import org.dspace.app.rest.model.DynamicLayoutSectionRest.DynamicLayoutTopComponentRest;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.dspace.layout.DynamicLayoutTopComponent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DynamicLayoutSectionComponentConverter} for
 * {@link DynamicLayoutTopComponent}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class DynamicLayoutTopComponentConverter implements DynamicLayoutSectionComponentConverter {

    @Override
    public boolean support(DynamicLayoutSectionComponent component) {
        return component instanceof DynamicLayoutTopComponent;
    }

    @Override
    public DynamicLayoutTopComponentRest convert(DynamicLayoutSectionComponent component) {
        DynamicLayoutTopComponent topComponent = (DynamicLayoutTopComponent) component;
        DynamicLayoutTopComponentRest rest = new DynamicLayoutTopComponentRest();
        boolean showThumbnails =
            Objects.isNull(topComponent.getShowThumbnails()) ?
                false : topComponent.getShowThumbnails();

        rest.setDiscoveryConfigurationName(topComponent.getDiscoveryConfigurationName());
        rest.setOrder(topComponent.getOrder());
        rest.setSortField(topComponent.getSortField());
        rest.setStyle(component.getStyle());
        rest.setTitleKey(topComponent.getTitleKey());
        rest.setNumberOfItems(topComponent.getNumberOfItems());
        rest.setShowThumbnails(showThumbnails);
        return rest;
    }

}
