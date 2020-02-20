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
import org.dspace.xmlworkflow.state.Workflow;

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


    /**
     * Retrieve the workflow configuration for a single collection
     *
     * @param collection the collection for which we want our workflow
     * @return the workflow configuration
     * @throws WorkflowConfigurationException occurs if there is a configuration error in the workflow
     */
    public Workflow getWorkflow(Collection collection) throws WorkflowConfigurationException;
}
