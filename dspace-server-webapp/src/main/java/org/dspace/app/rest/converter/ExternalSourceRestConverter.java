/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ExternalSourceRest;
import org.dspace.external.provider.ExternalDataProvider;
import org.springframework.stereotype.Component;

/**
 * This converter deals with the conversion between ExternalDataProvider objects and ExternalSourceRest objects
 */
@Component
public class ExternalSourceRestConverter implements DSpaceConverter<ExternalDataProvider, ExternalSourceRest> {

    @Override
    public ExternalSourceRest fromModel(ExternalDataProvider obj) {
        ExternalSourceRest externalSourceRest = new ExternalSourceRest();
        externalSourceRest.setId(obj.getSourceIdentifier());
        externalSourceRest.setName(obj.getSourceIdentifier());
        externalSourceRest.setHierarchical(false);
        return externalSourceRest;
    }

    @Override
    public ExternalDataProvider toModel(ExternalSourceRest obj) {
        return null;
    }
}
