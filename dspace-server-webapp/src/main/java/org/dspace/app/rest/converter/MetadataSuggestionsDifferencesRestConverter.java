/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.HashMap;
import java.util.Map;

import org.dspace.app.rest.model.MetadataDifferenceRest;
import org.dspace.app.rest.model.MetadataSuggestionsDifferencesRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.external.provider.metadata.service.impl.MetadataSuggestionDifference;
import org.dspace.external.provider.metadata.service.impl.MetadataSuggestionDifferences;
import org.dspace.workflow.WorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class will act as a converter between the {@link MetadataSuggestionDifferences} object and the
 * {@link MetadataSuggestionsDifferencesRest} object
 */
@Component
public class MetadataSuggestionsDifferencesRestConverter
    implements DSpaceConverter<MetadataSuggestionDifferences, MetadataSuggestionsDifferencesRest> {

    @Autowired
    private ConverterService converterService;

    @Override
    public MetadataSuggestionsDifferencesRest convert(MetadataSuggestionDifferences modelObject,
                                                      Projection projection) {
        MetadataSuggestionsDifferencesRest metadataSuggestionsDifferencesRest =
            new MetadataSuggestionsDifferencesRest();
        Map<String, MetadataDifferenceRest> differences = new HashMap<>();
        for (Map.Entry<String, MetadataSuggestionDifference> entry : modelObject.getDifferences().entrySet()) {
            differences.put(entry.getKey(), converterService.toRest(entry.getValue(), Projection.DEFAULT));
        }
        metadataSuggestionsDifferencesRest.setId(modelObject.getId());
        metadataSuggestionsDifferencesRest.setDifferences(differences);
        metadataSuggestionsDifferencesRest.setMetadataSuggestion(modelObject.getSuggestionName());
        InProgressSubmission inProgressSubmission = modelObject.getInProgressSubmission();
        if (inProgressSubmission instanceof WorkflowItem) {
            metadataSuggestionsDifferencesRest.setWorkflowItemId(((WorkflowItem) inProgressSubmission).getID());
        } else {
            metadataSuggestionsDifferencesRest.setWorkspaceItemId(((WorkspaceItem) inProgressSubmission).getID());
        }
        return metadataSuggestionsDifferencesRest;
    }

    @Override
    public Class<MetadataSuggestionDifferences> getModelClass() {
        return MetadataSuggestionDifferences.class;
    }
}
