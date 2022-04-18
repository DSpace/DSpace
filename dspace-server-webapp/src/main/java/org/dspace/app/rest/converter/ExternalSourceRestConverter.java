/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ExternalSourceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.external.provider.ExternalDataProvider;
import org.springframework.stereotype.Component;

/**
 * This converter deals with the conversion between ExternalDataProvider objects and ExternalSourceRest objects
 */
@Component
public class ExternalSourceRestConverter implements DSpaceConverter<ExternalDataProvider, ExternalSourceRest> {

    public ExternalSourceRest convert(ExternalDataProvider modelObject, Projection projection) {
        ExternalSourceRest externalSourceRest = new ExternalSourceRest();
        externalSourceRest.setId(modelObject.getSourceIdentifier());
        externalSourceRest.setName(modelObject.getSourceIdentifier());
        externalSourceRest.setHierarchical(false);
        return externalSourceRest;
    }

    public Class<ExternalDataProvider> getModelClass() {
        return ExternalDataProvider.class;
    }
}
