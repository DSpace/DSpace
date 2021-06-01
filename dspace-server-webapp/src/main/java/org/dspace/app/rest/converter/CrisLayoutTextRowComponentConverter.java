/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.dspace.layout.CrisLayoutTextRowComponent;
import org.springframework.stereotype.Component;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@Component
public class CrisLayoutTextRowComponentConverter implements CrisLayoutSectionComponentConverter {
    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutTextRowComponent;
    }

    @Override
    public CrisLayoutSectionRest.CrisLayoutSectionComponentRest convert(CrisLayoutSectionComponent component) {
        CrisLayoutTextRowComponent textComponent = (CrisLayoutTextRowComponent) component;

        return new CrisLayoutSectionRest.CrisLayoutTextRowComponentRest(textComponent.getOrder(),
            textComponent.getStyle(), textComponent.getContent(), textComponent.getContentType());
    }
}
