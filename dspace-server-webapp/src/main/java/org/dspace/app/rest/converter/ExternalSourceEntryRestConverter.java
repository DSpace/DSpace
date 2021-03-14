/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ExternalSourceEntryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.external.model.ExternalDataObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This converter deals with the conversion between ExternalDataObjects and ExternalSourceEntryRest objects
 */
@Component
public class ExternalSourceEntryRestConverter implements DSpaceConverter<ExternalDataObject, ExternalSourceEntryRest> {

    @Autowired
    private MetadataValueDTOListConverter metadataConverter;

    public ExternalSourceEntryRest convert(ExternalDataObject modelObject, Projection projection) {
        ExternalSourceEntryRest externalSourceEntryRest = new ExternalSourceEntryRest();
        externalSourceEntryRest.setId(modelObject.getId());
        externalSourceEntryRest.setExternalSource(modelObject.getSource());
        externalSourceEntryRest.setDisplay(modelObject.getDisplayValue());
        externalSourceEntryRest.setValue(modelObject.getValue());
        externalSourceEntryRest.setExternalSource(modelObject.getSource());
        externalSourceEntryRest.setMetadata(metadataConverter.convert(modelObject.getMetadata()));
        return externalSourceEntryRest;
    }

    public Class<ExternalDataObject> getModelClass() {
        return ExternalDataObject.class;
    }
}
