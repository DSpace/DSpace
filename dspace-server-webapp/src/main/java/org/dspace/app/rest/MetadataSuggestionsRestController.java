/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.MetadataSuggestionEntryRest;
import org.dspace.app.rest.model.MetadataSuggestionsSourceRest;
import org.dspace.app.rest.model.hateoas.MetadataChangeResource;
import org.dspace.app.rest.model.hateoas.MetadataSuggestionEntryResource;
import org.dspace.app.rest.model.hateoas.MetadataSuggestionsSourceResource;
import org.dspace.app.rest.repository.MetadataSuggestionsRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller deals with calls to the /api/integration/metadata-suggestions endpoint
 */
@RestController
@RequestMapping("/api/integration/metadata-suggestions")
public class MetadataSuggestionsRestController implements InitializingBean {

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private MetadataSuggestionsRestRepository metadataSuggestionsRestRepository;

    @Autowired
    private WorkflowItemService workflowItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    HalLinkService linkService;

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
            .register(this, Arrays.asList(new Link("/api/integration/metadata-suggestions", "metadata-suggestions")));
    }

    /**
     * This endpoint will return all the MetadataSuggestionsSources that are able to deal with the given workflow
     * or workspace item
     * @param pageable          The pageable object for this call
     * @param assembler         The assembler object used for this call
     * @param workspaceItemId   The given workspaceItem id
     * @param workflowItemId    The given workflowItem id
     * @param response          The relevant response
     * @param request           The relevant request
     * @return                  A paginated list of MetadataSuggestionsSources that adhere to the given parameters
     * @throws SQLException     If something goes wrong
     */
    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<MetadataSuggestionsSourceResource> getMetadataSuggestions(Pageable pageable,
        PagedResourcesAssembler assembler, @RequestParam(name = "workspaceitem", required = false)
        Integer workspaceItemId, @RequestParam(name = "workflowitem", required = false) Integer workflowItemId,
        HttpServletResponse response, HttpServletRequest request) throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        InProgressSubmission inProgressSubmission = resolveInProgressSubmission(workspaceItemId, workflowItemId,
                                                                                context);
        Page<MetadataSuggestionsSourceRest> metadataSuggestionsSourceRestPage = metadataSuggestionsRestRepository
            .getAllMetadataSuggestionSources(pageable, inProgressSubmission);
        Page<MetadataSuggestionsSourceResource> metadataSuggestionsSourceResources =
            metadataSuggestionsSourceRestPage.map(metadataSuggestionsSourceRest ->
              new MetadataSuggestionsSourceResource(metadataSuggestionsSourceRest));
        metadataSuggestionsSourceResources.forEach(linkService::addLinks);
        PagedResources<MetadataSuggestionsSourceResource> result = assembler
            .toResource(metadataSuggestionsSourceResources);
        return result;

    }

    private InProgressSubmission resolveInProgressSubmission(Integer workspaceItemId, Integer workflowItemId,
        Context context) throws SQLException {
        InProgressSubmission inProgressSubmission = null;
        if (workflowItemId != null) {
            inProgressSubmission = workflowItemService.find(context, workflowItemId);
        } else if (workspaceItemId != null) {
            inProgressSubmission = workspaceItemService.find(context, workspaceItemId);
        }
        return inProgressSubmission;
    }

    /**
     * This endpoint will return a single MetadataSuggestionsSource that adheres to the given suggestionName
     * @param suggestionName    The given suggestionName String on which the MetadataSuggestionSource will be matched
     * @return                  The proper MetadataSuggestionsSource
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{suggestionName}")
    public MetadataSuggestionsSourceResource getMetadataSuggestion(
        @PathVariable("suggestionName") String suggestionName) {

        MetadataSuggestionsSourceRest metadataSuggestionsSourceRest = metadataSuggestionsRestRepository
            .getMetadataSuggestionSource(suggestionName);
        MetadataSuggestionsSourceResource metadataSuggestionsSourceResource =
            new MetadataSuggestionsSourceResource(metadataSuggestionsSourceRest);
        linkService.addLinks(metadataSuggestionsSourceResource);
        return metadataSuggestionsSourceResource;
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
     * @return                  The relevant MetadataSuggestionEntryResource
     * @throws SQLException     If something goes wrong
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{suggestionName}/entryValues/{entryId}")
    public MetadataSuggestionEntryResource getMetadataSuggestionEntry(
        @PathVariable("suggestionName") String suggestionName, @PathVariable("entryId") String entryId,
        @RequestParam(name = "workspaceitem", required = false) Integer workspaceItemId,
        @RequestParam(name = "workflowitem", required = false) Integer workflowItemId,
        HttpServletResponse response, HttpServletRequest request) throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        InProgressSubmission inProgressSubmission = resolveInProgressSubmission(workspaceItemId, workflowItemId,
                                                                                context);
        MetadataSuggestionEntryRest metadataSuggestionEntryRest = metadataSuggestionsRestRepository.getMetadataSuggestionEntry(suggestionName, entryId, inProgressSubmission);
        MetadataSuggestionEntryResource metadataSuggestionEntryResource = new MetadataSuggestionEntryResource(
            metadataSuggestionEntryRest);
        linkService.addLinks(metadataSuggestionEntryResource);
        return metadataSuggestionEntryResource;
    }


    //TODO implement method. This is already here for the linkFactory purposes
    @RequestMapping(method = RequestMethod.GET, value = "/{suggestionName}/entryValues/{entryId}/changes")
    public MetadataChangeResource getMetadataSuggestionEntryChanges(
        @PathVariable("suggestionName") String suggestionName, @PathVariable("entryId") String entryId,
        @RequestParam(name = "workspaceitem", required = false) Integer workspaceItemId,
        @RequestParam(name = "workflowitem", required = false) Integer workflowItemId,
        HttpServletResponse response, HttpServletRequest request) throws SQLException {

        throw new RepositoryMethodNotImplementedException("", "Method not yet implemented");
    }
}
