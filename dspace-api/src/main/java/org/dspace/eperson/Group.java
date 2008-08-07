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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
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

    /** lists of epeople and groups in the group */
    private List<EPerson> epeople = new ArrayList<EPerson>();

    private List<Group> groups = new ArrayList<Group>();

    /** lists that need to be written out again */
    private boolean epeopleChanged = false;

    private boolean groupsChanged = false;

    /** is this just a stub, or is all data loaded? */
    private boolean isDataLoaded = false;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

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

        // Cache ourselves
        context.cache(this, row.getIntColumn("eperson_group_id"));

        modifiedMetadata = false;
        clearDetails();
    }

    /**
     * Populate Group with eperson and group objects
     * 
     * @param context
     * @throws SQLException
     */
    public void loadData()
    {
        // only populate if not already populated
        if (!isDataLoaded)
        {
            // naughty thing to do - swallowing SQL exception and throwing it as
            // a RuntimeException - a hack to avoid changing the API all over
            // the place
            try
            {
                // get epeople objects
                TableRowIterator tri = DatabaseManager.queryTable(myContext,"eperson",
                                "SELECT eperson.* FROM eperson, epersongroup2eperson WHERE " +
                                "epersongroup2eperson.eperson_id=eperson.eperson_id AND " +
                                "epersongroup2eperson.eperson_group_id= ?",
                                myRow.getIntColumn("eperson_group_id"));

                try
                {
                    while (tri.hasNext())
                    {
                        TableRow r = (TableRow) tri.next();

                        // First check the cache
                        EPerson fromCache = (EPerson) myContext.fromCache(
                                EPerson.class, r.getIntColumn("eperson_id"));

                        if (fromCache != null)
                        {
                            epeople.add(fromCache);
                        }
                        else
                        {
                            epeople.add(new EPerson(myContext, r));
                        }
                    }
                }
                finally
                {
                    // close the TableRowIterator to free up resources
                    if (tri != null)
                        tri.close();
                }

                // now get Group objects
                tri = DatabaseManager.queryTable(myContext,"epersongroup",
                                "SELECT epersongroup.* FROM epersongroup, group2group WHERE " +
                                "group2group.child_id=epersongroup.eperson_group_id AND "+
                                "group2group.parent_id= ? ",
                                myRow.getIntColumn("eperson_group_id"));

                try
                {
                    while (tri.hasNext())
                    {
                        TableRow r = (TableRow) tri.next();

                        // First check the cache
                        Group fromCache = (Group) myContext.fromCache(Group.class,
                                r.getIntColumn("eperson_group_id"));

                        if (fromCache != null)
                        {
                            groups.add(fromCache);
                        }
                        else
                        {
                            groups.add(new Group(myContext, r));
                        }
                    }
                }
                finally
                {
                    // close the TableRowIterator to free up resources
                    if (tri != null)
                        tri.close();
                }

            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            isDataLoaded = true;
        }
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

        context.addEvent(new Event(Event.CREATE, Constants.GROUP, g.getID(), null));

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
        modifiedMetadata = true;
        addDetails("name");
    }

    /**
     * add an eperson member
     * 
     * @param e
     *            eperson
     */
    public void addMember(EPerson e)
    {
        loadData(); // make sure Group has data loaded

        if (isMember(e))
        {
            return;
        }

        epeople.add(e);
        epeopleChanged = true;

        myContext.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail()));
    }

    /**
     * add group to this group
     * 
     * @param g
     */
    public void addMember(Group g)
    {
        loadData(); // make sure Group has data loaded

        // don't add if it's already a member
        if (isMember(g))
        {
            return;
        }

        groups.add(g);
        groupsChanged = true;

        myContext.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName()));
    }

    /**
     * remove an eperson from a group
     * 
     * @param e
     *            eperson
     */
    public void removeMember(EPerson e)
    {
        loadData(); // make sure Group has data loaded

        if (epeople.remove(e))
        {
            epeopleChanged = true;
            myContext.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail()));
        }
    }

    /**
     * remove group from this group
     * 
     * @param g
     */
    public void removeMember(Group g)
    {
        loadData(); // make sure Group has data loaded

        if (groups.remove(g))
        {
            groupsChanged = true;
            myContext.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName()));
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

        loadData(); // make sure Group has data loaded

        return epeople.contains(e);
    }

    /**
     * check to see if group is a member
     * 
     * @param g
     *            group to check
     * @return
     */
    public boolean isMember(Group g)
    {
        loadData(); // make sure Group has data loaded

        return groups.contains(g);
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

        // only test for membership if context contains a user
        if (currentuser != null)
        {
            return epersonInGroup(c, groupid, currentuser);
        }

        // currentuser not set, return FALSE
        return false;
    }

    /**
     * Get all of the groups that an eperson is a member of
     * 
     * @param c
     * @param e
     * @return
     * @throws SQLException
     */
    public static Group[] allMemberGroups(Context c, EPerson e)
            throws SQLException
    {
        List<Group> groupList = new ArrayList<Group>();

        Set<Integer> myGroups = allMemberGroupIDs(c, e);
        // now convert those Integers to Groups
        Iterator i = myGroups.iterator();

        while (i.hasNext())
        {
            groupList.add(Group.find(c, ((Integer) i.next()).intValue()));
        }

        return (Group[]) groupList.toArray(new Group[0]);
    }

    /**
     * get Set of Integers all of the group memberships for an eperson
     * 
     * @param c
     * @param e
     * @return Set of Integer groupIDs
     * @throws SQLException
     */
    public static Set<Integer> allMemberGroupIDs(Context c, EPerson e)
            throws SQLException
    {
        // two queries - first to get groups eperson is a member of
        // second query gets parent groups for groups eperson is a member of

        TableRowIterator tri = DatabaseManager.queryTable(c, "epersongroup2eperson",
                "SELECT * FROM epersongroup2eperson WHERE eperson_id= ?",
                 e.getID());

        Set<Integer> groupIDs = new HashSet<Integer>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                int childID = row.getIntColumn("eperson_group_id");

                groupIDs.add(new Integer(childID));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        // Also need to get all "Special Groups" user is a member of!
        // Otherwise, you're ignoring the user's membership to these groups!
        Group[] specialGroups = c.getSpecialGroups();
        for(int j=0; j<specialGroups.length;j++)
            groupIDs.add(new Integer(specialGroups[j].getID()));
        
        // now we have all owning groups, also grab all parents of owning groups
        // yes, I know this could have been done as one big query and a union,
        // but doing the Oracle port taught me to keep to simple SQL!

        String groupQuery = "";

        Iterator i = groupIDs.iterator();

        // Build a list of query parameters
        Object[] parameters = new Object[groupIDs.size()];
        int idx = 0;
        while (i.hasNext())
        {
            int groupID = ((Integer) i.next()).intValue();

            parameters[idx++] = new Integer(groupID);
            
            groupQuery += "child_id= ? ";
            if (i.hasNext())
                groupQuery += " OR ";
        }

        if ("".equals(groupQuery))
        {
            // don't do query, isn't member of any groups
            return groupIDs;
        }
        
        // was member of at least one group
        // NOTE: even through the query is built dynamicaly all data is seperated into the
        // the parameters array.
        tri = DatabaseManager.queryTable(c, "group2groupcache",
                "SELECT * FROM group2groupcache WHERE " + groupQuery,
                parameters);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                int parentID = row.getIntColumn("parent_id");

                groupIDs.add(new Integer(parentID));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        return groupIDs;
    }
    
    
    /**
     * Get all of the epeople who are a member of the
     * specified group, or a member of a sub-group of the
     * specified group, etc.
     * 
     * @param c   
     *          DSpace context
     * @param g   
     *          Group object
     * @return   Array of EPerson objects
     * @throws SQLException
     */
    public static EPerson[] allMembers(Context c, Group g)
            throws SQLException
    {
        List<EPerson> epersonList = new ArrayList<EPerson>();

        Set<Integer> myEpeople = allMemberIDs(c, g);
        // now convert those Integers to EPerson objects
        Iterator i = myEpeople.iterator();

        while (i.hasNext())
        {
            epersonList.add(EPerson.find(c, ((Integer) i.next()).intValue()));
        }

        return (EPerson[]) epersonList.toArray(new EPerson[0]);
    }

    /**
     * Get Set of all Integers all of the epeople
     * members for a group
     * 
     * @param c
     *          DSpace context
     * @param g
     *          Group object
     * @return Set of Integer epersonIDs
     * @throws SQLException
     */
    public static Set<Integer> allMemberIDs(Context c, Group g)
            throws SQLException
    {
        // two queries - first to get all groups which are a member of this group
        // second query gets all members of each group in the first query
        Set<Integer> epeopleIDs = new HashSet<Integer>();
        
        // Get all groups which are a member of this group
        TableRowIterator tri = DatabaseManager.queryTable(c, "group2groupcache",
                "SELECT * FROM group2groupcache WHERE parent_id= ? ",
                g.getID());
        
        Set<Integer> groupIDs = new HashSet<Integer>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                int childID = row.getIntColumn("child_id");

                groupIDs.add(new Integer(childID));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        // now we have all the groups (including this one)
        // it is time to find all the EPeople who belong to those groups
        // and filter out all duplicates

        Object[] parameters = new Object[groupIDs.size()+1];
        int idx = 0;
        Iterator i = groupIDs.iterator();

        // don't forget to add the current group to this query!
        parameters[idx++] = new Integer(g.getID());
        String epersonQuery = "eperson_group_id= ? ";
        if (i.hasNext())
            epersonQuery += " OR ";
        
        while (i.hasNext())
        {
            int groupID = ((Integer) i.next()).intValue();
            parameters[idx++] = new Integer(groupID);
            
            epersonQuery += "eperson_group_id= ? ";
            if (i.hasNext())
                epersonQuery += " OR ";
        }

        //get all the EPerson IDs
        // Note: even through the query is dynamicaly built all data is seperated
        // into the parameters array.
        tri = DatabaseManager.queryTable(c, "epersongroup2eperson",
                "SELECT * FROM epersongroup2eperson WHERE " + epersonQuery,
                parameters);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                int epersonID = row.getIntColumn("eperson_id");

                epeopleIDs.add(new Integer(epersonID));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        return epeopleIDs;
    }

    private static boolean epersonInGroup(Context c, int groupID, EPerson e)
            throws SQLException
    {
        Set<Integer> groupIDs = Group.allMemberGroupIDs(c, e);

        return groupIDs.contains(new Integer(groupID));
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
     * Finds all groups in the site
     * 
     * @param context
     *            DSpace context
     * @param sortField
     *            field to sort by -- Group.ID or Group.NAME
     * 
     * @return array of all groups in the site
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

        // NOTE: The use of 's' in the order by clause can not cause an sql 
        // injection because the string is derived from constant values above.
        TableRowIterator rows = DatabaseManager.queryTable(
        		context, "epersongroup",
                "SELECT * FROM epersongroup ORDER BY "+s);

        try
        {
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
        finally
        {
            if (rows != null)
                rows.close();
        }
    }
    
    
    /**
     * Find the groups that match the search query across eperson_group_id or name
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return array of Group objects
     */
    public static Group[] search(Context context, String query)
    		throws SQLException
	{
	    return search(context, query, -1, -1);
	}
    
    /**
     * Find the groups that match the search query across eperson_group_id or name
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * @param offset
     *            Inclusive offset 
     * @param limit
     *            Maximum number of matches returned
     * 
     * @return array of Group objects
     */
    public static Group[] search(Context context, String query, int offset, int limit)
    		throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
		queryBuf.append("SELECT * FROM epersongroup WHERE LOWER(name) LIKE LOWER(?) OR eperson_group_id = ? ORDER BY name ASC ");
		
        // Add offset and limit restrictions - Oracle requires special code
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            // First prepare the query to generate row numbers
            if (limit > 0 || offset > 0)
            {
                queryBuf.insert(0, "SELECT /*+ FIRST_ROWS(n) */ rec.*, ROWNUM rnum  FROM (");
                queryBuf.append(") ");
            }

            // Restrict the number of rows returned based on the limit
            if (limit > 0)
            {
                queryBuf.append("rec WHERE rownum<=? ");
                // If we also have an offset, then convert the limit into the maximum row number
                if (offset > 0)
                    limit += offset;
            }

            // Return only the records after the specified offset (row number)
            if (offset > 0)
            {
                queryBuf.insert(0, "SELECT * FROM (");
                queryBuf.append(") WHERE rnum>?");
            }
        }
        else
        {
            if (limit > 0)
                queryBuf.append(" LIMIT ? ");

            if (offset > 0)
                queryBuf.append(" OFFSET ? ");
        }

        String dbquery = queryBuf.toString();

        // When checking against the eperson-id, make sure the query can be made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e) {
			int_param = new Integer(-1);
		}

        // Create the parameter array, including limit and offset if part of the query
        Object[] paramArr = new Object[]{params, int_param};
        if (limit > 0 && offset > 0)
            paramArr = new Object[] {params, int_param,limit,offset};
        else if (limit > 0)
            paramArr = new Object[] {params, int_param,limit};
        else if (offset > 0)
            paramArr = new Object[] {params, int_param,offset};

        TableRowIterator rows =
			DatabaseManager.query(context, dbquery, paramArr);

        try
        {
            List groupRows = rows.toList();
            Group[] groups = new Group[groupRows.size()];

            for (int i = 0; i < groupRows.size(); i++)
            {
                TableRow row = (TableRow) groupRows.get(i);

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
        finally
        {
            if (rows != null)
                rows.close();
        }
	}

    /**
     * Returns the total number of groups returned by a specific query, without the overhead 
     * of creating the Group objects to store the results.
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return the number of groups mathching the query
     */
    public static int searchResultCount(Context context, String query)
    	throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
		String dbquery = "SELECT count(*) as gcount FROM epersongroup WHERE LOWER(name) LIKE LOWER(?) OR eperson_group_id = ? ";
		
		// When checking against the eperson-id, make sure the query can be made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e) {
			int_param = new Integer(-1);
		}
		
		// Get all the epeople that match the query
		TableRow row = DatabaseManager.querySingle(context, dbquery, new Object[] {params, int_param});
		
		// use getIntColumn for Oracle count data
		Long count;
        if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
        {
            count = new Long(row.getIntColumn("gcount"));
        }
        else  //getLongColumn works for postgres
        {
            count = new Long(row.getLongColumn("gcount"));
        }

		return count.intValue();
	}
    
    
    /**
     * Delete a group
     * 
     */
    public void delete() throws SQLException
    {
        // FIXME: authorizations

        myContext.addEvent(new Event(Event.DELETE, Constants.GROUP, getID(), getName()));

        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove any ResourcePolicies that reference this group
        AuthorizeManager.removeGroupPolicies(myContext, getID());

        // Remove any group memberships first
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM EPersonGroup2EPerson WHERE eperson_group_id= ? ",
                getID());

        // remove any group2groupcache entries
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM group2groupcache WHERE parent_id= ? OR child_id= ? ",
                getID(),getID());

        // Now remove any group2group assignments
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM group2group WHERE parent_id= ? OR child_id= ? ",
                getID(),getID());

        // don't forget the new table
        deleteEpersonGroup2WorkspaceItem();

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        epeople.clear();

        log.info(LogManager.getHeader(myContext, "delete_group", "group_id="
                + getID()));
    }

    /**
     * @throws SQLException
     */
    private void deleteEpersonGroup2WorkspaceItem() throws SQLException
    {
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM EPersonGroup2WorkspaceItem WHERE eperson_group_id= ? ",
                getID());
    }

    /**
     * Return EPerson members of a Group
     */
    public EPerson[] getMembers()
    {
        loadData(); // make sure all data is loaded

        EPerson[] myArray = new EPerson[epeople.size()];
        myArray = (EPerson[]) epeople.toArray(myArray);

        return myArray;
    }
   
    /**
     * Return Group members of a Group
     * 
     * @return
     */
    public Group[] getMemberGroups()
    {
        loadData(); // make sure all data is loaded

        Group[] myArray = new Group[groups.size()];
        myArray = (Group[]) groups.toArray(myArray);

        return myArray;
    }
    
    /**
     * Return true if group has no members
     */
    public boolean isEmpty()
    {
        loadData(); // make sure all data is loaded

        if ((epeople.size() == 0) && (groups.size() == 0))
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

        if (modifiedMetadata)
        {
            myContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.GROUP, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }

        // Redo eperson mappings if they've changed
        if (epeopleChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(myContext,
                    "delete from epersongroup2eperson where eperson_group_id= ? ",
                    getID());

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

            epeopleChanged = false;
        }

        // Redo Group mappings if they've changed
        if (groupsChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(myContext,
                    "delete from group2group where parent_id= ? ",
                    getID());

            // Add new mappings
            Iterator i = groups.iterator();

            while (i.hasNext())
            {
                Group g = (Group) i.next();

                TableRow mappingRow = DatabaseManager.create(myContext,
                        "group2group");
                mappingRow.setColumn("parent_id", getID());
                mappingRow.setColumn("child_id", g.getID());
                DatabaseManager.update(myContext, mappingRow);
            }

            // groups changed, now change group cache
            rethinkGroupCache();

            groupsChanged = false;
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

    /**
     * Regenerate the group cache AKA the group2groupcache table in the database -
     * meant to be called when a group is added or removed from another group
     * 
     */
    private void rethinkGroupCache() throws SQLException
    {
        // read in the group2group table
        TableRowIterator tri = DatabaseManager.queryTable(myContext, "group2group",
                "SELECT * FROM group2group");

        Map<Integer,Set<Integer>> parents = new HashMap<Integer,Set<Integer>>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = (TableRow) tri.next();

                Integer parentID = new Integer(row.getIntColumn("parent_id"));
                Integer childID = new Integer(row.getIntColumn("child_id"));

                // if parent doesn't have an entry, create one
                if (!parents.containsKey(parentID))
                {
                    Set<Integer> children = new HashSet<Integer>();

                    // add child id to the list
                    children.add(childID);
                    parents.put(parentID, children);
                }
                else
                {
                    // parent has an entry, now add the child to the parent's record
                    // of children
                    Set<Integer> children =  parents.get(parentID);
                    children.add(childID);
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        // now parents is a hash of all of the IDs of groups that are parents
        // and each hash entry is a hash of all of the IDs of children of those
        // parent groups
        // so now to establish all parent,child relationships we can iterate
        // through the parents hash

        Iterator i = parents.keySet().iterator();

        while (i.hasNext())
        {
            Integer parentID = (Integer) i.next();

            Set<Integer> myChildren = getChildren(parents, parentID);

            Iterator j = myChildren.iterator();

            while (j.hasNext())
            {
                // child of a parent
                Integer childID = (Integer) j.next();

                ((Set<Integer>) parents.get(parentID)).add(childID);
            }
        }

        // empty out group2groupcache table
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM group2groupcache WHERE id >= 0");

        // write out new one
        Iterator pi = parents.keySet().iterator(); // parent iterator

        while (pi.hasNext())
        {
            Integer parent = (Integer) pi.next();

            Set<Integer> children =  parents.get(parent);
            Iterator ci = children.iterator(); // child iterator

            while (ci.hasNext())
            {
                Integer child = (Integer) ci.next();

                TableRow row = DatabaseManager.create(myContext,
                        "group2groupcache");

                int parentID = parent.intValue();
                int childID = child.intValue();

                row.setColumn("parent_id", parentID);
                row.setColumn("child_id", childID);

                DatabaseManager.update(myContext, row);
            }
        }
    }

    /**
     * Used recursively to generate a map of ALL of the children of the given
     * parent
     * 
     * @param parents
     *            Map of parent,child relationships
     * @param parent
     *            the parent you're interested in
     * @return Map whose keys are all of the children of a parent
     */
    private Set<Integer> getChildren(Map<Integer,Set<Integer>> parents, Integer parent)
    {
        Set<Integer> myChildren = new HashSet<Integer>();

        // degenerate case, this parent has no children
        if (!parents.containsKey(parent))
            return myChildren;

        // got this far, so we must have children
        Set<Integer> children =  parents.get(parent);

        // now iterate over all of the children
        Iterator i = children.iterator();

        while (i.hasNext())
        {
            Integer childID = (Integer) i.next();

            // add this child's ID to our return set
            myChildren.add(childID);

            // and now its children
            myChildren.addAll(getChildren(parents, childID));
        }

        return myChildren;
    }
}
