/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.dspace.workflowbasic.service.TaskListItemService;

/**
 * Abstract factory to get services for the workflowbasic package, use BasicWorkflowServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class BasicWorkflowServiceFactory extends WorkflowServiceFactory {

    public abstract BasicWorkflowService getBasicWorkflowService();

    public abstract BasicWorkflowItemService getBasicWorkflowItemService();

    public abstract TaskListItemService getTaskListItemService();

    public static BasicWorkflowServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("workflowServiceFactory", BasicWorkflowServiceFactory.class);
    }
}
