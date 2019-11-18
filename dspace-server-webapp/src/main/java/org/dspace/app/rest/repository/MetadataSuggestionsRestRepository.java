/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;
import java.util.Optional;

import org.dspace.app.rest.converter.MetadataSuggestionEntryConverter;
import org.dspace.app.rest.converter.MetadataSuggestionsSourceRestConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.MetadataSuggestionEntryRest;
import org.dspace.app.rest.model.MetadataSuggestionsSourceRest;
import org.dspace.content.InProgressSubmission;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;
import org.dspace.external.provider.metadata.service.MetadataSuggestionProviderService;
import org.dspace.external.provider.metadata.service.impl.MetadataItemSuggestions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is a repository dealing with MetadataSuggestion objects and its methods
 */
@Component(MetadataSuggestionsSourceRest.CATEGORY + "." + MetadataSuggestionsSourceRest.NAME)
public class MetadataSuggestionsRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    private MetadataSuggestionProviderService metadataSuggestionProviderService;

    @Autowired
    private MetadataSuggestionEntryConverter metadataSuggestionEntryConverter;

    @Autowired
    private MetadataSuggestionsSourceRestConverter metadataSuggestionsSourceRestConverter;

    /**
     * This method will create a page of MetadataSuggestionsSources that adheres to the given parameters
     * @param pageable              The pageable object for this request
     * @param inProgressSubmission  The InProgressionSubmission object for this call
     * @return                      A page containing MetadataSuggestionsSources
     */
    public Page<MetadataSuggestionsSourceRest> getAllMetadataSuggestionSources(Pageable pageable,
        InProgressSubmission inProgressSubmission) {

        if (inProgressSubmission == null) {
            throw new DSpaceBadRequestException("A valid workflowItem or workspaceItem needs to be passed along in" +
                                                    " the request parameters");
        }
        List<MetadataSuggestionProvider> metadataSuggestionProviders = metadataSuggestionProviderService
            .getMetadataSuggestionProviders(inProgressSubmission);
        Page<MetadataSuggestionsSourceRest> page = utils.getPage(metadataSuggestionProviders, pageable)
                                                        .map(metadataSuggestionsSourceRestConverter);

        return page;
    }

    /**
     * This method will return a single MetadataSuggestionsSource with the id being equal to the given id
     * @param id    The given id
     * @return      The MetadataSuggestionsSource that has the same id as the given id
     */
    public MetadataSuggestionsSourceRest getMetadataSuggestionSource(String id) {
        MetadataSuggestionProvider metadataSuggestionProvider = metadataSuggestionProviderService
            .getMetadataSuggestionProvider(id);
        if (metadataSuggestionProvider == null) {
            throw new ResourceNotFoundException("MetadataSuggestionProvider for: " + id + " couldn't be found");
        }
        return metadataSuggestionsSourceRestConverter.fromModel(metadataSuggestionProvider);
    }

    /**
     * This method constructs a {@link MetadataSuggestionEntryRest} object based on the given parameter
     * @param suggestionName    The name of the MetadataSuggestionProvider to be used
     * @param entryId           The ID of the entry to be looked up in the relevant MetadataSuggestionProvider
     * @param inProgressSubmission  The InProgressSubmission to be used
     * @return
     */
    public MetadataSuggestionEntryRest getMetadataSuggestionEntry(String suggestionName, String entryId, InProgressSubmission inProgressSubmission) {
        //TODO Save Workspaceitem/workflowitemid in rest object so we can use it in the linkfactory
        MetadataItemSuggestions metadataItemSuggestions = metadataSuggestionProviderService.getMetadataItemSuggestions(suggestionName, entryId, inProgressSubmission);
        return metadataSuggestionEntryConverter.fromModel(metadataItemSuggestions);
    }
}
