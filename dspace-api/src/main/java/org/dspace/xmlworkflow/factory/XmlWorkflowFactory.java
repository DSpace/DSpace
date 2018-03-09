/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.factory;

import org.dspace.content.Collection;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;

import java.io.IOException;
import java.sql.SQLException;

/**
 * The xmlworkflowfactory is responsible for parsing the
 * workflow xml file and is used to retrieve the workflow for
 * a certain collection
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface XmlWorkflowFactory {

    public Workflow getWorkflow(Collection collection) throws IOException, WorkflowConfigurationException, SQLException;

    public Step createStep(Workflow workflow, String stepID) throws WorkflowConfigurationException, IOException;

    public WorkflowActionConfig createWorkflowActionConfig(String actionID);
}
