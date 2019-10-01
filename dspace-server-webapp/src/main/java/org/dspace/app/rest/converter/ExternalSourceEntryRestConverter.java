/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ExternalSourceEntryRest;
import org.dspace.external.model.ExternalDataObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This converter deals with the conversion between ExternalDataObjects and ExternalSourceEntryRest objects
 */
@Component
public class ExternalSourceEntryRestConverter implements DSpaceConverter<ExternalDataObject, ExternalSourceEntryRest> {

    @Autowired
    private MockMetadataConverter metadataConverter;

    @Override
    public ExternalSourceEntryRest fromModel(ExternalDataObject externalDataObject) {
        ExternalSourceEntryRest externalSourceEntryRest = new ExternalSourceEntryRest();
        externalSourceEntryRest.setId(externalDataObject.getId());
        externalSourceEntryRest.setExternalSource(externalDataObject.getSource());
        externalSourceEntryRest.setDisplay(externalDataObject.getDisplayValue());
        externalSourceEntryRest.setValue(externalDataObject.getValue());
        externalSourceEntryRest.setExternalSource(externalDataObject.getSource());
        externalSourceEntryRest.setMetadata(metadataConverter.convert(externalDataObject.getMetadata()));
        return externalSourceEntryRest;
    }

    @Override
    public ExternalDataObject toModel(ExternalSourceEntryRest obj) {
        return null;
    }
}
