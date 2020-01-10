/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.factory;

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;

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
public interface XmlWorkflowFactory {

    /**
     * Retrieve the workflow configuration for a single collection
     *
     * @param collection the collection for which we want our workflow
     * @return the workflow configuration
     * @throws WorkflowConfigurationException occurs if there is a configuration error in the workflow
     */
    public Workflow getWorkflow(Collection collection) throws WorkflowConfigurationException;

    /**
     * Retrieves the workflow configuration by name
     *
     * @param workflowName the name for which we want our workflow
     * @return the workflow configuration
     * @throws WorkflowConfigurationException occurs if there is no workflow configured by that name
     */
    public Workflow getWorkflowByName(String workflowName) throws WorkflowConfigurationException;

    /**
     * Creates a list of all configured workflows, or returns the cache of this if it was already created
     *
     * @return List of all configured workflows
     */
    public List<Workflow> getAllConfiguredWorkflows();

    /**
     * Check to see if there is a workflow configured by the given name
     *
     * @param workflowName Name of a possible configured workflow
     * @return True if there is a workflow configured by this name, false otherwise
     */
    public boolean workflowByThisNameExists(String workflowName);

    /**
     * Check to see if the given workflowName is the workflow configured to be default for collections
     *
     * @param workflowName Name of workflow to check if default
     * @return True if given workflowName is the workflow mapped to default for collections, otherwise false
     */
    public boolean isDefaultWorkflow(String workflowName);

    /**
     * Gets the default workflow, i.e. the workflow that is mapped to collection=default in workflow.xml
     */
    public Workflow getDefaultWorkflow();

    /**
     * Return a list of collections that are mapped to the given workflow in the workflow configuration.
     * * Makes use of a cache so it only retrieves the workflowName->List<collectionHandle> if it's not cached
     *
     * @param context      Dspace context
     * @param workflowName Name of workflow we want the collections of that are mapped to is
     * @return List of collections mapped to the requested workflow
     */
    public List<Collection> getCollectionHandlesMappedToWorklow(Context context, String workflowName);

    /**
     * Returns list of collections that are not mapped to any configured workflow, and thus use the default workflow
     *
     * @return List of collections not mapped to any workflow
     */
    public List<Collection> getAllNonMappedCollectionsHandles(Context context);

    /**
     * Retrieves a {@link WorkflowActionConfig} object based on its name, should correspond with bean id in workflow-actions.xml
     *
     * @param workflowActionName Name of workflow action we want to retrieve
     * @return Workflow action object corresponding to the given workflowActionName
     */
    public WorkflowActionConfig getActionByName(String workflowActionName);

    /**
     * Retrieves a {@link Step} object based on its name, should correspond with bean id in workflow.xml
     *
     * @param workflowStepName Name of workflow step we want to retrieve
     * @return Workflow step object corresponding to the given workflowStepName
     */
    public Step getStepByName(String workflowStepName);
}
