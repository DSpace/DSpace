/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.MetadataSuggestionEntryRest;
import org.dspace.app.rest.model.MetadataSuggestionsSourceRest;
import org.dspace.app.rest.model.hateoas.MetadataChangeResource;
import org.dspace.app.rest.model.hateoas.MetadataSuggestionEntryResource;
import org.dspace.app.rest.repository.MetadataSuggestionsRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
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
    "/{suggestionName}/entryValues/{entryId}")
public class MetadataSuggestionsRestController {

    @Autowired
    private MetadataSuggestionsRestRepository metadataSuggestionsRestRepository;

    @Autowired
    HalLinkService linkService;

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
    @RequestMapping(method = RequestMethod.GET)
    public MetadataSuggestionEntryResource getMetadataSuggestionEntry(
        @PathVariable("suggestionName") String suggestionName, @PathVariable("entryId") String entryId,
        @RequestParam(name = "workspaceitem", required = false) Integer workspaceItemId,
        @RequestParam(name = "workflowitem", required = false) Integer workflowItemId,
        HttpServletResponse response, HttpServletRequest request) {

        Context context = ContextUtil.obtainContext(request);
        InProgressSubmission inProgressSubmission = metadataSuggestionsRestRepository
            .resolveInProgressSubmission(workspaceItemId, workflowItemId, context);
        if (inProgressSubmission == null) {
            throw new ResourceNotFoundException("The InProgressSubmission for the given workspace or workflow ID was " +
                                                    "not found");
        }
        MetadataSuggestionEntryRest metadataSuggestionEntryRest = metadataSuggestionsRestRepository
            .getMetadataSuggestionEntry(suggestionName, entryId, inProgressSubmission);
        MetadataSuggestionEntryResource metadataSuggestionEntryResource = new MetadataSuggestionEntryResource(
            metadataSuggestionEntryRest);
        linkService.addLinks(metadataSuggestionEntryResource);
        return metadataSuggestionEntryResource;
    }


    @RequestMapping(method = RequestMethod.GET, value = "/changes")
    public MetadataChangeResource getMetadataSuggestionEntryChanges(
        @PathVariable("suggestionName") String suggestionName, @PathVariable("entryId") String entryId,
        @RequestParam(name = "workspaceitem", required = false) Integer workspaceItemId,
        @RequestParam(name = "workflowitem", required = false) Integer workflowItemId,
        HttpServletResponse response, HttpServletRequest request) throws SQLException {

        throw new RepositoryMethodNotImplementedException("", "Method not yet implemented");
    }
}
