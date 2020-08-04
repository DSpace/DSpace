/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * The workflowfactory is responsible for parsing the workflow xml file and is used to retrieve info about the workflow:
 * - the workflow for a certain collection
 * - collections mapped to a certain workflow
 * - collections not mapped to any workflow
 * - configured workflows and the default workflow
 * - workflow action by name
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
public class XmlWorkflowFactoryImpl implements XmlWorkflowFactory {

    public static final String LEGACY_WORKFLOW_NAME = "defaultWorkflow";

    private Logger log = org.apache.logging.log4j.LogManager.getLogger(XmlWorkflowFactoryImpl.class);

    private Map<String, Workflow> workflowMapping;

    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected HandleService handleService;

    @Override
    public Workflow getWorkflow(Collection collection) throws WorkflowConfigurationException {
        // Attempt to retrieve our workflow object
        if (workflowMapping.get(collection.getHandle()) == null) {
            final Workflow defaultWorkflow = workflowMapping.get(LEGACY_WORKFLOW_NAME);
            if (defaultWorkflow != null) {
                return defaultWorkflow;
            }
        } else {
            return workflowMapping.get(collection.getHandle());
        }

        throw new WorkflowConfigurationException(
                "Error while retrieving workflow for the following collection: " + collection.getHandle());
    }

    @Required
    public void setWorkflowMapping(Map<String, Workflow> workflowMapping) {
        this.workflowMapping = workflowMapping;
    }

    @Override
    public Workflow getWorkflowByName(String workflowName) throws WorkflowConfigurationException {
        for (Workflow workflow : workflowMapping.values()) {
            if (workflow.getID().equals(workflowName)) {
                return workflow;
            }
        }
        throw new WorkflowConfigurationException(
                "Error while retrieving workflow by the following name: " + workflowName);
    }

    @Override
    public Workflow getDefaultWorkflow() {
        return this.workflowMapping.get(LEGACY_WORKFLOW_NAME);
    }

    @Override
    public List<Workflow> getAllConfiguredWorkflows() {
        return new ArrayList<>(this.workflowMapping.values());
    }

    @Override
    public List<Collection> getCollectionHandlesMappedToWorklow(Context context, String workflowName) {
        List<Collection> collectionsMapped = new ArrayList<>();
        for (String handle : this.workflowMapping.keySet()) {
            if (this.workflowMapping.get(handle).getID().equals(workflowName)) {
                try {
                    Collection collection = (Collection) handleService.resolveToObject(context, handle);
                    if (collection != null) {
                        collectionsMapped.add(collection);
                    }
                } catch (SQLException e) {
                    log.error("SQLException in XmlWorkflowFactoryImpl.getCollectionHandlesMappedToWorklow trying to " +
                            "retrieve collection with handle: " + handle, e);
                }
            }
        }
        return collectionsMapped;
    }

    @Override
    public List<Collection> getAllNonMappedCollectionsHandles(Context context) {
        List<Collection> nonMappedCollections = new ArrayList<>();
        try {
            for (Collection collection : this.collectionService.findAll(context)) {
                if (workflowMapping.get(collection.getHandle()) == null) {
                    nonMappedCollections.add(collection);
                }
            }
        } catch (SQLException e) {
            log.error("SQLException in XmlWorkflowFactoryImpl.getAllNonMappedCollectionsHandles trying to " +
                    "retrieve all collections", e);
        }
        return nonMappedCollections;
    }

    @Override
    public boolean workflowByThisNameExists(String workflowName) {
        for (Workflow workflow : this.workflowMapping.values()) {
            if (workflow.getID().equals(workflowName)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean isDefaultWorkflow(String workflowName) {
        if (StringUtils.isNotBlank(workflowName)) {
            Workflow defaultWorkflow = this.getDefaultWorkflow();
            if (defaultWorkflow != null && StringUtils.isNotBlank(defaultWorkflow.getID())) {
                return (defaultWorkflow.getID().equals(workflowName));
            }
        }
        return false;
    }

    @Override
    public WorkflowActionConfig getActionByName(String workflowActionName) {
        return new DSpace().getServiceManager().getServiceByName(workflowActionName, WorkflowActionConfig.class);
    }

    @Override
    public Step getStepByName(String workflowStepName) {
        return new DSpace().getServiceManager().getServiceByName(workflowStepName, Step.class);
    }

}
