/*
 * GroupDAO.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.eperson.dao;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * FIXME: We actually implement the Link interface for two other pairs of
 * classes as well, but we can't cleanly express this below.
 *
 * @author James Rutherford
 */
public class GroupDAOCore extends GroupDAO
{
    protected Logger log = Logger.getLogger(GroupDAO.class);

    protected Context context;
    protected EPersonDAO epersonDAO;

    public GroupDAOCore(Context context)
    {
        super(context);
    }

    public Group create() throws AuthorizeException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson Group");
        }

        Group group = childDAO.create();

        log.info(LogManager.getHeader(context, "create_group", "group_id="
                + group.getID()));

        return group;
    }

    public Group retrieve(int id)
    {
        Group group = (Group) context.fromCache(Group.class, id);

        if (group == null)
        {
            group = childDAO.retrieve(id);
        }

        return group;
    }

    /**
     * FIXME: Look back into ItemDAOPostgres to see how we were cunning there
     * about updating Bundles + Bitstreams and use that below for EPeople and
     * Groups.
     */
    public void update(Group group) throws AuthorizeException
    {
        // Check authorisation - if you're not the eperson
        // see if the authorization system says you can
        if (!context.ignoreAuthorization())
        {
            AuthorizeManager.authorizeAction(context, group, Constants.WRITE);
        }

        log.info(LogManager.getHeader(context, "update_group", "group_id="
                + group.getID()));

        EPerson[] epeople = group.getMembers();

        for (EPerson storedEPerson : epersonDAO.getEPeople(group))
        {
            boolean deleted = true;
            for (EPerson eperson : epeople)
            {
                if (eperson.equals(storedEPerson))
                {
                    deleted = false;
                    break;
                }
            }

            if (deleted)
            {
                unlink(group, storedEPerson);
            }
        }

        for (EPerson eperson : epeople)
        {
            link(group, eperson);
        }
        
        Group[] groups = group.getMemberGroups();

        for (Group storedGroup : getMemberGroups(group))
        {
            boolean deleted = true;
            for (Group g : groups)
            {
                if (g.equals(storedGroup))
                {
                    deleted = false;
                    break;
                }
            }

            if (deleted)
            {
                unlink(group, storedGroup);
            }
        }

        for (EPerson eperson : epeople)
        {
            link(group, eperson);
        }

        childDAO.update(group);
    }

    public void delete(int id) throws AuthorizeException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete a Group");
        }

        Group group = retrieve(id);

        context.removeCached(group, id);

        // Remove any ResourcePolicies that reference this group
        AuthorizeManager.removeGroupPolicies(context, id);

        log.info(LogManager.getHeader(context, "delete_group", "group_id=" +
                    id));

        childDAO.delete(id);
    }

    public List<Group> getGroups()
    {
        // default to sorting by id. it's a bit arbitrary, but i don't think
        // anyone will care.
        return childDAO.getGroups(Group.ID);
    }

    public List<Group> search(String query)
    {
        return childDAO.search(query, -1, -1);
    }

    /**
     * Find out whether or not the logged in EPerson is a member of the given
     * Group. The reason we take an ID rather than a full object is because we
     * may be able to give a really quick answer without having to actually
     * inspect the Group beyond knowing its ID.
     */
    public boolean currentUserInGroup(int groupID)
    {
        // special, everyone is member of group 0 (anonymous)
        if (groupID == 0)
        {
            return true;
        }

        // first, check for membership if it's a special group
        // (special groups can be set even if person isn't authenticated)
        if (context.inSpecialGroup(groupID))
        {
            return true;
        }

        EPerson currentuser = context.getCurrentUser();

        // only test for membership if context contains a user
        if (currentuser != null)
        {
            Set<Integer> groupIDs = getGroupIDs(currentuser);

            return groupIDs.contains(groupID);
        }

        return false;
    }
}
