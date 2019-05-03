/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import java.util.Map;

import org.dspace.content.Collection;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Workflow;
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
 */
public class XmlWorkflowFactoryImpl implements XmlWorkflowFactory {

    public static final String LEGACY_WORKFLOW_NAME = "defaultWorkflow";

    private Map<String, Workflow> workflowMapping;

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
}