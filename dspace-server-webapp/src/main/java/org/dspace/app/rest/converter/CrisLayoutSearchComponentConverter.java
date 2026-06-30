/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutSearchComponentRest;
import org.dspace.layout.CrisLayoutSearchComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link CrisLayoutSectionComponentConverter} for
 * {@link CrisLayoutSearchComponent}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class CrisLayoutSearchComponentConverter implements CrisLayoutSectionComponentConverter {

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutSearchComponent;
    }

    @Override
    public CrisLayoutSearchComponentRest convert(CrisLayoutSectionComponent component) {
        CrisLayoutSearchComponent topComponent = (CrisLayoutSearchComponent) component;
        CrisLayoutSearchComponentRest rest = new CrisLayoutSearchComponentRest();
        rest.setDiscoveryConfigurationName(topComponent.getDiscoveryConfigurationName());
        rest.setStyle(component.getStyle());
        rest.setSearchType(topComponent.getSearchType());
        rest.setInitialStatements(topComponent.getInitialStatements());
        rest.setDisplayTitle(topComponent.isDisplayTitle());
        return rest;
    }

}
