/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 * 
 */

package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
 * Class representing a campus unit.
 * 
 * @author Ben Wallberg
 */

public class Unit extends DSpaceObject {

    // findAll sortby types
    public static final int ID = 0; // sort by ID

    public static final int NAME = 1; // sort by NAME (default)

    /** log4j logger */
    private static Logger log = Logger.getLogger(Unit.class);

    /** Our context */
    private Context myContext;

    /** The row in the table representing this object */
    private TableRow myRow;

    /** is this just a stub, or is all data loaded? */
    private boolean isDataLoaded = false;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /** The groups in this bundle */
    private List<Group> groups;

    /**
     * Construct a Unit from a given context and tablerow
     * 
     * @param context
     * @param row
     */
    Unit(Context context, TableRow row) throws SQLException
    {
        myContext = context;
        myRow = row;
        groups = new ArrayList<Group>();

        // Get groups
        TableRowIterator tri = DatabaseManager.queryTable(
                myContext, "epersongroup",
                "SELECT epersongroup.* FROM epersongroup, epersongroup2unit WHERE "
                        + "epersongroup2unit.eperson_group_id=epersongroup.eperson_group_id AND "
                        + "epersongroup2unit.unit_id= ? ",
                myRow.getIntColumn("unit_id"));

        try
        {
            while (tri.hasNext())
            {
                TableRow r = (TableRow) tri.next();

                // First check the cache
                Group fromCache = (Group) context.fromCache(
                        Group.class, r.getIntColumn("eperson_group_id"));

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

        // Cache ourselves
        context.cache(this, row.getIntColumn("unit_id"));

        modifiedMetadata = false;
        clearDetails();
    }

    /**
     * Create a new unit
     * 
     * @param context
     *            DSpace context object
     */
    public static Unit create(Context context) throws SQLException,
            AuthorizeException
    {
        // FIXME - authorization?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create a Unit");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "unit");

        Unit g = new Unit(context, row);

        log.info(LogManager.getHeader(context, "create_unit", "unit_id="
                + g.getID()));

        return g;
    }

    /**
     * get the ID of the unit object
     * 
     * @return id
     */
    public int getID()
    {
        return myRow.getIntColumn("unit_id");
    }

    /**
     * get name of unit
     * 
     * @return name
     */
    public String getName()
    {
        return myRow.getStringColumn("name");
    }

    /**
     * set name of unit
     * 
     * @param name
     *            new unit name
     */
    public void setName(String name)
    {
        myRow.setColumn("name", name);
        modifiedMetadata = true;
        addDetails("name");
    }

    /**
     * find the unit by its ID
     * 
     * @param context
     * @param id
     */
    public static Unit find(Context context, int id) throws SQLException
    {
        // First check the cache
        Unit fromCache = (Unit) context.fromCache(Unit.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "unit", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new Unit(context, row);
        }
    }

    /**
     * Find the unit by its name - assumes name is unique
     * 
     * @param context
     * @param name
     * 
     * @return Unit
     */
    public static Unit findByName(Context context, String name)
            throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context, "unit",
                "name", name);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            Unit fromCache = (Unit) context.fromCache(Unit.class, row
                    .getIntColumn("unit_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new Unit(context, row);
            }
        }
    }

    /**
     * Finds all units in the site
     * 
     * @param context
     *            DSpace context
     * @param sortField
     *            field to sort by -- Unit.ID or Unit.NAME
     * 
     * @return array of all units in the site
     */
    public static Unit[] findAll(Context context, int sortField)
            throws SQLException
    {
        String s;

        switch (sortField)
        {
        case ID:
            s = "unit_id";

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
        		context, "unit",
                "SELECT * FROM unit ORDER BY "+s);

        try
        {
            List gRows = rows.toList();

            Unit[] units = new Unit[gRows.size()];

            for (int i = 0; i < gRows.size(); i++)
            {
                TableRow row = (TableRow) gRows.get(i);

                // First check the cache
                Unit fromCache = (Unit) context.fromCache(Unit.class, row
                        .getIntColumn("unit_id"));

                if (fromCache != null)
                {
                    units[i] = fromCache;
                }
                else
                {
                    units[i] = new Unit(context, row);
                }
            }

            return units;
        }
        finally
        {
            if (rows != null)
                rows.close();
        }
    }
    
    
    /**
     * Find the units that match the search query across unit_id or name
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return array of Unit objects
     */
    public static Unit[] search(Context context, String query)
    		throws SQLException
	{
	    return search(context, query, -1, -1);
	}
    
    /**
     * Find the units that match the search query across unit_id or name
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
     * @return array of Unit objects
     */
    public static Unit[] search(Context context, String query, int offset, int limit)
    		throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
		queryBuf.append("SELECT * FROM unit WHERE LOWER(name) LIKE LOWER(?) OR unit_id = ? ORDER BY name ASC ");
		
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

        // When checking against the unit-id, make sure the query can be made into a number
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
            List unitRows = rows.toList();
            Unit[] units = new Unit[unitRows.size()];

            for (int i = 0; i < unitRows.size(); i++)
            {
                TableRow row = (TableRow) unitRows.get(i);

                // First check the cache
                Unit fromCache = (Unit) context.fromCache(Unit.class, row
                        .getIntColumn("unit_id"));

                if (fromCache != null)
                {
                    units[i] = fromCache;
                }
                else
                {
                    units[i] = new Unit(context, row);
                }
            }
            return units;
        }
        finally
        {
            if (rows != null)
                rows.close();
        }
	}

    /**
     * Returns the total number of units returned by a specific query, without the overhead 
     * of creating the Unit objects to store the results.
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return the number of units mathching the query
     */
    public static int searchResultCount(Context context, String query)
    	throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
		String dbquery = "SELECT count(*) as gcount FROM unit WHERE LOWER(name) LIKE LOWER(?) OR unit_id = ? ";
		
		// When checking against the unit-id, make sure the query can be made into a number
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
     * Delete a unit
     * 
     */
    public void delete() throws SQLException
    {
        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove groups
        Group[] gs = getGroups();

        for (int i = 0; i < gs.length; i++)
        {
            removeGroup(gs[i]);
        }

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "delete_unit", "unit_id="
                + getID()));
    }

    /**
     * Update the unit - writing out unit object and Unit list if necessary
     */
    public void update() throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation
        DatabaseManager.update(myContext, myRow);

        if (modifiedMetadata)
        {
            myContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.UNIT, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }

        log.info(LogManager.getHeader(myContext, "update_unit", "unit_id="
                + getID()));
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Unit as
     * this object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same unit
     *         as this object
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof Unit))
        {
            return false;
        }

        return (getID() == ((Unit) other).getID());
    }

    public int getType()
    {
        return Constants.UNIT;
    }

    public String getHandle()
    {
        return null;
    }

    /**
     * Get the groups this unit maps to
     * 
     * @return array of <code>Group</code> s this unit maps to
     * @throws SQLException
     */
    public Group[] getGroups() throws SQLException
    {
        // Get the group table rows
        TableRowIterator tri = DatabaseManager.queryTable(myContext, "epersongroup",
                "SELECT epersongroup.* FROM epersongroup, epersongroup2unit WHERE " + 
                "epersongroup.eperson_group_id=epersongroup2unit.eperson_group_id AND " +
                "epersongroup2unit.unit_id= ? ",
                 myRow.getIntColumn("unit_id"));

        // Build a list of Group objects
        List<Group> groups = new ArrayList<Group>();
        try
        {
            while (tri.hasNext())
            {
                TableRow r = tri.next();

                // First check the cache
                Group fromCache = (Group) myContext.fromCache(Group.class, r
                        .getIntColumn("eperson_group_id"));

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

        Group[] groupArray = new Group[groups.size()];
        groupArray = (Group[]) groups.toArray(groupArray);

        return groupArray;
    }


    /**
     * Add an existing group to this unit
     * 
     * @param b
     *            the group to add
     */
    public void addGroup(Group b) throws SQLException
    {
        log.info(LogManager.getHeader(myContext, "add_group", "unit_id="
                + getID() + ",eperson_group_id=" + b.getID()));

        // First check that the group isn't already in the list
        for (int i = 0; i < groups.size(); i++)
        {
            Group existing = (Group) groups.get(i);

            if (b.getID() == existing.getID())
            {
                // Group is already there; no change
                return;
            }
        }

        // Add the group object
        groups.add(b);

        // Add the mapping row to the database
        TableRow mappingRow = DatabaseManager.create(myContext,
                "epersongroup2unit");
        mappingRow.setColumn("unit_id", getID());
        mappingRow.setColumn("eperson_group_id", b.getID());
        DatabaseManager.update(myContext, mappingRow);
    }


    /**
     * Remove a group from this unit
     * 
     * @param b
     *            the group to remove
     */
    public void removeGroup(Group b) throws SQLException
    {
        log.info(LogManager.getHeader(myContext, "remove_group",
                "unit_id=" + getID() + ",eperson_group_id=" + b.getID()));

        // Remove from internal list of groups
        ListIterator li = groups.listIterator();

        while (li.hasNext())
        {
            Group existing = (Group) li.next();

            if (b.getID() == existing.getID())
            {
                // We've found the group to remove
                li.remove();
            }
        }

        // Delete the mapping row
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM epersongroup2unit WHERE unit_id= ? "+
                "AND eperson_group_id= ? ", 
                getID(), b.getID());

    }

}
