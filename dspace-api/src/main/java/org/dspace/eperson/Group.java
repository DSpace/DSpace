/*
 * Group.java
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.event.Event;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.UnsupportedIdentifierException;

/**
 * Class representing a group of e-people.
 *
 * @author David Stuve
 * @version $Revision$
 */
public class Group extends DSpaceObject
{
    // findAll sortby types
    public static final int ID = 0; // sort by ID
    public static final int NAME = 1; // sort by NAME (default)

    /** log4j logger */
    private static Logger log = Logger.getLogger(Group.class);

    protected GroupDAO dao;
    protected EPersonDAO epersonDAO;

    private String name;

    /** lists of epeople and groups in the group */
    protected List<EPerson> epeople;
    protected List<Group> groups;

    /** lists that need to be written out again */
    private boolean epeopleChanged = false;

    private boolean groupsChanged = false;

    /** is this just a stub, or is all data loaded? */
    private boolean isDataLoaded = false;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;
    
    public Group(Context context, int id)
    {
        this.id = id;
        this.context = context;

        dao = GroupDAOFactory.getInstance(context);
        epersonDAO = EPersonDAOFactory.getInstance(context);

        modifiedMetadata = false;
        clearDetails();

        epeople = new ArrayList<EPerson>();
        groups = new ArrayList<Group>();

        context.cache(this, id);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
        modifiedMetadata = true;
        addDetails("name");
    }

    public void addMember(EPerson e)
    {
        if (isMember(e))
        {
            return;
        }

        epeople.add(e);

        context.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail()));
    }

    public void addMember(Group g)
    {
        if (isMember(g))
        {
            return;
        }

        groups.add(g);

        context.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName()));
    }

    public void removeMember(EPerson e)
    {
        if (epeople.remove(e))
        {
            epeopleChanged = true;
            context.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail()));
        }
    }

    public void removeMember(Group g)
    {
        if (groups.remove(g))
        {
            groupsChanged = true;
            context.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName()));
        }
    }

    public boolean isMember(EPerson e)
    {
        // special, group 0 is anonymous
        if (id == 0)
        {
            return true;
        }

        return this.contains(epeople, e);
    }

    public boolean isMember(Group g)
    {
        return this.contains(groups, g);
    }

    public Group[] getMemberGroups()
    {
        return (Group[]) groups.toArray(new Group[0]);
    }

    /**
     * Return EPerson members of a Group
     */
    public EPerson[] getMembers()
    {
        return (EPerson[]) epeople.toArray(new EPerson[0]);
    }

    /**
     * Return true if group has no members
     */
    public boolean isEmpty()
    {
        if ((epeople.size() == 0) && (groups.size() == 0))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Override some identification methods
    ////////////////////////////////////////////////////////////////////

    public void addExternalIdentifier(ExternalIdentifier identifier)
            throws UnsupportedIdentifierException
    {
        throw new UnsupportedIdentifierException("Groups cannot have ExternalIdentifiers");
    }

    public void setExternalIdentifiers(List<ExternalIdentifier> identifiers)
            throws UnsupportedIdentifierException
    {
        throw new UnsupportedIdentifierException("Groups cannot have ExternalIdentifiers");
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    /**
     * return type found in Constants
     */
    public int getType()
    {
        return Constants.GROUP;
    }

    ////////////////////////////////////////////////////////////////////
    // Deprecated methods
    ////////////////////////////////////////////////////////////////////

    @Deprecated
    public static boolean isMember(Context context, int groupID)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        return dao.currentUserInGroup(groupID);
    }

    @Deprecated
    public static Group create(Context context) throws AuthorizeException
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.create();
        
		context.addEvent(new Event(Event.CREATE, Constants.GROUP, group.getID(), null));
        
        return group;
    }

    @Deprecated
    public void update() throws AuthorizeException
    {
        dao.update(this);
        
        if (modifiedMetadata)
        {
            context.addEvent(new Event(Event.MODIFY_METADATA, Constants.GROUP, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }
    }

    @Deprecated
    public void delete() throws AuthorizeException
    {
        dao.delete(this.getID());
        context.addEvent(new Event(Event.DELETE, Constants.GROUP, getID(), getName()));
        
    }

    @Deprecated
    public static Group find(Context context, int id)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.retrieve(id);

        return group;
    }

    @Deprecated
    public static Group[] findAll(Context context, int sortField)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        List<Group> groups = dao.getGroups(sortField);

        return (Group[]) groups.toArray(new Group[0]);
    }

    /**
     * FIXME: Assumes the group name is unique. I don't think this is enforced
     * anywhere. Even so, this should probably call search() anyway.
     */
    @Deprecated
    public static Group findByName(Context context, String name)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        Group group = dao.retrieve(name);

        return group;
    }

    @Deprecated
    public static EPerson[] allMembers(Context context, Group group)
    {
        EPersonDAO dao = EPersonDAOFactory.getInstance(context);
        List<EPerson> epeople = dao.getAllEPeople(group);

        return (EPerson[]) epeople.toArray(new EPerson[0]);
    }

    @Deprecated
    public static Group[] allMemberGroups(Context context, EPerson eperson)
    {
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        List<Group> groups = dao.getGroups(eperson);

        return (Group[]) groups.toArray(new Group[0]);
    }

    @Deprecated
    public static Group[] search(Context context, String query)
	{
	    return search(context, query, -1, -1);
	}

    @Deprecated
    public static Group[] search(Context context, String query,
            int offset, int limit)
	{
        GroupDAO dao = GroupDAOFactory.getInstance(context);
        List<Group> groups = dao.search(query, offset, limit);

        return (Group[]) groups.toArray(new Group[0]);
	}
}
