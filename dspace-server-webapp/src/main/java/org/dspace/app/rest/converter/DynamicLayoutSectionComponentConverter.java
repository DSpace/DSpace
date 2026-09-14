/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DynamicLayoutSectionRest.DynamicLayoutSectionComponentRest;
import org.dspace.layout.DynamicLayoutSectionComponent;

/**
 * Interface to mark all the DynamicLayoutSectionComponent converters.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public interface DynamicLayoutSectionComponentConverter {

    /**
     * Returns true if the converter is able to convert the given CRIS layout
     * section component.
     *
     * @param component the layout section component to convert
     * @return true if the convertion can be performed, false otherwise
     */
    public boolean support(DynamicLayoutSectionComponent component);

    /**
     * Convert the given CRIS layout section component to the related REST resource.
     * 
     * @param component the layout section component to convert
     * @return the rest resource created from the given component
     */
    public DynamicLayoutSectionComponentRest convert(DynamicLayoutSectionComponent component);
}
