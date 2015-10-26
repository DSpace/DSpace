/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.service.SupervisorService;
import org.springframework.beans.factory.annotation.Autowired;

public class SupervisorServiceImpl implements SupervisorService{

    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;

    protected SupervisorServiceImpl()
    {
    }

    @Override
    public boolean isOrder(Context context, WorkspaceItem workspaceItem, Group group)
        throws SQLException
    {
        return workspaceItem.getSupervisorGroups().contains(group);
    }

    @Override
    public void remove(Context context, WorkspaceItem workspaceItem, Group group)
        throws SQLException, AuthorizeException
    {
        // get the workspace item and the group from the request values
        workspaceItem.getSupervisorGroups().remove(group);

        // get the item and have it remove the policies for the group
        Item item = workspaceItem.getItem();
        itemService.removeGroupPolicies(context, item, group);
    }

    @Override
    public void add(Context context, Group group, WorkspaceItem workspaceItem, int policy)
        throws SQLException, AuthorizeException
    {
        // make a table row in the database table, and update with the relevant
        // details
        workspaceItem.getSupervisorGroups().add(group);
        group.getSupervisedItems().add(workspaceItem);

        // If a default policy type has been requested, apply the policies using
        // the DSpace API for doing so
        if (policy != POLICY_NONE)
        {
            Item item = workspaceItem.getItem();

            // "Editor" implies READ, WRITE, ADD permissions
            // "Observer" implies READ permissions
            if (policy == POLICY_EDITOR)
            {
                ResourcePolicy r = resourcePolicyService.create(context);
                r.setdSpaceObject(item);
                r.setGroup(group);
                r.setAction(Constants.READ);
                resourcePolicyService.update(context, r);
                
                r = resourcePolicyService.create(context);
                r.setdSpaceObject(item);
                r.setGroup(group);
                r.setAction(Constants.WRITE);
                resourcePolicyService.update(context, r);
                
                r = resourcePolicyService.create(context);
                r.setdSpaceObject(item);
                r.setGroup(group);
                r.setAction(Constants.ADD);
                resourcePolicyService.update(context, r);
                
            } 
            else if (policy == POLICY_OBSERVER)
            {
                ResourcePolicy r = resourcePolicyService.create(context);
                r.setdSpaceObject(item);
                r.setGroup(group);
                r.setAction(Constants.READ);
                resourcePolicyService.update(context, r);
            }
        }
    }
}
