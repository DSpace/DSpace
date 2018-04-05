/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.service.*;

/**
 * Abstract factory to get services for the xmlworkflow package, use XmlWorkflowServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class XmlWorkflowServiceFactory extends WorkflowServiceFactory {

    public abstract XmlWorkflowFactory getWorkflowFactory();

    public abstract WorkflowRequirementsService getWorkflowRequirementsService();

    public abstract XmlWorkflowService getXmlWorkflowService();

    public abstract ClaimedTaskService getClaimedTaskService();

    public abstract CollectionRoleService getCollectionRoleService();

    public abstract InProgressUserService getInProgressUserService();

    public abstract PoolTaskService getPoolTaskService();

    public abstract WorkflowItemRoleService getWorkflowItemRoleService();

    public abstract XmlWorkflowItemService getXmlWorkflowItemService();

    public static XmlWorkflowServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("workflowServiceFactory", XmlWorkflowServiceFactory.class);
    }
}
