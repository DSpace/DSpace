/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.factory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;

/**
 * The xmlworkflowfactory is responsible for parsing the
 * workflow xml file and is used to retrieve the workflow for
 * a certain collection
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
public interface XmlWorkflowFactory {

    public final String LEGACY_WORKFLOW_NAME = "default";

    public Workflow getWorkflow(Collection collection) throws IOException, WorkflowConfigurationException, SQLException;

    public Workflow getWorkflowByName(String workflowName) throws WorkflowConfigurationException;

    public List<Workflow> getAllConfiguredWorkflows() throws WorkflowConfigurationException;

    public List<String> getCollectionHandlesMappedToWorklow(String workflowName) throws WorkflowConfigurationException;

    public Step createStep(Workflow workflow, String stepID) throws WorkflowConfigurationException, IOException;

    public WorkflowActionConfig createWorkflowActionConfig(String actionID);
}
