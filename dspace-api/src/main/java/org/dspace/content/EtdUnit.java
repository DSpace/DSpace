/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 * 
 */

package org.dspace.content;

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
 * Class representing an ETD department as seen in the Proquest metadata element
 * /DISS_submission/DISS_description/DISS_institution/DISS_inst_contact
 * 
 * @author Ben Wallberg
 */

public class EtdUnit extends DSpaceObject {

    // findAll sortby types
    public static final int ID = 0; // sort by ID

    public static final int NAME = 1; // sort by NAME (default)

    /** log4j logger */
    private static Logger log = Logger.getLogger(EtdUnit.class);

    /** Our context */
    private Context myContext;

    /** The row in the table representing this object */
    private TableRow myRow;

    /** is this just a stub, or is all data loaded? */
    private boolean isDataLoaded = false;

    /** Flag set when metadata is modified, for events */
    private boolean modifiedMetadata;

    /** The mapped collections  */
    private List<Collection> collections;

    /**
     * Construct a EtdUnit from a given context and tablerow
     * 
     * @param context
     * @param row
     */
    EtdUnit(Context context, TableRow row) throws SQLException
    {
        myContext = context;
        myRow = row;
        collections = new ArrayList<Collection>();

        // Get collections
        TableRowIterator tri = DatabaseManager.queryTable(
                myContext, "collection",
                "SELECT collection.* FROM collection, collection2etdunit WHERE "
                        + "collection2etdunit.collection_id=collection.collection_id AND "
                        + "collection2etdunit.etdunit_id= ? ",
                myRow.getIntColumn("etdunit_id"));

        try
        {
            while (tri.hasNext())
            {
                TableRow r = (TableRow) tri.next();

                // First check the cache
                Collection fromCache = (Collection) context.fromCache(
                        Collection.class, r.getIntColumn("collection_id"));

                if (fromCache != null)
                {
                    collections.add(fromCache);
                }
                else
                {
                    collections.add(new Collection(myContext, r));
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
        context.cache(this, row.getIntColumn("etdunit_id"));

        modifiedMetadata = false;
        clearDetails();
    }

    /**
     * Create a new etdunit
     * 
     * @param context
     *            DSpace context object
     */
    public static EtdUnit create(Context context) throws SQLException,
            AuthorizeException
    {
        // FIXME - authorization?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create a EtdUnit");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context, "etdunit");

        EtdUnit g = new EtdUnit(context, row);

        log.info(LogManager.getHeader(context, "create_etdunit", "etdunit_id="
                + g.getID()));

        return g;
    }

    /**
     * get the ID of the etdunit object
     * 
     * @return id
     */
    public int getID()
    {
        return myRow.getIntColumn("etdunit_id");
    }

    /**
     * get name of etdunit
     * 
     * @return name
     */
    public String getName()
    {
        return myRow.getStringColumn("name");
    }

    /**
     * set name of etdunit
     * 
     * @param name
     *            new etdunit name
     */
    public void setName(String name)
    {
        myRow.setColumn("name", name);
        modifiedMetadata = true;
        addDetails("name");
    }

    /**
     * find the etdunit by its ID
     * 
     * @param context
     * @param id
     */
    public static EtdUnit find(Context context, int id) throws SQLException
    {
        // First check the cache
        EtdUnit fromCache = (EtdUnit) context.fromCache(EtdUnit.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "etdunit", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new EtdUnit(context, row);
        }
    }

    /**
     * Find the etdunit by its name - assumes name is unique
     * 
     * @param context
     * @param name
     * 
     * @return EtdUnit
     */
    public static EtdUnit findByName(Context context, String name)
            throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context, "etdunit",
                "name", name);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
            EtdUnit fromCache = (EtdUnit) context.fromCache(EtdUnit.class, row
                    .getIntColumn("etdunit_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new EtdUnit(context, row);
            }
        }
    }

    /**
     * Finds all etdunits in the site
     * 
     * @param context
     *            DSpace context
     * @param sortField
     *            field to sort by -- EtdUnit.ID or EtdUnit.NAME
     * 
     * @return array of all etdunits in the site
     */
    public static EtdUnit[] findAll(Context context, int sortField)
            throws SQLException
    {
        String s;

        switch (sortField)
        {
        case ID:
            s = "etdunit_id";

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
        		context, "etdunit",
                "SELECT * FROM etdunit ORDER BY "+s);

        try
        {
            List gRows = rows.toList();

            EtdUnit[] etdunits = new EtdUnit[gRows.size()];

            for (int i = 0; i < gRows.size(); i++)
            {
                TableRow row = (TableRow) gRows.get(i);

                // First check the cache
                EtdUnit fromCache = (EtdUnit) context.fromCache(EtdUnit.class, row
                        .getIntColumn("etdunit_id"));

                if (fromCache != null)
                {
                    etdunits[i] = fromCache;
                }
                else
                {
                    etdunits[i] = new EtdUnit(context, row);
                }
            }

            return etdunits;
        }
        finally
        {
            if (rows != null)
                rows.close();
        }
    }
    
    
    /**
     * Find the etdunits that match the search query across etdunit_id or name
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return array of EtdUnit objects
     */
    public static EtdUnit[] search(Context context, String query)
    		throws SQLException
	{
	    return search(context, query, -1, -1);
	}
    
    /**
     * Find the etdunits that match the search query across etdunit_id or name
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
     * @return array of EtdUnit objects
     */
    public static EtdUnit[] search(Context context, String query, int offset, int limit)
    		throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
        StringBuffer queryBuf = new StringBuffer();
		queryBuf.append("SELECT * FROM etdunit WHERE LOWER(name) LIKE LOWER(?) OR etdunit_id = ? ORDER BY name ASC ");
		
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

        // When checking against the etdunit-id, make sure the query can be made into a number
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
            List etdunitRows = rows.toList();
            EtdUnit[] etdunits = new EtdUnit[etdunitRows.size()];

            for (int i = 0; i < etdunitRows.size(); i++)
            {
                TableRow row = (TableRow) etdunitRows.get(i);

                // First check the cache
                EtdUnit fromCache = (EtdUnit) context.fromCache(EtdUnit.class, row
                        .getIntColumn("etdunit_id"));

                if (fromCache != null)
                {
                    etdunits[i] = fromCache;
                }
                else
                {
                    etdunits[i] = new EtdUnit(context, row);
                }
            }
            return etdunits;
        }
        finally
        {
            if (rows != null)
                rows.close();
        }
	}

    /**
     * Returns the total number of etdunits returned by a specific query, without the overhead 
     * of creating the EtdUnit objects to store the results.
     * 
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * 
     * @return the number of etdunits mathching the query
     */
    public static int searchResultCount(Context context, String query)
    	throws SQLException
	{
		String params = "%"+query.toLowerCase()+"%";
		String dbquery = "SELECT count(*) as gcount FROM etdunit WHERE LOWER(name) LIKE LOWER(?) OR etdunit_id = ? ";
		
		// When checking against the etdunit-id, make sure the query can be made into a number
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
     * Delete a etdunit
     * 
     */
    public void delete() throws SQLException
    {
        // Remove from cache
        myContext.removeCached(this, getID());

        // Remove collections
        Collection[] gs = getCollections();

        for (int i = 0; i < gs.length; i++)
        {
            removeCollection(gs[i]);
        }

        // Remove ourself
        DatabaseManager.delete(myContext, myRow);

        log.info(LogManager.getHeader(myContext, "delete_etdunit", "etdunit_id="
                + getID()));
    }

    /**
     * Update the etdunit - writing out etdunit object and EtdUnit list if necessary
     */
    public void update() throws SQLException, AuthorizeException
    {
        // FIXME: Check authorisation
        DatabaseManager.update(myContext, myRow);

        if (modifiedMetadata)
        {
            myContext.addEvent(new Event(Event.MODIFY_METADATA, Constants.ETDUNIT, getID(), getDetails()));
            modifiedMetadata = false;
            clearDetails();
        }

        log.info(LogManager.getHeader(myContext, "update_etdunit", "etdunit_id="
                + getID()));
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same EtdUnit as
     * this object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same etdunit
     *         as this object
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof EtdUnit))
        {
            return false;
        }

        return (getID() == ((EtdUnit) other).getID());
    }

    public int getType()
    {
        return Constants.ETDUNIT;
    }

    public String getHandle()
    {
        return null;
    }

    /**
     * Get the collections this etdunit maps to
     * 
     * @return array of <code>Collection</code> s this etdunit maps to
     * @throws SQLException
     */
    public Collection[] getCollections() throws SQLException
    {
        // Get the collection table rows
        TableRowIterator tri = DatabaseManager.queryTable(myContext, "collection",
                "SELECT collection.* FROM collection, collection2etdunit WHERE " + 
                "collection.collection_id=collection2etdunit.collection_id AND " +
                "collection2etdunit.etdunit_id= ? ORDER BY name",
                 myRow.getIntColumn("etdunit_id"));

        // Build a list of Collection objects
        List<Collection> collections = new ArrayList<Collection>();
        try
        {
            while (tri.hasNext())
            {
                TableRow r = tri.next();

                // First check the cache
                Collection fromCache = (Collection) myContext.fromCache(Collection.class, r
                        .getIntColumn("collection_id"));

                if (fromCache != null)
                {
                    collections.add(fromCache);
                }
                else
                {
                    collections.add(new Collection(myContext, r));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        Collection[] collectionArray = new Collection[collections.size()];
        collectionArray = (Collection[]) collections.toArray(collectionArray);

        return collectionArray;
    }


    /**
     * Add an existing collection to this etdunit
     * 
     * @param b
     *            the collection to add
     */
    public void addCollection(Collection b) throws SQLException
    {
        log.info(LogManager.getHeader(myContext, "add_collection", "etdunit_id="
                + getID() + ",collection_id=" + b.getID()));

        // First check that the collection isn't already in the list
        for (int i = 0; i < collections.size(); i++)
        {
            Collection existing = (Collection) collections.get(i);

            if (b.getID() == existing.getID())
            {
                // Collection is already there; no change
                return;
            }
        }

        // Add the collection object
        collections.add(b);

        // Add the mapping row to the database
        TableRow mappingRow = DatabaseManager.create(myContext,
                "collection2etdunit");
        mappingRow.setColumn("etdunit_id", getID());
        mappingRow.setColumn("collection_id", b.getID());
        DatabaseManager.update(myContext, mappingRow);
    }


    /**
     * Remove a collection from this etdunit
     * 
     * @param b
     *            the collection to remove
     */
    public void removeCollection(Collection b) throws SQLException
    {
        log.info(LogManager.getHeader(myContext, "remove_collection",
                "etdunit_id=" + getID() + ",collection_id=" + b.getID()));

        // Remove from internal list of collections
        ListIterator li = collections.listIterator();

        while (li.hasNext())
        {
            Collection existing = (Collection) li.next();

            if (b.getID() == existing.getID())
            {
                // We've found the collection to remove
                li.remove();
            }
        }

        // Delete the mapping row
        DatabaseManager.updateQuery(myContext,
                "DELETE FROM collection2etdunit WHERE etdunit_id= ? "+
                "AND collection_id= ? ", 
                getID(), b.getID());

    }


    /**
     * Get the etdunits a collections maps to
     * 
     * @return array of <code>EtdUnit</code>
     * @throws SQLException
     */
    public static EtdUnit[] getEtdUnits(Context myContext, Collection g) throws SQLException
    {
        // Get the etdunit table rows
        TableRowIterator tri = DatabaseManager.queryTable(myContext, "etdunit",
                "SELECT etdunit.* FROM etdunit, collection2etdunit WHERE " + 
                "etdunit.etdunit_id=collection2etdunit.etdunit_id AND " +
                "collection2etdunit.collection_id= ? ",
                 g.getID());

        // Build a list of EtdUnit objects
        List<EtdUnit> etdunits = new ArrayList<EtdUnit>();
        try
        {
            while (tri.hasNext())
            {
                TableRow r = tri.next();

                // First check the cache
                EtdUnit fromCache = (EtdUnit) myContext.fromCache(EtdUnit.class, r
                        .getIntColumn("etdunit_id"));

                if (fromCache != null)
                {
                    etdunits.add(fromCache);
                }
                else
                {
                    etdunits.add(new EtdUnit(myContext, r));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        EtdUnit[] etdunitArray = (EtdUnit[]) etdunits.toArray(new EtdUnit[0]);

        return etdunitArray;
    }


}
