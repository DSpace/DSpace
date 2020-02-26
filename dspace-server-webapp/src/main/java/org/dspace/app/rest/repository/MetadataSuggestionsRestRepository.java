/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.MetadataSuggestionEntryRest;
import org.dspace.app.rest.model.MetadataSuggestionsDifferencesRest;
import org.dspace.app.rest.model.MetadataSuggestionsSourceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;
import org.dspace.external.provider.metadata.service.MetadataSuggestionProviderService;
import org.dspace.external.provider.metadata.service.impl.MetadataItemSuggestions;
import org.dspace.external.provider.metadata.service.impl.MetadataSuggestionDifferences;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * This is a repository dealing with MetadataSuggestion objects and its methods
 */
@Component(MetadataSuggestionsSourceRest.CATEGORY + "." + MetadataSuggestionsSourceRest.NAME)
public class MetadataSuggestionsRestRepository extends DSpaceRestRepository<MetadataSuggestionsSourceRest, String> {

    private static final Logger log = Logger.getLogger(ItemRestRepository.class);

    @Autowired
    private MetadataSuggestionProviderService metadataSuggestionProviderService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private WorkflowItemService workflowItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private Utils utils;

    /**
     * This method will create a page of MetadataSuggestionsSources that adheres to the given parameters
     * @param pageable              The pageable object for this request
     * @param inProgressSubmission  The InProgressionSubmission object for this call
     * @return A page containing MetadataSuggestionsSources
     */
    public Page<MetadataSuggestionsSourceRest> getAllMetadataSuggestionSources(Pageable pageable,
                                                                               InProgressSubmission
                                                                                   inProgressSubmission) {

        List<MetadataSuggestionProvider> metadataSuggestionProviders = metadataSuggestionProviderService
            .getMetadataSuggestionProviders(inProgressSubmission);
        return converter.toRestPage(utils.getPage(metadataSuggestionProviders, pageable), Projection.DEFAULT);
    }

    /**
     * This method constructs a {@link MetadataSuggestionEntryRest} object based on the given parameter
     * @param suggestionName    The name of the MetadataSuggestionProvider to be used
     * @param entryId           The ID of the entry to be looked up in the relevant MetadataSuggestionProvider
     * @param inProgressSubmission  The InProgressSubmission to be used
     * @return
     */
    public MetadataSuggestionEntryRest getMetadataSuggestionEntry(String suggestionName, String entryId,
                                                                  InProgressSubmission inProgressSubmission) {
        Optional<MetadataItemSuggestions> metadataItemSuggestionsOptional = metadataSuggestionProviderService
            .getMetadataItemSuggestions(suggestionName, entryId, inProgressSubmission);
        if (metadataItemSuggestionsOptional.isPresent()) {
            return converter.toRest(metadataItemSuggestionsOptional.get(), Projection.DEFAULT);
        } else {
            throw new ResourceNotFoundException(
                "MetadataSuggestionEntry for suggestionName: " + suggestionName + " and entryId: " + entryId + " for " +
                    "the given inProgressSubmission could not be found");
        }
    }

    /**
     * This method constructs a MetadataSuggestionsDifferencesRest object from the given parameters
     * @param suggestionName        The suggestionName from where the information will be retrieved
     * @param entryId               The id of the entry for the given suggestionName
     * @param inProgressSubmission  The InProgressSubmission who's metadata will be checked
     * @return A MetadataSuggestionsDifferencesRest object that holds all the differences between the found values and
     * the values within the InProgressSubmission
     */
    public MetadataSuggestionsDifferencesRest getMetadataSuggestionsDifferences(String suggestionName, String entryId,
                                                                        InProgressSubmission inProgressSubmission) {
        Optional<MetadataSuggestionDifferences> metadataSuggestionDifferences = metadataSuggestionProviderService
            .getMetadataSuggestionDifferences(suggestionName, entryId, inProgressSubmission);
        if (metadataSuggestionDifferences.isPresent()) {
            return converter.toRest(metadataSuggestionDifferences.get(), Projection.DEFAULT);
        } else {
            throw new ResourceNotFoundException(
                "The MetadataSuggestionDifferences for suggestionName: " + suggestionName + " and entryId: " + entryId +
                    " for InProgressSubmission with id: " + inProgressSubmission.getID() + " could not be found");
        }
    }

    @Override
    public MetadataSuggestionsSourceRest findOne(Context context, String id) {
        Optional<MetadataSuggestionProvider> metadataSuggestionProviderOptional = metadataSuggestionProviderService
            .getMetadataSuggestionProvider(id);
        if (metadataSuggestionProviderOptional.isPresent()) {
            return converter.toRest(metadataSuggestionProviderOptional.get(), Projection.DEFAULT);
        } else {
            throw new ResourceNotFoundException("MetadataSuggestionProvider for: " + id + " couldn't be found");
        }
    }

    @Override
    public Page<MetadataSuggestionsSourceRest> findAll(Context context, Pageable pageable) {
        HttpServletRequest httpServletRequest = requestService.getCurrentRequest().getHttpServletRequest();
        String workspaceItemIdString = httpServletRequest.getParameter("workspaceitem");
        Integer workspaceItemId = null;
        if (StringUtils.isNotBlank(workspaceItemIdString)) {
            workspaceItemId = Integer.parseInt(workspaceItemIdString);
        }
        String workflowItemIdString = httpServletRequest.getParameter("workflowitem");
        Integer workflowItemId = null;
        if (StringUtils.isNotBlank(workflowItemIdString)) {
            workflowItemId = Integer.parseInt(workflowItemIdString);
        }
        InProgressSubmission inProgressSubmission = resolveInProgressSubmission(workspaceItemId, workflowItemId,
                                                                                context);
        if (inProgressSubmission == null) {
            throw new DSpaceBadRequestException("A valid workflowItem or workspaceItem needs to be passed along in" +
                                                    " the request parameters");
        }
        try {
            if (!authorizeService.authorizeActionBoolean(context, context.getCurrentUser(),
                                                         inProgressSubmission.getItem(), Constants.WRITE, false)) {
                throw new AccessDeniedException("You do not have write rights on this InProgressSubmission for id: " +
                                                    inProgressSubmission.getID());
            }
        } catch (SQLException e) {
            log.error("Looking up permissions for InProgressSubmission with ID: " + inProgressSubmission.getID() +
                          " threw an error", e);
        }
        Page<MetadataSuggestionsSourceRest> metadataSuggestionsSourceRestPage =
            getAllMetadataSuggestionSources(pageable, inProgressSubmission);
        return metadataSuggestionsSourceRestPage;
    }

    public InProgressSubmission resolveInProgressSubmission(Integer workspaceItemId, Integer workflowItemId,
                                                            Context context) {
        InProgressSubmission inProgressSubmission = null;
        try {
            if (workflowItemId != null) {
                inProgressSubmission = workflowItemService.find(context, workflowItemId);
            } else if (workspaceItemId != null) {
                inProgressSubmission = workspaceItemService.find(context, workspaceItemId);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return inProgressSubmission;
    }

    public Class<MetadataSuggestionsSourceRest> getDomainClass() {
        return MetadataSuggestionsSourceRest.class;
    }

    /**
     * This is the Repository method that will retrieve all the MetadataSuggestionEntries for the given Parameters
     * @param suggestionName        The name of the suggestionProvider to use
     * @param inProgressSubmission  The InProgressSubmission that will be used
     * @param query                 The query for the request
     * @param bitstream             The bitstream for the request
     * @param useMetadata           The boolean indicating whether we use metadata or not
     * @param pageable              The pageable
     * @return A page containing the MetadataSuggestionEntryRest objects resulting from the calls to the Provider
     */
    public Page<MetadataSuggestionEntryRest> getMetadataSuggestionEntries(String suggestionName,
                                                                          InProgressSubmission inProgressSubmission,
                                                                          String query,
                                                                          Bitstream bitstream,
                                                                          boolean useMetadata, Pageable pageable) {
        Optional<MetadataSuggestionProvider> metadataSuggestionProviderOptional = metadataSuggestionProviderService
            .getMetadataSuggestionProvider(suggestionName);
        MetadataSuggestionProvider metadataSuggestionProvider = null;
        if (metadataSuggestionProviderOptional.isPresent()) {
            metadataSuggestionProvider = metadataSuggestionProviderOptional.get();
        } else {
            throw new DSpaceBadRequestException(
                "The given SuggestionName could not be resolved to a MetadataSuggestionProvider");
        }
        if (!metadataSuggestionProvider.supports(inProgressSubmission, query, bitstream, useMetadata)) {
            throw new DSpaceBadRequestException(
                "The given MetadataSuggestionProvider doesn't support this InProgressSubmission");
        }

        List<MetadataItemSuggestions> list = metadataSuggestionProviderService
            .getMetadataItemSuggestions(metadataSuggestionProvider, inProgressSubmission, query, bitstream,
                                        useMetadata, Math.toIntExact(pageable.getOffset()), pageable.getPageSize());
        int total = metadataSuggestionProviderService.getTotalMetadataItemSuggestions(metadataSuggestionProvider,
                                                                                      inProgressSubmission, query,
                                                                                      bitstream, useMetadata);
        return converter.toRestPage(list, pageable, total, Projection.DEFAULT);
    }
}
