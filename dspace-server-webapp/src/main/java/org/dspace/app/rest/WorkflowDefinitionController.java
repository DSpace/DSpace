/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handles the config for workflow definitions
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@RestController
@RequestMapping("/api/" + WorkflowDefinitionRest.CATEGORY + "/" + WorkflowDefinitionRest.NAME_PLURAL)
public class WorkflowDefinitionController {

    protected XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    @Autowired
    protected ConverterService converter;

    @Autowired
    protected Utils utils;

    @Autowired
    private CollectionService collectionService;

    /**
     * GET endpoint that returns the workflow definition that applies to a specific collection eventually fallback
     * to the default configuration.
     *
     * @param request  The request object
     * @param response The response object
     * @param uuid     Uuid of the collection
     * @return the workflow definition for this collection
     */
    @GetMapping("/search/findByCollection")
    public WorkflowDefinitionRest get(HttpServletRequest request, HttpServletResponse response,
                                      @RequestParam(name = "uuid", required = true) UUID uuid)
            throws SQLException, IOException {
        try {
            Context context = ContextUtil.obtainContext(request);
            Collection collectionFromUuid = collectionService.find(context, uuid);
            return converter.toRest(xmlWorkflowFactory.getWorkflow(collectionFromUuid), utils.obtainProjection());
        } catch (WorkflowConfigurationException e) {
            // TODO ? Better exception?
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * GET endpoint that returns the list of collections that make an explicit use of the workflow-definition.
     * If a collection doesn't specify the workflow-definition to be used, the default mapping applies,
     * but this collection is not included in the list returned by this method.
     *
     * @param request      The request object
     * @param response     The response object
     * @param workflowName Name of workflow we want the collections of that are mapped to is
     * @return List of collections mapped to the requested workflow
     * @throws SQLException if db error
     */
    @GetMapping("{workflowName}/collections")
    public List<CollectionRest> get(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable String workflowName) throws SQLException {
        try {
            List<String> collectionsHandlesMappedToWorkflow
                    = xmlWorkflowFactory.getCollectionHandlesMappedToWorklow(workflowName);
            List<CollectionRest> collectionResourcesFromHandles = new ArrayList<>();
            for (String handle : collectionsHandlesMappedToWorkflow) {
                Context context = ContextUtil.obtainContext(request);
                Collection collection = (Collection) handleService.resolveToObject(context, handle);
                if (collection != null) {
                    collectionResourcesFromHandles.add(converter.toRest(collection, utils.obtainProjection()));
                }
            }
            return collectionResourcesFromHandles;
        } catch (WorkflowConfigurationException e) {
            // TODO ? Better exception?
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
