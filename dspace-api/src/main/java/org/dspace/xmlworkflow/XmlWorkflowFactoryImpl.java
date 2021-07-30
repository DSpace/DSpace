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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is injected with the external workflow configuration and is used to
 * retrieve information about the workflow:
 * <ul>
 *   <li>the workflow for a certain collection
 *   <li>collections mapped to a certain workflow
 *   <li>collections not mapped to any workflow
 *   <li>configured workflows and the default workflow
 *   <li>workflow action by name
 * </ul>
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Maria Verdonck (Atmire) on 11/12/2019
 * @author Mykhaylo Boychuk (4Science)
 */
public class XmlWorkflowFactoryImpl implements XmlWorkflowFactory {

    public static final String LEGACY_WORKFLOW_NAME = "defaultWorkflow";

    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(XmlWorkflowFactoryImpl.class);

    /** Map Handles into Workflows. */
    private Map<String, Workflow> workflowMapping;

    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected HandleService handleService;

    /*
     * Contains all the wolrkflows in the context
     */
    @Autowired
    private List<Workflow> workflows;

    @Override
    public Workflow getWorkflow(Collection collection) throws WorkflowConfigurationException {
        // Attempt to retrieve our workflow object
        String workflowName = collectionService.getMetadataFirstValue(collection, "cris", "workflow", "name", Item.ANY);
        if (workflowByThisNameExists(workflowName)) {
            return getWorkflowByName(workflowName);
        }
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

    /**
     * Inject the mapping from Collection Handle into Workflow.
     * @param workflowMapping map from Handle name to Workflow object.
     */
    @Autowired(required = true)
    public void setWorkflowMapping(Map<String, Workflow> workflowMapping) {
        this.workflowMapping = workflowMapping;
    }

    @Override
    public Workflow getWorkflowByName(String workflowName) throws WorkflowConfigurationException {
        for (Workflow workflow : this.workflows) {
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
        return this.workflows;
    }

    @Override
    public List<Collection> getCollectionHandlesMappedToWorkflow(Context context, String workflowName) {
        HashSet<Collection> collections = new HashSet<Collection>();
        for (String handle : this.workflowMapping.keySet()) {
            if (this.workflowMapping.get(handle).getID().equals(workflowName)) {
                try {
                    Collection collection = (Collection) handleService.resolveToObject(context, handle);
                    if (collection != null) {
                        collections.add(collection);
                    }
                } catch (SQLException e) {
                    log.error("SQLException in XmlWorkflowFactoryImpl.getCollectionHandlesMappedToWorkflow trying to " +
                            "retrieve collection with handle: " + handle, e);
                }
            }
        }
        if (workflowByThisNameExists(workflowName)) {
            addCollectionsWithWorkflowMetadata(context, workflowName, collections);
        }
        return new ArrayList<>(collections);
    }

    private void addCollectionsWithWorkflowMetadata(Context context, String workflowName,
            HashSet<Collection> collections) {
        try {
            for (Collection col : collectionService.findAll(context)) {
                String wfwName = collectionService.getMetadataFirstValue(col, "cris", "workflow", "name", Item.ANY);
                if (StringUtils.isNotBlank(wfwName)) {
                    if (wfwName.equals(workflowName)) {
                        collections.add(col);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("SQLException in XmlWorkflowFactoryImpl.getCollectionHandlesMappedToWorkflow trying to "
                    + "retrieve collections mapped to workwflow with name: " + workflowName, e);
        }
    }

    @Override
    public List<Collection> getAllNonMappedCollectionsHandles(Context context) {
        List<Collection> nonMappedCollections = new ArrayList<>();
        try {
            for (Collection collection : this.collectionService.findAll(context)) {
                String wfwName = collectionService.getMetadataFirstValue(collection, "cris", "workflow", "name", null);
                if (Objects.isNull(this.workflowMapping.get(collection.getHandle())) && StringUtils.isBlank(wfwName)) {
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
        for (Workflow workflow : this.workflows) {
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
