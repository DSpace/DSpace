/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "collections" subresource of an individual workflow definition.
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@Component(WorkflowDefinitionRest.CATEGORY + "." + WorkflowDefinitionRest.NAME + "."
    + WorkflowDefinitionRest.COLLECTIONS_MAPPED_TO)
public class WorkflowDefinitionCollectionsLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    protected XmlWorkflowFactory xmlWorkflowFactory;

    @Autowired
    protected ConverterService converter;

    @Autowired
    protected Utils utils;

    /**
     * GET endpoint that returns the list of collections that make an explicit use of the workflow-definition.
     * If a collection doesn't specify the workflow-definition to be used, the default mapping applies,
     * but this collection is not included in the list returned by this method.
     *
     * @param request      The request object
     * @param workflowName Name of workflow we want the collections of that are mapped to is
     * @return List of collections mapped to the requested workflow
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<CollectionRest> getCollections(@Nullable HttpServletRequest request,
                                               String workflowName,
                                               @Nullable Pageable optionalPageable,
                                               Projection projection) {
        if (xmlWorkflowFactory.workflowByThisNameExists(workflowName)) {
            Context context = obtainContext();
            List<Collection> collectionsMappedToWorkflow = new ArrayList<>();
            if (xmlWorkflowFactory.isDefaultWorkflow(workflowName)) {
                collectionsMappedToWorkflow.addAll(xmlWorkflowFactory.getAllNonMappedCollectionsHandles(context));
            }
            collectionsMappedToWorkflow.addAll(xmlWorkflowFactory.getCollectionHandlesMappedToWorklow(context,
                workflowName));
            Pageable pageable = optionalPageable != null ? optionalPageable : new PageRequest(0, 20);
            return converter.toRestPage(utils.getPage(collectionsMappedToWorkflow, pageable),
                projection);
        } else {
            throw new ResourceNotFoundException("No workflow with name " + workflowName + " is configured");
        }
    }

}
