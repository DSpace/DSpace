/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutBrowseComponentRest;
import org.dspace.layout.CrisLayoutBrowseComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link CrisLayoutSectionComponentConverter} for
 * {@link CrisLayoutBrowseComponent}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Component
public class CrisLayoutBrowseComponentConverter implements CrisLayoutSectionComponentConverter {

    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutBrowseComponent;
    }

    @Override
    public CrisLayoutBrowseComponentRest convert(CrisLayoutSectionComponent component) {
        CrisLayoutBrowseComponent browseComponent = (CrisLayoutBrowseComponent) component;
        CrisLayoutBrowseComponentRest rest = new CrisLayoutBrowseComponentRest();
        rest.setBrowseNames(browseComponent.getBrowseNames());
        rest.setStyle(component.getStyle());
        return rest;
    }

}
