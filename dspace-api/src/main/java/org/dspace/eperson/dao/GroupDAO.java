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
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.dao.CRUD;
import org.dspace.dao.Link;
import org.dspace.dao.StackableDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * FIXME: We actually implement the Link interface for two other pairs of
 * classes as well, but we can't cleanly express this below.
 *
 * @author James Rutherford
 */
public abstract class GroupDAO extends StackableDAO<GroupDAO>
        implements CRUD<Group>, Link<Group, Group>
{
    protected Logger log = Logger.getLogger(GroupDAO.class);

    protected Context context;
    protected EPersonDAO epersonDAO;

    protected GroupDAO childDAO;

    public GroupDAO()
    {
    }

    public GroupDAO(Context context)
    {
        this.context = context;

        epersonDAO = EPersonDAOFactory.getInstance(context);
    }

    public GroupDAO getChild()
    {
        return childDAO;
    }

    public void setChild(GroupDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public Group create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public Group retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public Group retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public Group retrieve(String name)
    {
        return childDAO.retrieve(name);
    }

    public void update(Group group) throws AuthorizeException
    {
        childDAO.update(group);
    }

    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    public List<Group> getGroups()
    {
        return childDAO.getGroups();
    }

    public List<Group> getGroups(int sortField)
    {
        return childDAO.getGroups(sortField);
    }

    /**
     * Returns a list of all the Groups the given EPerson is a member of.
     */
    public List<Group> getGroups(EPerson eperson)
    {
        return childDAO.getGroups(eperson);
    }

    public Set<Integer> getGroupIDs(EPerson eperson)
    {
        return childDAO.getGroupIDs(eperson);
    }

    public List<Group> getSupervisorGroups()
    {
        return childDAO.getSupervisorGroups();
    }

    /**
     * Gets all the groups that are supervising an in-progress submission
     */
    public List<Group> getSupervisorGroups(InProgressSubmission ips)
    {
        return childDAO.getSupervisorGroups(ips);
    }


    /**
     * Returns a list of all the immediate subgroups of the given Group.
     */
    public List<Group> getMemberGroups(Group group)
    {
        return childDAO.getMemberGroups(group);
    }

    /**
     * Find the groups that match the search query across eperson_group_id or
     * name.
     *
     * @param query The search string
     *
     * @return List of Group objects
     */
    public List<Group> search(String query)
    {
        return childDAO.search(query);
    }

    /**
     * Find the groups that match the search query across eperson_group_id or
     * name.
     *
     * @param query The search string
     * @param offset Inclusive offset
     * @param limit Maximum number of matches returned
     *
     * @return List of Group objects
     */
    public List<Group> search(String query, int offset, int limit)
    {
        return childDAO.search(query, offset, limit);
    }

    /**
     * Find out whether or not the logged in EPerson is a member of the given
     * Group. The reason we take an ID rather than a full object is because we
     * may be able to give a really quick answer without having to actually
     * inspect the Group beyond knowing its ID.
     */
    public boolean currentUserInGroup(int groupID)
    {
        return childDAO.currentUserInGroup(groupID);
    }

    // FIXME: All of these should probably check authorization
    public void link(Group parent, Group child)
    {
        childDAO.link(parent, child);
    }

    public void unlink(Group parent, Group child)
    {
        childDAO.link(parent, child);
    }

    public boolean linked(Group parent, Group child)
    {
        return childDAO.linked(parent, child);
    }

    public void link(Group group, EPerson eperson)
    {
        childDAO.link(group, eperson);
    }

    public void unlink(Group group, EPerson eperson)
    {
        childDAO.link(group, eperson);
    }

    public boolean linked(Group group, EPerson eperson)
    {
        return childDAO.linked(group, eperson);
    }


    public void link(Group group, InProgressSubmission ips)
    {
        childDAO.link(group, ips);
    }

    public void unlink(Group group, InProgressSubmission ips)
    {
        childDAO.link(group, ips);
    }

    public boolean linked(Group group, InProgressSubmission ips)
    {
        return childDAO.linked(group, ips);
    }

    public void cleanSupervisionOrders()
    {
        childDAO.cleanSupervisionOrders();
    }

}

