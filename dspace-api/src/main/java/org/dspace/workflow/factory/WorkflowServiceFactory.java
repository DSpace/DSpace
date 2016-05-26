/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;

/**
 * Abstract factory to get services for the workflow package, use WorkflowServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class WorkflowServiceFactory {

    public abstract WorkflowService getWorkflowService();

    public abstract WorkflowItemService getWorkflowItemService();

    public static WorkflowServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("workflowServiceFactory", WorkflowServiceFactory.class);
    }
}
