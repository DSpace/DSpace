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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

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

    /** Our context */
    private Context myContext;

    /** The row in the table representing this object */
    private TableRow myRow;

    /** list of epeople in the group */
    private List epeople = new ArrayList();

    /** epeople list needs to be written out again */
    private boolean epeoplechanged = false;

    /**
     * Construct a Group from a given context and tablerow
     * 
     * @param context
     * @param row
     */
    Group(Context context, TableRow row) throws SQLException
    {
        myContext = context;
        myRow = row;

        // get epeople objects
        TableRowIterator tri = DatabaseManager
                .query(
                        myContext,
                        "eperson",
                        "SELECT eperson.* FROM eperson, epersongroup2eperson WHERE "
                                + "epersongroup2eperson.eperson_id=eperson.eperson_id AND "
                                + "epersongroup2eperson.eperson_group_id="
                                + myRow.getIntColumn("eperson_group_id"));

        while (tri.hasNext())
        {
            TableRow r = (TableRow) tri.next();

            // First check the cache
            EPerson fromCache = (EPerson) myContext.fromCache(EPerson.class, r
                    .getIntColumn("eperson_id"));

            if (fromCache != null)
            {
                epeople.add(fromCache);
            }
            else
            {
                epeople.add(new EPerson(myContext, r));
            }
        }

        // Cache ourselves
        context.cache(this, row.getIntColumn("eperson_group_id"));
    }

    /**
     * Create a new group
     * 
     * @param context
     *            DSpace context object
     */
    public static Group create(Context context) throws SQLException,
            AuthorizeException
    {
        // FIXME - authorization?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson Group");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "epersongroup");

        Group g = new Group(context, row);

        log.info(LogManager.getHeader(context, "create_group", "group_id="
                + g.getID()));

        return g;
    }

    /**
     * get the ID of the group object
     * 
     * @return id
     */
    public int getID()
    {
        return myRow.getIntColumn("eperson_group_id");
    }

    /**
     * get name of group
     * 
     * @return name
     */
    public String getName()
    {
        return myRow.getStringColumn("name");
    }

    /**
     * set name of group
     * 
     * @param name
     *            new group name
     */
    public void setName(String name)
    {
        myRow.setColumn("name", name);
    }

    /**
     * add an eperson member
     * 
     * @param e
     *            eperson
     */
    public void addMember(EPerson e)
    {
        if (epeople.contains(e))
        {
            return;
        }

        epeople.add(e);
        epeoplechanged = true;
    }

    /**
     * remove an eperson from a group
     * 
     * @param e
     *            eperson
     */
    public void removeMember(EPerson e)
    {
        if (epeople.remove(e))
        {
            epeoplechanged = true;
        }
    }

    /**
     * check to see if an eperson is a member
     * 
     * @param e
     *            eperson to check membership
     */
    public boolean isMember(EPerson e)
    {
        // special, group 0 is anonymous
        if (getID() == 0)
        {
            return true;
        }

        return epeople.contains(e);
    }

    /**
     * fast check to see if an eperson is a member called with eperson id, does
     * database lookup without instantiating all of the epeople objects and is
     * thus a static method
     * 
     * @param c
     *            context
     * @param groupid
     *            group ID to check
     */
    public static boolean isMember(Context c, int groupid) throws SQLException
    {
        // special, everyone is member of group 0 (anonymous)
        if (groupid == 0)
        {
            return true;
        }

        // first, check for membership if it's a special group
        // (special groups can be set even if person isn't authenticated)
        if (c.inSpecialGroup(groupid))
        {
            return true;
        }

        EPerson currentuser = c.getCurrentUser();

        //  only test for membership if context contains a user
        if (currentuser != null)
        {
            // not in special groups, try database
            int userid = currentuser.getID();

            TableRowIterator tri = DatabaseManager
                    .query(
                            c,
                            "eperson",
                            "SELECT eperson.* FROM eperson, epersongroup2eperson WHERE "
                                    + "epersongroup2eperson.eperson_id=eperson.eperson_id AND "
                                    + "epersongroup2eperson.eperson_group_id="
                                    + groupid + " AND eperson.eperson_id="
                                    + userid);

            if (tri.hasNext())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * find the group by its ID
     * 
     * @param context
     * @param id
     */
    public static Group find(Context context, int id) throws SQLException
    {
        // First check the cache
        Group fromCache = (Group) context.fromCache(Group.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "epersongroup", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new Group(context, row);
        }
    }

    /**
     * Find the group by its name - assumes name is unique
     * 
     * @param context
     * @param name
     * 
     * @return Group
     */
    public static Group findByName(Context context, String name)
            throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context, "epersongroup",
                "name", name);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            Group fromCache = (Group) context.fromCache(Group.class, row
                    .getIntColumn("eperson_group_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new Group(context, row);
            }
        }
    }

    /**
     * finds all groups in the site
     * 
     * @param context
     * @param sortField,
     *            Group.ID or Group.NAME (default)
     * 
     * @return Group []
     */
    public static Group[] findAll(Context context, int sortField)
            throws SQLException
    {
        String s;

        switch (sortField)
        {
        case ID:
            s = "eperson_group_id";

            break;

        case NAME:
            s = "name";

            break;

        default:
            s = "name";
        }

        TableRowIterator rows = DatabaseManager.query(context, "epersongroup",
                "SELECT * FROM epersongroup ORDER BY " + s);

        List gRows = rows.toList();

        Group[] groups = new Group[gRows.size()];

        for (int i = 0; i < gRows.size(); i++)
        {
            TableRow row = (TableRow) gRows.get(i);

            // First check the cache
            Group fromCache = (Group) context.fromCache(Group.class, row
                    .getIntColumn("eperson_group_id"));

            if (fromCache != null)
            {
                groups[i] = fromCache;
            }
            else
            {
                groups[i] = new Group(context, row);
            }
        }

        return groups;
    }

    /**
     * Delete a group
     *  
     */
    public void delete() throws SQLException
    {
        // FIXME: authorizations
        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove any ResourcePolicies that reference this group
        AuthorizeManager.removeGroupPolicies(myContext, getID());

        // Remove any group memberships first
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM EPersonGroup2EPerson WHERE eperson_group_id="
                        + getID());

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        epeople.clear();

        log.info(LogManager.getHeader(myContext, "delete_group", "group_id="
                + getID()));
    }

    /**
     * Return EPerson members of a group
     */
    public EPerson[] getMembers()
    {
        EPerson[] myArray = new EPerson[epeople.size()];
        myArray = (EPerson[]) epeople.toArray(myArray);

        return myArray;
    }

    /**
     * Return true if group has no members
     */
    public boolean isEmpty()
    {
        if (epeople.size() == 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Update the group - writing out group object and EPerson list if necessary
     */
    public void update() throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation
        DatabaseManager.update(myContext, myRow);

        // Redo eperson mappings if they've changed
        if (epeoplechanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(myContext,
                    "delete from epersongroup2eperson where eperson_group_id="
                            + getID());

            // Add new mappings
            Iterator i = epeople.iterator();

            while (i.hasNext())
            {
                EPerson e = (EPerson) i.next();

                TableRow mappingRow = DatabaseManager.create(myContext,
                        "epersongroup2eperson");
                mappingRow.setColumn("eperson_id", e.getID());
                mappingRow.setColumn("eperson_group_id", getID());
                DatabaseManager.update(myContext, mappingRow);
            }

            epeoplechanged = false;
        }

        log.info(LogManager.getHeader(myContext, "update_group", "group_id="
                + getID()));
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Group as
     * this object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same group
     *         as this object
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof Group))
        {
            return false;
        }

        return (getID() == ((Group) other).getID());
    }

    public int getType()
    {
        return Constants.GROUP;
    }

    public String getHandle()
    {
        return null;
    }
}
