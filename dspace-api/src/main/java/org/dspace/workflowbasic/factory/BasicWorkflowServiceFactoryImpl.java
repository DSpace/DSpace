/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflowbasic.factory;

import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.dspace.workflowbasic.service.TaskListItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the workflowbasic package, use BasicWorkflowServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class BasicWorkflowServiceFactoryImpl extends BasicWorkflowServiceFactory {

    @Autowired(required = true)
    private BasicWorkflowService basicWorkflowService;
    @Autowired(required = true)
    private BasicWorkflowItemService basicWorkflowItemService;
    @Autowired(required = true)
    private TaskListItemService taskListItemService;


    @Override
    public BasicWorkflowService getBasicWorkflowService() {
        return basicWorkflowService;
    }

    @Override
    public BasicWorkflowItemService getBasicWorkflowItemService() {
        return basicWorkflowItemService;
    }

    @Override
    public TaskListItemService getTaskListItemService() {
        return taskListItemService;
    }

    @Override
    public WorkflowService getWorkflowService() {
        return getBasicWorkflowService();
    }

    @Override
    public WorkflowItemService getWorkflowItemService() {
        return getBasicWorkflowItemService();
    }
}
