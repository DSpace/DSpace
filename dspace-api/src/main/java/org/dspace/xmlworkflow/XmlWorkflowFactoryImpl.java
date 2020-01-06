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
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * The workflowfactory is responsible for parsing the
 * workflow xml file and is used to retrieve the workflow for
 * a certain collection
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

    @Autowired(required = true)
    protected CollectionService collectionService;

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
    public List<String> getCollectionHandlesMappedToWorklow(String workflowName) {
        List<String> collectionsMapped = new ArrayList<>();
        for (String handle : this.workflowMapping.keySet()) {
            if (this.workflowMapping.get(handle).getID().equals(workflowName)) {
                collectionsMapped.add(handle);
            }
        }
        return collectionsMapped;
    }

    @Override
    public List<String> getAllNonMappedCollectionsHandles(Context context) {
        List<String> nonMappedCollectionHandles = new ArrayList<>();
        try {
            for (Collection collection : this.collectionService.findAll(context)) {
                if (workflowMapping.get(collection.getHandle()) == null) {
                    nonMappedCollectionHandles.add(collection.getHandle());
                }
            }
        } catch (SQLException e) {
            log.error("SQLException in XmlWorkflowFactoryImpl.getAllNonMappedCollectionsHandles trying to " +
                    "retrieve all collections");
        }
        return nonMappedCollectionHandles;
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
        WorkflowActionConfig actionConfig
                = new DSpace().getServiceManager().getServiceByName(workflowActionName, WorkflowActionConfig.class);
        if (actionConfig != null) {
            return actionConfig;
        }
        return null;
    }

}
