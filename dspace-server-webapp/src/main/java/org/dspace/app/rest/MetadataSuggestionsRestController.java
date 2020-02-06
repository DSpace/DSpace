/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.MetadataSuggestionEntryRest;
import org.dspace.app.rest.model.MetadataSuggestionsDifferencesRest;
import org.dspace.app.rest.model.MetadataSuggestionsSourceRest;
import org.dspace.app.rest.model.hateoas.MetadataChangeResource;
import org.dspace.app.rest.model.hateoas.MetadataSuggestionEntryResource;
import org.dspace.app.rest.model.hateoas.MetadataSuggestionsDifferencesResource;
import org.dspace.app.rest.repository.MetadataSuggestionsRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller deals with calls to the /api/integration/metadata-suggestions endpoint
 */
@RestController
@RequestMapping("/api/" + MetadataSuggestionsSourceRest.INTEGRATION + "/" + MetadataSuggestionsSourceRest.PLURAL_NAME +
    "/{suggestionName}")
public class MetadataSuggestionsRestController {

    @Autowired
    private MetadataSuggestionsRestRepository metadataSuggestionsRestRepository;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    HalLinkService linkService;

    @Autowired
    private AuthorizeService authorizeService;

    /**
     * This endpoint will retrieve all MetadataSuggestionEntries based on the given parameters. It'll query the
     * SuggestionProvider with the given SuggestionName with the proper parameters and provide a paged response
     *
     * @param pageable          The pageable for this request
     * @param response          The HttpServletResponse
     * @param request           The HttpServletRequest
     * @param suggestionName    The name of the suggestionProvider
     * @param query             The query that'll be searched for
     * @param bitstreamUuid     The UUID of the bitstream that will be used by the provider
     * @param useMetadata       The boolean indicating whether we use metadata or not
     * @param workspaceItemId   The ID of the workspaceItem
     * @param workflowItemId    The ID of the workflowItem
     * @param assembler         The Assembler to create the PagedResources
     * @return                  The PagedResources containing all the MetadataSuggestionEntryResources
     * @throws SQLException     If something goes wrong
     */
    @RequestMapping(method = RequestMethod.GET, value = "/entries")
    public PagedResources<MetadataSuggestionEntryResource> getMetadataSugggestionEntries(Pageable pageable,
        HttpServletResponse response, HttpServletRequest request,
        @PathVariable("suggestionName") String suggestionName,
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "bitstream", required = false) UUID bitstreamUuid,
        @RequestParam(name = "use-metadata", required = false, defaultValue = "false") Boolean useMetadata,
        @RequestParam(name = "workspaceitem", required = false) Integer workspaceItemId,
        @RequestParam(name = "workflowitem", required = false) Integer workflowItemId,
        PagedResourcesAssembler assembler) throws SQLException {

        int count = countParameters(query, bitstreamUuid, useMetadata);
        if (count > 1) {
            throw new DSpaceBadRequestException("Can only provide one of the following parameters :" +
                                                    " query, bitstream, use-metadata");
        }
        if (workflowItemId == null && workspaceItemId == null) {
            throw new DSpaceBadRequestException("You need to provide either a workflowitem ID or a workspaceitem ID");
        }
        Context context = ContextUtil.obtainContext(request);
        InProgressSubmission inProgressSubmission = metadataSuggestionsRestRepository
            .resolveInProgressSubmission(workspaceItemId, workflowItemId, context);
        if (inProgressSubmission == null) {
            throw new DSpaceBadRequestException("A valid InProgressSubmission couldn't be found for the given" +
                                                    " parameters");
        }
        Bitstream bitstream = null;
        if (bitstreamUuid != null) {
            bitstream = bitstreamService.find(context, bitstreamUuid);
            if (!isBitstreamValid(bitstream, inProgressSubmission)) {
                throw new DSpaceBadRequestException("The given Bitstream UUID couldn't be resolved to a Bitstream" +
                                                        " within the item for the InProgressSubmission provided");
            }
        }

        Page<MetadataSuggestionEntryRest> page =
            metadataSuggestionsRestRepository.getMetadataSuggestionEntries(suggestionName, inProgressSubmission,
                                                                           query, bitstream, useMetadata, pageable);

        Page<MetadataSuggestionEntryResource> metadataSuggestionEntryResources = page
            .map(metadataSuggestionEntryRest -> new MetadataSuggestionEntryResource(metadataSuggestionEntryRest));
        metadataSuggestionEntryResources.forEach(linkService::addLinks);
        PagedResources<MetadataSuggestionEntryResource> result = assembler.toResource(metadataSuggestionEntryResources);
        return result;


    }

    /**
     * This method counts the number of active parameters
     * @param query             The query
     * @param bitstreamUuid     The bitstreamUuuid
     * @param useMetadata       The use-metadata boolean
     * @return  The amount of parameters given
     */
    private int countParameters(String query, UUID bitstreamUuid, boolean useMetadata) {
        int count = 0;
        if (StringUtils.isNotBlank(query)) {
            count++;
        }
        if (bitstreamUuid != null) {
            count++;
        }
        if (useMetadata) {
            count++;
        }
        return count;
    }

    /**
     * Verifies that the given Bitstream is present in the InProgressSubmission
     * @param bitstream             The given Bitstream object
     * @param inProgressSubmission  The InProgressSubmission to check
     * @return                      A boolean indicating whether the Bitstream is present within the
     *                              InProgressSubmission or not
     * @throws SQLException         If something goes wrong
     */
    private boolean isBitstreamValid(Bitstream bitstream, InProgressSubmission inProgressSubmission)
        throws SQLException {
        for (Bundle bundle : inProgressSubmission.getItem().getBundles()) {
            for (Bitstream b : bundle.getBitstreams()) {
                if (b.equals(bitstream)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This endpoint will return a {@link MetadataSuggestionEntryResource} object based on the given path values
     * and parameters
     * @param suggestionName    The name of the MetadataSuggestionProvider to be used
     * @param entryId           The ID of the entry to be looked up in the relevant MetadataSuggestionProvider
     * @param workspaceItemId   The ID of the Workspace item to be used if present
     * @param workflowItemId    The ID of the Workflow item to be used if present
     * @param response          The HttpServletResponse
     * @param request           The HttpServletRequest
     * @return The relevant MetadataSuggestionEntryResource
     * @throws SQLException     If something goes wrong
     */
    @RequestMapping(method = RequestMethod.GET, value = "/entryValues/{entryId}")
    public MetadataSuggestionEntryResource getMetadataSuggestionEntry(
        @PathVariable("suggestionName") String suggestionName, @PathVariable("entryId") String entryId,
        @RequestParam(name = "workspaceitem", required = false) Integer workspaceItemId,
        @RequestParam(name = "workflowitem", required = false) Integer workflowItemId,
        HttpServletResponse response, HttpServletRequest request) throws SQLException {

        InProgressSubmission inProgressSubmission = getInProgressSubmission(workspaceItemId, workflowItemId, request);
        MetadataSuggestionEntryRest metadataSuggestionEntryRest = metadataSuggestionsRestRepository
            .getMetadataSuggestionEntry(suggestionName, entryId, inProgressSubmission);
        MetadataSuggestionEntryResource metadataSuggestionEntryResource = new MetadataSuggestionEntryResource(
            metadataSuggestionEntryRest);
        linkService.addLinks(metadataSuggestionEntryResource);
        return metadataSuggestionEntryResource;
    }

    private InProgressSubmission getInProgressSubmission(Integer workspaceItemId, Integer workflowItemId,
        HttpServletRequest request) throws SQLException {
        if (workflowItemId == null && workspaceItemId == null) {
            throw new DSpaceBadRequestException("You need to provide either a workflowitem ID or a workspaceitem ID");
        }
        Context context = ContextUtil.obtainContext(request);
        InProgressSubmission inProgressSubmission = metadataSuggestionsRestRepository
            .resolveInProgressSubmission(workspaceItemId, workflowItemId, context);
        if (inProgressSubmission == null) {
            throw new ResourceNotFoundException("The InProgressSubmission for the given workspace or workflow ID was " +
                                                    "not found");
        }
        if (!authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), inProgressSubmission.getItem(),
                                                     Constants.WRITE, false)) {
            throw new AccessDeniedException("You do not have write rights on this InProgressSubmission for id: " +
                                                inProgressSubmission.getID());
        }
        return inProgressSubmission;
    }


    @RequestMapping(method = RequestMethod.GET, value = "/entryValues/{entryId}/changes")
    public MetadataChangeResource getMetadataSuggestionEntryChanges(
        @PathVariable("suggestionName") String suggestionName, @PathVariable("entryId") String entryId,
        @RequestParam(name = "workspaceitem", required = false) Integer workspaceItemId,
        @RequestParam(name = "workflowitem", required = false) Integer workflowItemId,
        HttpServletResponse response, HttpServletRequest request) throws SQLException {

        throw new RepositoryMethodNotImplementedException("", "Method not yet implemented");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/entryValueDifferences/{entryId}")
    public MetadataSuggestionsDifferencesResource getMetadataSuggestionEntryDifferences(
        @PathVariable("suggestionName") String suggestionName, @PathVariable("entryId") String entryId,
        @RequestParam(name = "workspaceitem", required = false) Integer workspaceitem,
        @RequestParam(name = "workflowitem", required = false) Integer workflowitem,
        HttpServletResponse response, HttpServletRequest request) throws SQLException {

        InProgressSubmission inProgressSubmission = getInProgressSubmission(workspaceitem, workflowitem, request);
        MetadataSuggestionsDifferencesRest metadataSuggestionsDifferencesRest = metadataSuggestionsRestRepository
            .getMetadataSuggestionsDifferences(suggestionName, entryId, inProgressSubmission);

        MetadataSuggestionsDifferencesResource metadataSuggestionsDifferencesResource =
            new MetadataSuggestionsDifferencesResource(metadataSuggestionsDifferencesRest);
        linkService.addLinks(metadataSuggestionsDifferencesResource);
        return metadataSuggestionsDifferencesResource;
    }
}
