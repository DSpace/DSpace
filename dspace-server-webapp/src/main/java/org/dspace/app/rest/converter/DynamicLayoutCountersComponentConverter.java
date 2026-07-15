/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DynamicLayoutSectionRest;
import org.dspace.layout.DynamicLayoutCountersComponent;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * converter for component {@link DynamicLayoutCountersComponent}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
@Component
public class DynamicLayoutCountersComponentConverter implements DynamicLayoutSectionComponentConverter {
    @Override
    public boolean support(DynamicLayoutSectionComponent component) {
        return component instanceof DynamicLayoutCountersComponent;
    }

    @Override
    public DynamicLayoutSectionRest.DynamicLayoutSectionComponentRest convert(DynamicLayoutSectionComponent component) {
        return DynamicLayoutSectionRest.DynamicLayoutCountersComponentRest.from(
            (DynamicLayoutCountersComponent) component);
    }
}
