/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow.factory;

import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.InProgressUserService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the xmlworkflow package, use XmlWorkflowServiceFactory.getInstance() to
 * retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class WorkflowServiceFactoryImpl extends WorkflowServiceFactory {

    @Autowired(required = true)
    private XmlWorkflowFactory workflowFactory;
    @Autowired(required = true)
    private WorkflowRequirementsService workflowRequirementsService;
    @Autowired(required = true)
    private WorkflowService workflowService;
    @Autowired(required = true)
    private ClaimedTaskService claimedTaskService;
    @Autowired(required = true)
    private CollectionRoleService collectionRoleService;
    @Autowired(required = true)
    private InProgressUserService inProgressUserService;
    @Autowired(required = true)
    private PoolTaskService poolTaskService;
    @Autowired(required = true)
    private WorkflowItemRoleService workflowItemRoleService;
    @Autowired(required = true)
    private WorkflowItemService workflowItemService;

    @Override
    public XmlWorkflowFactory getWorkflowFactory() {
        return workflowFactory;
    }

    @Override
    public WorkflowRequirementsService getWorkflowRequirementsService() {
        return workflowRequirementsService;
    }

    @Override
    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    @Override
    public ClaimedTaskService getClaimedTaskService() {
        return claimedTaskService;
    }

    @Override
    public CollectionRoleService getCollectionRoleService() {
        return collectionRoleService;
    }

    @Override
    public InProgressUserService getInProgressUserService() {
        return inProgressUserService;
    }

    @Override
    public PoolTaskService getPoolTaskService() {
        return poolTaskService;
    }

    @Override
    public WorkflowItemRoleService getWorkflowItemRoleService() {
        return workflowItemRoleService;
    }

    @Override
    public WorkflowItemService getWorkflowItemService() {
        return workflowItemService;
    }
}
