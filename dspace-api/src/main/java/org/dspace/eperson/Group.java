/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
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
    private static final Logger log = Logger.getLogger(Group.class);

    /** ID of Anonymous Group */
    public static final int ANONYMOUS_ID = 0;

    /** ID of Administrator Group */
    public static final int ADMIN_ID = 1;

    /** The row in the table representing this object */
    private final TableRow myRow;

    /** lists of epeople and groups in the group */
    private List<EPerson> epeople = new ArrayList<EPerson>();

    private List<Group> groups = new ArrayList<Group>();

    /** lists that need to be written out again */
    private boolean epeopleChanged = false;

    private boolean groupsChanged = false;

    /** is this just a stub, or is all data loaded? */
    private boolean isDataLoaded = false;


    /**
     * Construct a Group from a given context and tablerow
     * 
     * @param context
     * @param row
     */
    Group(Context context, TableRow row) throws SQLException
    {
        super(context);

        // Ensure that my TableRow is typed.
        if (null == row.getTable())
            row.setTable("epersongroup");

        myRow = row;

        // Cache ourselves
        context.cache(this, row.getIntColumn("eperson_group_id"));

        clearDetails();
    }

    /**
     * Populate Group with eperson and group objects
     * 
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
                TableRowIterator tri = DatabaseManager.queryTable(ourContext,"eperson",
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
                        EPerson fromCache = (EPerson) ourContext.fromCache(
                                EPerson.class, r.getIntColumn("eperson_id"));

                        if (fromCache != null)
                        {
                            epeople.add(fromCache);
                        }
                        else
                        {
                            epeople.add(new EPerson(ourContext, r));
                        }
                    }
                }
                finally
                {
                    // close the TableRowIterator to free up resources
                    if (tri != null)
                    {
                        tri.close();
                    }
                }

                // now get Group objects
                tri = DatabaseManager.queryTable(ourContext,"epersongroup",
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
                        Group fromCache = (Group) ourContext.fromCache(Group.class,
                                r.getIntColumn("eperson_group_id"));

                        if (fromCache != null)
                        {
                            groups.add(fromCache);
                        }
                        else
                        {
                            groups.add(new Group(ourContext, r));
                        }
                    }
                }
                finally
                {
                    // close the TableRowIterator to free up resources
                    if (tri != null)
                    {
                        tri.close();
                    }
                }

            }
            catch (Exception e)
            {
                throw new IllegalStateException(e);
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

        context.addEvent(new Event(Event.CREATE, Constants.GROUP, g.getID(),
                null, g.getIdentifiers(context)));

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
        return getMetadataFirstValue(MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
    }

    /**
     * set name of group
     * 
     * @param name
     *            new group name
     */
    public void setName(String name) {
        setMetadataSingleValue(MetadataSchema.DC_SCHEMA, "title", null, null, name);
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

        ourContext.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail(), getIdentifiers(ourContext)));
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
        // and don't add itself
        if (isMember(g) || getID()==g.getID())
        {
            return;
        }

        groups.add(g);
        groupsChanged = true;

        ourContext.addEvent(new Event(Event.ADD, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName(), getIdentifiers(ourContext)));
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
            ourContext.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.EPERSON, e.getID(), e.getEmail(), getIdentifiers(ourContext)));
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
            ourContext.addEvent(new Event(Event.REMOVE, Constants.GROUP, getID(), Constants.GROUP, g.getID(), g.getName(), getIdentifiers(ourContext)));
        }
    }

    /**
     * check to see if an eperson is a direct member.
     * If the eperson is a member via a subgroup will be returned <code>false</code>
     * 
     * @param e
     *            eperson to check membership
     */
    public boolean isMember(EPerson e)
    {
        // special, group 0 is anonymous
        if (getID() == Group.ANONYMOUS_ID)
        {
            return true;
        }

        loadData(); // make sure Group has data loaded

        return epeople.contains(e);
    }

    /**
     * Check to see if g is a direct group member.
     * If g is a subgroup via another group will be returned <code>false</code>
     * 
     * @param g
     *            group to check
     */
    public boolean isMember(Group g)
    {
        loadData(); // make sure Group has data loaded

        return groups.contains(g);
    }

    /**
     * fast check to see if the current EPerson is a member of a Group.  Does
     * database lookup without instantiating all of the EPerson objects and is
     * thus a static method.
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

        EPerson currentuser = c.getCurrentUser();

        return epersonInGroup(c, groupid, currentuser);
    }

    /**
     * Fast check to see if a given EPerson is a member of a Group.
     * Does database lookup without instantiating all of the EPerson objects and
     * is thus a static method.
     *
     * @param c current DSpace context.
     * @param eperson candidate to test for membership.
     * @param groupid group whose membership is to be tested.
     * @return true if {@link eperson} is a member of Group {@link groupid}.
     * @throws SQLException passed through
     */
    public static boolean isMember(Context c, EPerson eperson, int groupid)
            throws SQLException
    {
        // Every EPerson is a member of Anonymous
        if (groupid == 0)
        {
            return true;
        }

        return epersonInGroup(c, groupid, eperson);
    }

    /**
     * Get all of the groups that an eperson is a member of.
     * 
     * @param c
     * @param e
     * @throws SQLException
     */
    public static Group[] allMemberGroups(Context c, EPerson e)
            throws SQLException
    {
        List<Group> groupList = new ArrayList<Group>();

        Set<Integer> myGroups = allMemberGroupIDs(c, e);
        // now convert those Integers to Groups
        Iterator<Integer> i = myGroups.iterator();

        while (i.hasNext())
        {
            groupList.add(Group.find(c, (i.next()).intValue()));
        }

        return groupList.toArray(new Group[groupList.size()]);
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
        Set<Integer> groupIDs = new HashSet<Integer>();
        
        if (e != null)
        {
            // two queries - first to get groups eperson is a member of
            // second query gets parent groups for groups eperson is a member of

            TableRowIterator tri = DatabaseManager.queryTable(c,
                    "epersongroup2eperson",
                    "SELECT * FROM epersongroup2eperson WHERE eperson_id= ?", e
                            .getID());

            try
            {
                while (tri.hasNext())
                {
                    TableRow row = tri.next();

                    int childID = row.getIntColumn("eperson_group_id");

                    groupIDs.add(Integer.valueOf(childID));
                }
            }
            finally
            {
                // close the TableRowIterator to free up resources
                if (tri != null)
                {
                    tri.close();
                }
            }
        }
        // Also need to get all "Special Groups" user is a member of!
        // Otherwise, you're ignoring the user's membership to these groups!
        // However, we only do this is we are looking up the special groups
        // of the current user, as we cannot look up the special groups
        // of a user who is not logged in.
        if ((c.getCurrentUser() == null) || (((c.getCurrentUser() != null) && (c.getCurrentUser().getID() == e.getID()))))
        {
            Group[] specialGroups = c.getSpecialGroups();
            for(Group special : specialGroups)
            {
                groupIDs.add(Integer.valueOf(special.getID()));
            }
        }

        // all the users are members of the anonymous group 
        groupIDs.add(Integer.valueOf(0));
        
        // now we have all owning groups, also grab all parents of owning groups
        // yes, I know this could have been done as one big query and a union,
        // but doing the Oracle port taught me to keep to simple SQL!

        StringBuilder groupQuery = new StringBuilder();
        groupQuery.append("SELECT * FROM group2groupcache WHERE ");

        Iterator<Integer> i = groupIDs.iterator();

        // Build a list of query parameters
        Object[] parameters = new Object[groupIDs.size()];
        int idx = 0;
        while (i.hasNext())
        {
            int groupID = (i.next()).intValue();

            parameters[idx++] = Integer.valueOf(groupID);
            
            groupQuery.append("child_id= ? ");
            if (i.hasNext())
            {
                groupQuery.append(" OR ");
            }
        }

        // was member of at least one group
        // NOTE: even through the query is built dynamically, all data is
        // separated into the parameters array.
        TableRowIterator tri = DatabaseManager.queryTable(c, "group2groupcache",
                groupQuery.toString(),
                parameters);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                int parentID = row.getIntColumn("parent_id");

                groupIDs.add(Integer.valueOf(parentID));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
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
        Iterator<Integer> i = myEpeople.iterator();

        while (i.hasNext())
        {
            epersonList.add(EPerson.find(c, (i.next()).intValue()));
        }

        return epersonList.toArray(new EPerson[epersonList.size()]);
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

                groupIDs.add(Integer.valueOf(childID));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        // now we have all the groups (including this one)
        // it is time to find all the EPeople who belong to those groups
        // and filter out all duplicates

        Object[] parameters = new Object[groupIDs.size()+1];
        int idx = 0;
        Iterator<Integer> i = groupIDs.iterator();

        // don't forget to add the current group to this query!
        parameters[idx++] = Integer.valueOf(g.getID());

        StringBuilder epersonQuery = new StringBuilder();
        epersonQuery.append("SELECT * FROM epersongroup2eperson WHERE ");
        epersonQuery.append("eperson_group_id= ? ");

        if (i.hasNext())
        {
            epersonQuery.append(" OR ");
        }
        
        while (i.hasNext())
        {
            int groupID = (i.next()).intValue();
            parameters[idx++] = Integer.valueOf(groupID);
            
            epersonQuery.append("eperson_group_id= ? ");
            if (i.hasNext())
            {
                epersonQuery.append(" OR ");
            }
        }

        //get all the EPerson IDs
        // Note: even through the query is dynamically built all data is separated
        // into the parameters array.
        tri = DatabaseManager.queryTable(c, "epersongroup2eperson",
                epersonQuery.toString(),
                parameters);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                int epersonID = row.getIntColumn("eperson_id");

                epeopleIDs.add(Integer.valueOf(epersonID));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        return epeopleIDs;
    }

    private static boolean epersonInGroup(Context c, int groupID, EPerson e)
            throws SQLException
    {
        Set<Integer> groupIDs = Group.allMemberGroupIDs(c, e);

        return groupIDs.contains(Integer.valueOf(groupID));
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
     * @return the named Group, or null if not found
     */
    public static Group findByName(Context context, String name)
            throws SQLException
    {
        String query = "select * from epersongroup e " +
                "LEFT JOIN metadatavalue m on (m.resource_id = e.eperson_group_id and m.resource_type_id = ? and m.metadata_field_id = ?) " +
                "where ";
        if(DatabaseManager.isOracle()) {
            query += " dbms_lob.substr(m.text_value) = ?";
        }else{
            query += " m.text_value = ?";

        }
        TableRow row = DatabaseManager.querySingle(context, query,
                Constants.GROUP,
                MetadataField.findByElement(context, MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID(),
                name
        );

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
                // Force the row to be a Group row, as it has all epersongroup
                // columns but also some others, so could not be typed using
                // querySingleTable.
                row.setTable("epersongroup");
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
            s = "e.eperson_group_id";

            break;

        case NAME:
            s = "m_text_value";

            break;

        default:
            s = "m_text_value";
        }

        // NOTE: The use of 's' in the order by clause can not cause an SQL 
        // injection because the string is derived from constant values above.
        TableRowIterator rows = DatabaseManager.query(
                context,
                "select e.* from epersongroup e " +
                        "LEFT JOIN metadatavalue m on (m.resource_id = e.eperson_group_id and m.resource_type_id = ? and m.metadata_field_id = ?) " +
                        "order by ?",
                Constants.GROUP,
                MetadataField.findByElement(context, MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID(),
                s
        );


        try
        {
            List<TableRow> gRows = rows.toList();

            Group[] groups = new Group[gRows.size()];

            for (int i = 0; i < gRows.size(); i++)
            {
                TableRow row = gRows.get(i);

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
            {
                rows.close();
            }
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
		queryBuf.append("SELECT * FROM epersongroup " +
                "LEFT JOIN metadatavalue m on (m.resource_id = epersongroup.eperson_group_id and m.resource_type_id = ? and m.metadata_field_id = ?) " +
                "WHERE LOWER(m.text_value) LIKE LOWER(?) OR eperson_group_id = ? ");

        if(DatabaseManager.isOracle()){
            queryBuf.append(" ORDER BY cast(m.text_value as varchar2(128))");
        }else{
            queryBuf.append(" ORDER BY m.text_value");
        }
        queryBuf.append(" ASC");

        // Add offset and limit restrictions - Oracle requires special code
        if (DatabaseManager.isOracle())
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
                {
                    limit += offset;
                }
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
            {
                queryBuf.append(" LIMIT ? ");
            }

            if (offset > 0)
            {
                queryBuf.append(" OFFSET ? ");
            }
        }

        String dbquery = queryBuf.toString();

        // When checking against the eperson-id, make sure the query can be made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e) {
			int_param = Integer.valueOf(-1);
		}

        // Create the parameter array, including limit and offset if part of the query

        int metadataFieldId = MetadataField.findByElement(context, MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID();

        Object[] paramArr = new Object[]{Constants.GROUP, metadataFieldId, params, int_param};
        if (limit > 0 && offset > 0)
        {
            paramArr = new Object[]{Constants.GROUP, metadataFieldId,params, int_param, limit, offset};
        }
        else if (limit > 0)
        {
            paramArr = new Object[]{Constants.GROUP, metadataFieldId,params, int_param, limit};
        }
        else if (offset > 0)
        {
            paramArr = new Object[]{Constants.GROUP, metadataFieldId,params, int_param, offset};
        }

        TableRowIterator rows =
			DatabaseManager.query(context, dbquery, paramArr);

        try
        {
            List<TableRow> groupRows = rows.toList();
            Group[] groups = new Group[groupRows.size()];

            for (int i = 0; i < groupRows.size(); i++)
            {
                TableRow row = groupRows.get(i);

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
            {
                rows.close();
            }
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
     * @return the number of groups matching the query
     */
    public static int searchResultCount(Context context, String query)
    	throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
		String dbquery = "SELECT count(*) as gcount FROM epersongroup " +
                "LEFT JOIN metadatavalue m on (m.resource_id = epersongroup.eperson_group_id and m.resource_type_id = ? and m.metadata_field_id = ?) " +
                "WHERE LOWER(m.text_value) LIKE LOWER(?) OR eperson_group_id = ? ";
		
		// When checking against the eperson-id, make sure the query can be made into a number
		Integer int_param;
		try {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e) {
			int_param = Integer.valueOf(-1);
		}
		
		// Get all the epeople that match the query
		TableRow row = DatabaseManager.querySingle(
                context,
                dbquery,
                new Object[] {
                        Constants.GROUP,
                        MetadataField.findByElement(context, MetadataSchema.find(context, MetadataSchema.DC_SCHEMA).getSchemaID(), "title", null).getFieldID(),
                        params,
                        int_param
                }
        );
		
		// use getIntColumn for Oracle count data
		Long count;
        if (DatabaseManager.isOracle())
        {
            count = Long.valueOf(row.getIntColumn("gcount"));
        }
        else  //getLongColumn works for postgres
        {
            count = Long.valueOf(row.getLongColumn("gcount"));
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

        ourContext.addEvent(new Event(Event.DELETE, Constants.GROUP, getID(), getName(), getIdentifiers(ourContext)));

        // Remove from cache
        ourContext.removeCached(this, getID());

        // Remove any ResourcePolicies that reference this group
        AuthorizeManager.removeGroupPolicies(ourContext, getID());

        // Remove any group memberships first
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM EPersonGroup2EPerson WHERE eperson_group_id= ? ",
                getID());

        // remove any group2groupcache entries
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM group2groupcache WHERE parent_id= ? OR child_id= ? ",
                getID(),getID());

        // Now remove any group2group assignments
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM group2group WHERE parent_id= ? OR child_id= ? ",
                getID(),getID());

        // Delete the Dublin Core
        removeMetadataFromDatabase();

        // don't forget the new table
        deleteEpersonGroup2WorkspaceItem();

        // Remove ourself
        DatabaseManager.delete(ourContext, myRow);

        epeople.clear();

        log.info(LogManager.getHeader(ourContext, "delete_group", "group_id="
                + getID()));

    }

    /**
     * @throws SQLException
     */
    private void deleteEpersonGroup2WorkspaceItem() throws SQLException
    {
        DatabaseManager.updateQuery(ourContext,
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
     * Return Group members of a Group.
     */
    public Group[] getMemberGroups()
    {
        loadData(); // make sure all data is loaded

        Group[] myArray = new Group[groups.size()];
        myArray = (Group[]) groups.toArray(myArray);

        return myArray;
    }
    
    /**
     * Return true if group has no direct or indirect members
     */
    public boolean isEmpty()
    {
        loadData(); // make sure all data is loaded
        
        // the only fast check available is on epeople... 
        boolean hasMembers = (epeople.size() != 0);
        
        if (hasMembers)
        {
            return false;
        }
        else
        {
            // well, groups is never null...
            for (Group subGroup : groups){
                hasMembers = !subGroup.isEmpty();
                if (hasMembers){
                    return false;
                }
            }
            return !hasMembers;
        }
    }

    /**
     * Update the group - writing out group object and EPerson list if necessary
     */
    public void update() throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation
        DatabaseManager.update(ourContext, myRow);

        if (modifiedMetadata)
        {
            updateMetadata();
            clearDetails();
        }

        // Redo eperson mappings if they've changed
        if (epeopleChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(ourContext,
                    "delete from epersongroup2eperson where eperson_group_id= ? ",
                    getID());

            // Add new mappings
            Iterator<EPerson> i = epeople.iterator();

            while (i.hasNext())
            {
                EPerson e = i.next();

                TableRow mappingRow = DatabaseManager.row("epersongroup2eperson");
                mappingRow.setColumn("eperson_id", e.getID());
                mappingRow.setColumn("eperson_group_id", getID());
                DatabaseManager.insert(ourContext, mappingRow);
            }

            epeopleChanged = false;
        }

        // Redo Group mappings if they've changed
        if (groupsChanged)
        {
            // Remove any existing mappings
            DatabaseManager.updateQuery(ourContext,
                    "delete from group2group where parent_id= ? ",
                    getID());

            // Add new mappings
            Iterator<Group> i = groups.iterator();

            while (i.hasNext())
            {
                Group g = i.next();

                TableRow mappingRow = DatabaseManager.row("group2group");
                mappingRow.setColumn("parent_id", getID());
                mappingRow.setColumn("child_id", g.getID());
                DatabaseManager.insert(ourContext, mappingRow);
            }

            // groups changed, now change group cache
            rethinkGroupCache();

            groupsChanged = false;
        }

        log.info(LogManager.getHeader(ourContext, "update_group", "group_id="
                + getID()));
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Group as
     * this object, <code>false</code> otherwise
     * 
     * @param obj
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same group
     *         as this object
     */
     @Override
     public boolean equals(Object obj)
     {
         if (obj == null)
         {
             return false;
         }
         if (getClass() != obj.getClass())
         {
             return false;
         }
         final Group other = (Group) obj;
         if(this.getID() != other.getID())
         {
             return false;
         }
         return true;
     }

     @Override
     public int hashCode()
     {
         int hash = 7;
         hash = 59 * hash + (this.myRow != null ? this.myRow.hashCode() : 0);
         return hash;
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
        TableRowIterator tri = DatabaseManager.queryTable(ourContext, "group2group",
                "SELECT * FROM group2group");

        Map<Integer,Set<Integer>> parents = new HashMap<Integer,Set<Integer>>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = (TableRow) tri.next();

                Integer parentID = Integer.valueOf(row.getIntColumn("parent_id"));
                Integer childID = Integer.valueOf(row.getIntColumn("child_id"));

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
            {
                tri.close();
            }
        }

        // now parents is a hash of all of the IDs of groups that are parents
        // and each hash entry is a hash of all of the IDs of children of those
        // parent groups
        // so now to establish all parent,child relationships we can iterate
        // through the parents hash
        for (Map.Entry<Integer, Set<Integer>> parent : parents.entrySet())
        {
            Set<Integer> myChildren = getChildren(parents, parent.getKey());
            parent.getValue().addAll(myChildren);
        }

        // empty out group2groupcache table
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM group2groupcache WHERE id >= 0");

        // write out new one
        for (Map.Entry<Integer, Set<Integer>> parent : parents.entrySet())
        {
            int parentID = parent.getKey().intValue();

            for (Integer child : parent.getValue())
            {
                TableRow row = DatabaseManager.row("group2groupcache");

                row.setColumn("parent_id", parentID);
                row.setColumn("child_id", child);

                DatabaseManager.insert(ourContext, row);
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
        {
            return myChildren;
        }

        // got this far, so we must have children
        Set<Integer> children =  parents.get(parent);

        // now iterate over all of the children
        Iterator<Integer> i = children.iterator();

        while (i.hasNext())
        {
            Integer childID = i.next();

            // add this child's ID to our return set
            myChildren.add(childID);

            // and now its children
            myChildren.addAll(getChildren(parents, childID));
        }

        return myChildren;
    }
    
    public DSpaceObject getParentObject() throws SQLException
    {
        // could a collection/community administrator manage related groups?
        // check before the configuration options could give a performance gain
        // if all group management are disallowed
        if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup()
                || AuthorizeConfiguration.canCollectionAdminManageSubmitters()
                || AuthorizeConfiguration.canCollectionAdminManageWorkflows()
                || AuthorizeConfiguration.canCommunityAdminManageAdminGroup()
                || AuthorizeConfiguration
                        .canCommunityAdminManageCollectionAdminGroup()
                || AuthorizeConfiguration
                        .canCommunityAdminManageCollectionSubmitters()
                || AuthorizeConfiguration
                        .canCommunityAdminManageCollectionWorkflows())
        {
            // is this a collection related group?
            TableRow qResult = DatabaseManager
                    .querySingle(
                            ourContext,
                            "SELECT collection_id, workflow_step_1, workflow_step_2, " +
                            " workflow_step_3, submitter, admin FROM collection "
                                    + " WHERE workflow_step_1 = ? OR "
                                    + " workflow_step_2 = ? OR "
                                    + " workflow_step_3 = ? OR "
                                    + " submitter =  ? OR " + " admin = ?",
                            getID(), getID(), getID(), getID(), getID());
            if (qResult != null)
            {
                Collection collection = Collection.find(ourContext, qResult
                        .getIntColumn("collection_id"));
                
                if ((qResult.getIntColumn("workflow_step_1") == getID() ||
                        qResult.getIntColumn("workflow_step_2") == getID() ||
                        qResult.getIntColumn("workflow_step_3") == getID()))
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageWorkflows())
                    {
                        return collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionWorkflows())
                    {
                        return collection.getParentObject();
                    }
                }
                if (qResult.getIntColumn("submitter") == getID())
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageSubmitters())
                    {
                        return collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionSubmitters())
                    {
                        return collection.getParentObject();
                    }
                }
                if (qResult.getIntColumn("admin") == getID())
                {
                    if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup())
                    {
                        return collection;
                    }
                    else if (AuthorizeConfiguration.canCommunityAdminManageCollectionAdminGroup())
                    {
                        return collection.getParentObject();
                    }
                }
            }
            // is the group related to a community and community administrator allowed
            // to manage it?
            else if (AuthorizeConfiguration.canCommunityAdminManageAdminGroup())
            {
                qResult = DatabaseManager.querySingle(ourContext,
                        "SELECT community_id FROM community "
                                + "WHERE admin = ?", getID());

                if (qResult != null)
                {
                    Community community = Community.find(ourContext, qResult
                            .getIntColumn("community_id"));
                    return community;
                }
            }
        }
        return null;
    }

    @Override
    public void updateLastModified()
    {

    }

    /**
     * Main script used to set the group names for anonymous group & admin group, only to be called once on DSpace fresh_install
     * @param args not used
     * @throws SQLException database exception
     * @throws AuthorizeException should not occur since we disable authentication for this method.
     */
    public static void main(String[] args) throws SQLException, AuthorizeException {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        initDefaultGroupNames(context);

        //Clear the events to avoid the consumers which aren't needed at this time
        context.getEvents().clear();
        context.complete();
    }

    /**
     * Initializes the group names for anymous & administrator
     * @param context the dspace context
     * @throws SQLException database exception
     * @throws AuthorizeException
     */
    public static void initDefaultGroupNames(Context context) throws SQLException, AuthorizeException {
        // Check for Anonymous group. If not found, create it
        Group anonymousGroup = Group.find(context, ANONYMOUS_ID);
        if(anonymousGroup==null)
        {
            anonymousGroup = Group.create(context);
        }
        anonymousGroup.setName("Anonymous");
        anonymousGroup.update();

        // Check for Administrator group. If not found, create it
        Group adminGroup = Group.find(context, ADMIN_ID);
        if(adminGroup==null)
        {
            adminGroup = Group.create(context);
        }
        adminGroup.setName("Administrator");
        adminGroup.update();
    }
}
