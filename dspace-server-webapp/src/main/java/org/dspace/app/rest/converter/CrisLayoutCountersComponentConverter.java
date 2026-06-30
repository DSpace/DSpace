/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutSectionRest;
import org.dspace.layout.CrisLayoutCountersComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * converter for component {@link CrisLayoutCountersComponent}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
@Component
public class CrisLayoutCountersComponentConverter implements CrisLayoutSectionComponentConverter {
    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutCountersComponent;
    }

    @Override
    public CrisLayoutSectionRest.CrisLayoutSectionComponentRest convert(CrisLayoutSectionComponent component) {
        return CrisLayoutSectionRest.CrisLayoutCountersComponentRest.from(
            (CrisLayoutCountersComponent) component);
    }
}
