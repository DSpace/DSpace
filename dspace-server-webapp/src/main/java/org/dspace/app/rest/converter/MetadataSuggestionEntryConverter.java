/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataSuggestionEntryRest;
import org.dspace.app.rest.model.MetadataValueDTOList;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.external.provider.metadata.service.impl.MetadataItemSuggestions;
import org.dspace.workflow.WorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class acts as a converter from the {@link MetadataItemSuggestions} to their REST representation in
 * {@link MetadataSuggestionEntryRest} objects
 */
@Component
public class MetadataSuggestionEntryConverter implements
                                              DSpaceConverter<MetadataItemSuggestions, MetadataSuggestionEntryRest> {

    @Autowired
    private ConverterService converter;

    @Override
    public MetadataSuggestionEntryRest convert(MetadataItemSuggestions obj, Projection projection) {
        MetadataSuggestionEntryRest metadataSuggestionEntryRest = new MetadataSuggestionEntryRest();
        metadataSuggestionEntryRest.setDisplay(obj.getExternalDataObject().getDisplayValue());
        metadataSuggestionEntryRest.setId(obj.getExternalDataObject().getId());
        metadataSuggestionEntryRest.setValue(obj.getExternalDataObject().getValue());
        InProgressSubmission inProgressSubmission = obj.getInProgressSubmission();
        if (inProgressSubmission instanceof WorkflowItem) {
            metadataSuggestionEntryRest.setWorkflowItemId(((WorkflowItem) inProgressSubmission).getID());
        } else {
            metadataSuggestionEntryRest.setWorkspaceItemId(((WorkspaceItem) inProgressSubmission).getID());
        }
        MetadataValueDTOList metadataValueDTOList = new MetadataValueDTOList(obj.getExternalDataObject().getMetadata());
        metadataSuggestionEntryRest
            .setMetadataRest(converter.toRest(metadataValueDTOList, Projection.DEFAULT));
        metadataSuggestionEntryRest.setMetadataSuggestion(obj.getExternalDataObject().getSource());
        return metadataSuggestionEntryRest;
    }

    @Override
    public Class<MetadataItemSuggestions> getModelClass() {
        return MetadataItemSuggestions.class;
    }
}
