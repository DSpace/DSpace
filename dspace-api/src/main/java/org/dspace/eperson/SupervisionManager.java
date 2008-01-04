/*
 * SupervisionManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.eperson;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.authorize.dao.ResourcePolicyDAOFactory;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;

/**
 * Class to manage supervisions, primarily for use in applying supervisor
 * activities to the database, such as setting and unsetting supervision
 * orders and so forth.
 *
 * @author  Richard Jones
 * @version $Revision$
 */
public abstract class SupervisionManager
{
    /** value to use for no policy set */
    public static int POLICY_NONE = 0;

    /** value to use for editor policies */
    public static int POLICY_EDITOR = 1;

    /** value to use for observer policies */
    public static int POLICY_OBSERVER = 2;

    /**
     * finds out if there is a supervision order that matches this set
     * of values
     *
     * @param context   the context this object exists in
     * @param wsItemID  the workspace item to be supervised
     * @param groupID   the group to be doing the supervising
     *
     * @return boolean  true if there is an order that matches, false if not
     */
    public static boolean isOrder(Context context, int wsItemID, int groupID)
    {
        GroupDAO groupDAO = GroupDAOFactory.getInstance(context);
        WorkspaceItemDAO wsiDAO = WorkspaceItemDAOFactory.getInstance(context);

        Group group = groupDAO.retrieve(groupID);
        WorkspaceItem wsi = wsiDAO.retrieve(wsItemID);

        if (group == null || wsi == null)
        {
            return false;
        }

        return groupDAO.linked(group, wsi);
    }

    /**
     * removes the requested group from the requested workspace item in terms
     * of supervision.  This also removes all the policies that group has
     * associated with the item
     * 
     * @param context   the context this object exists in
     * @param wsItemID  the ID of the workspace item
     * @param groupID   the ID of the group to be removed from the item
     * @throws AuthorizeException
     */
    public static void remove(Context context, int wsItemID, int groupID)
        throws AuthorizeException
    {
        GroupDAO groupDAO = GroupDAOFactory.getInstance(context);
        WorkspaceItemDAO wsiDAO = WorkspaceItemDAOFactory.getInstance(context);

        // get the workspace item and the group from the request values
        WorkspaceItem wsi = wsiDAO.retrieve(wsItemID);
        Group group = groupDAO.retrieve(groupID);

        if (group == null || wsi == null)
        {
            return;
        }

        groupDAO.unlink(group, wsi);

        // get the item and have it remove the policies for the group
        Item item = wsi.getItem();
        item.removeGroupPolicies(group);
    }

    /**
     * removes redundant entries in the supervision orders database
     * 
     * @param context   the context this object exists in
     */
    public static void removeRedundant(Context context)
    {
        GroupDAO groupDAO = GroupDAOFactory.getInstance(context);
        groupDAO.cleanSupervisionOrders();
    }

    /**
     * adds a supervision order to the database
     * 
     * @param context   the context this object exists in
     * @param groupID   the ID of the group which will supervise
     * @param wsItemID  the ID of the workspace item to be supervised
     * @param policy    String containing the policy type to be used
     * @throws AuthorizeException
     */
    public static void add(Context context, int groupID, int wsItemID, int policy)
        throws AuthorizeException
    {
        GroupDAO groupDAO = GroupDAOFactory.getInstance(context);
        ResourcePolicyDAO rpDAO = ResourcePolicyDAOFactory.getInstance(context);
        WorkspaceItemDAO wsiDAO = WorkspaceItemDAOFactory.getInstance(context);

        // get the workspace item and the group from the request values
        WorkspaceItem wsi = wsiDAO.retrieve(wsItemID);
        Group group = groupDAO.retrieve(groupID);

        if (group == null || wsi == null)
        {
            return;
        }

        groupDAO.link(group, wsi);

        // If a default policy type has been requested, apply the policies using
        // the DSpace API for doing so
        if (policy != POLICY_NONE)
        {
            Item item = wsi.getItem();
            
            // "Editor" implies READ, WRITE, ADD permissions
            // "Observer" implies READ permissions
            if (policy == POLICY_EDITOR)
            {
                ResourcePolicy r = rpDAO.create();
                r.setResource(item);
                r.setGroup(group);
                r.setAction(Constants.READ);
                rpDAO.update(r);
                
                r = rpDAO.create();
                r.setResource(item);
                r.setGroup(group);
                r.setAction(Constants.WRITE);
                rpDAO.update(r);
                
                r = rpDAO.create();
                r.setResource(item);
                r.setGroup(group);
                r.setAction(Constants.ADD);
                rpDAO.update(r);
                
            } 
            else if (policy == POLICY_OBSERVER)
            {
                ResourcePolicy r = rpDAO.create();
                r.setResource(item);
                r.setGroup(group);
                r.setAction(Constants.READ);
                rpDAO.update(r);
            }
        }
    }
}
