/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DynamicLayoutSectionRest;
import org.dspace.layout.DynamicLayoutSectionComponent;
import org.dspace.layout.DynamicLayoutTextRowComponent;
import org.springframework.stereotype.Component;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@Component
public class DynamicLayoutTextRowComponentConverter implements DynamicLayoutSectionComponentConverter {
    @Override
    public boolean support(DynamicLayoutSectionComponent component) {
        return component instanceof DynamicLayoutTextRowComponent;
    }

    @Override
    public DynamicLayoutSectionRest.DynamicLayoutSectionComponentRest convert(DynamicLayoutSectionComponent component) {
        DynamicLayoutTextRowComponent textComponent = (DynamicLayoutTextRowComponent) component;

        return new DynamicLayoutSectionRest.DynamicLayoutTextRowComponentRest(textComponent.getOrder(),
            textComponent.getStyle(), textComponent.getContent(), textComponent.getContentType());
    }
}
