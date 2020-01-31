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
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles the config for workflow definitions
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@RestController
@RequestMapping("/api/" + WorkflowDefinitionRest.CATEGORY + "/" + WorkflowDefinitionRest.NAME_PLURAL)
public class WorkflowDefinitionController {

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
    @RequestMapping(method = RequestMethod.GET, value = "/{workflowName}/collections")
    public Page<CollectionRest> getCollections(HttpServletRequest request, @PathVariable String workflowName,
                                               Pageable pageable) {
        if (xmlWorkflowFactory.workflowByThisNameExists(workflowName)) {
            Context context = ContextUtil.obtainContext(request);
            List<Collection> collectionsMappedToWorkflow = new ArrayList<>();
            if (xmlWorkflowFactory.isDefaultWorkflow(workflowName)) {
                collectionsMappedToWorkflow.addAll(xmlWorkflowFactory.getAllNonMappedCollectionsHandles(context));
            }
            collectionsMappedToWorkflow.addAll(xmlWorkflowFactory.getCollectionHandlesMappedToWorklow(context,
                workflowName));
            return converter.toRestPage(utils.getPage(collectionsMappedToWorkflow, pageable),
                utils.obtainProjection(true));
        } else {
            throw new ResourceNotFoundException("No workflow with name " + workflowName + " is configured");
        }
    }

}
