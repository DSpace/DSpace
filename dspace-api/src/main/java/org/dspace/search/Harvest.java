/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class for extracting information about items, possibly just within a
 * certain community or collection, that have been created, modified or
 * withdrawn within a particular range of dates.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class Harvest
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(Harvest.class);

    /**
     * Obtain information about items that have been created, modified or
     * withdrawn within a given date range. You can also specify 'offset' and
     * 'limit' so that a big harvest can be split up into smaller sections.
     * <P>
     * Note that dates are passed in the standard ISO8601 format used by DSpace
     * (and OAI-PMH).
     * <P>
     * FIXME: Assumes all in_archive items have public metadata
     *
     * @param context
     *            DSpace context
     * @param scope
     *            a Collection, Community, or <code>null</code> indicating the scope is
     *            all of DSpace
     * @param startDate
     *            start of date range, or <code>null</code>
     * @param endDate
     *            end of date range, or <code>null</code>
     * @param offset
     *            for a partial harvest, the point in the overall list of
     *            matching items to start at. 0 means just start at the
     *            beginning.
     * @param limit
     *            the number of matching items to return in a partial harvest.
     *            Specify 0 to return the whole list (or the rest of the list if
     *            an offset was specified.)
     * @param items
     *            if <code>true</code> the <code>item</code> field of each
     *            <code>HarvestedItemInfo</code> object is filled out
     * @param collections
     *            if <code>true</code> the <code>collectionHandles</code>
     *            field of each <code>HarvestedItemInfo</code> object is
     *            filled out
     * @param withdrawn
     *            If <code>true</code>, information about withdrawn items is
     *            included
     * @param nonAnon
     *            If items without anonymous access should be included or not
     * @return List of <code>HarvestedItemInfo</code> objects
     * @throws java.sql.SQLException
     * @throws java.text.ParseException If the date is not in a supported format
     */
    public static List<HarvestedItemInfo> harvest(Context context, DSpaceObject scope,
            String startDate, String endDate, int offset, int limit,
            boolean items, boolean collections, boolean withdrawn,
            boolean nonAnon) throws SQLException, ParseException
    {

        // Put together our query. Note there is no need for an
        // "in_archive=true" condition, we are using the existence of
        // Handles as our 'existence criterion'.
        // FIXME: I think the "DISTINCT" is redundant
        String query = "SELECT DISTINCT handle.handle, handle.resource_id, item.withdrawn, item.last_modified FROM handle, item";


        // We are building a complex query that may contain a variable
        // about of input data points. To accommodate this while still
        // providing type safety we build a list of parameters to be
        // plugged into the query at the database level.
        List<Serializable> parameters = new ArrayList<Serializable>();

        if (scope != null)
        {
        	if (scope.getType() == Constants.COLLECTION)
        	{
        		query += ", collection2item";
        	}
        	else if (scope.getType() == Constants.COMMUNITY)
        	{
        		query += ", communities2item";
        	}
        }

        query += " WHERE handle.resource_type_id=" + Constants.ITEM + " AND handle.resource_id=item.item_id ";

        if (scope != null)
        {
        	if (scope.getType() == Constants.COLLECTION)
        	{
        		query += " AND collection2item.collection_id= ? " +
        	             " AND collection2item.item_id=handle.resource_id ";
        		parameters.add(Integer.valueOf(scope.getID()));
        	}
        	else if (scope.getType() == Constants.COMMUNITY)
        	{
        		query += " AND communities2item.community_id= ? " +
						 " AND communities2item.item_id=handle.resource_id";
        		parameters.add(Integer.valueOf(scope.getID()));
        	}
        }

        if (startDate != null)
        {
        	query = query + " AND item.last_modified >= ? ";
        	parameters.add(toTimestamp(startDate, false));
        }

        if (endDate != null)
        {
            /*
             * If the end date has seconds precision, e.g.:
             *
             * 2004-04-29T13:45:43Z
             *
             * we need to add 999 milliseconds to this. This is because SQL
             * TIMESTAMPs have millisecond precision, and so might have a value:
             *
             * 2004-04-29T13:45:43.952Z
             *
             * and so <= '2004-04-29T13:45:43Z' would not pick this up. Reading
             * things out of the database, TIMESTAMPs are rounded down, so the
             * above value would be read as '2004-04-29T13:45:43Z', and
             * therefore a caller would expect <= '2004-04-29T13:45:43Z' to
             * include that value.
             *
             * Got that? ;-)
             */
        	boolean selfGenerated = false;
            if (endDate.length() == 20)
            {
                endDate = endDate.substring(0, 19) + ".999Z";
                selfGenerated = true;
            }

        	query += " AND item.last_modified <= ? ";
            parameters.add(toTimestamp(endDate, selfGenerated));
        }

        if (!withdrawn)
        {
            // Exclude withdrawn items
            if (DatabaseManager.isOracle())
            {
                query += " AND withdrawn=0 ";
            }
            else
            {
                // postgres uses booleans
                query += " AND withdrawn=false ";
            }
        }

        // Order by item ID, so that for a given harvest the order will be
        // consistent. This is so that big harvests can be broken up into
        // several smaller operations (e.g. for OAI resumption tokens.)
        query += " ORDER BY handle.resource_id";

        log.debug(LogManager.getHeader(context, "harvest SQL", query));

        Object[] parametersArray = parameters.toArray();
        TableRowIterator tri = DatabaseManager.query(context, query, parametersArray);
        List<HarvestedItemInfo> infoObjects = new LinkedList<HarvestedItemInfo>();

        // Count of items read from the record set that match the selection criteria.
        // Note : Until 'index > offset' the records are not added to the output set.
        int index = 0;

        // Count of items added to the output set.
        int itemCounter = 0;

        try
        {
            // Process results of query into HarvestedItemInfo objects
            while ((tri.hasNext()) && ((limit == 0) || (itemCounter < limit)))
            {
                TableRow row = tri.next();

                HarvestedItemInfo itemInfo = new HarvestedItemInfo();
                itemInfo.context = context;
                itemInfo.handle = row.getStringColumn("handle");
                itemInfo.itemID = row.getIntColumn("resource_id");
                itemInfo.datestamp = row.getDateColumn("last_modified");
                itemInfo.withdrawn = row.getBooleanColumn("withdrawn");

                if (collections)
                {
                    // Add collections data
                    fillCollections(context, itemInfo);
                }

                if (items)
                {
                    // Add the item reference
                    itemInfo.item = Item.find(context, itemInfo.itemID);
                }

                if ((nonAnon) || (itemInfo.item == null) || (withdrawn && itemInfo.withdrawn))
                {
                    index++;
                    if (index > offset)
                    {
                        infoObjects.add(itemInfo);
                        itemCounter++;
                    }
                }
                else
                {
                    // We only want items that allow for anonymous access.
                    if (anonAccessAllowed(context, itemInfo))
                    {
                        index++;
                        if (index > offset)
                        {
                            infoObjects.add(itemInfo);
                            itemCounter++;
                        }
                    }
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

        return infoObjects;
    }

    /**
     * Get harvested item info for a single item. <code>item</code> field in
     * returned <code>HarvestedItemInfo</code> object is always filled out.
     *
     * @param context
     *            DSpace context
     * @param handle
     *            Prefix-less Handle of item
     * @param collections
     *            if <code>true</code> the <code>collectionHandles</code>
     *            field of the <code>HarvestedItemInfo</code> object is filled
     *            out
     *
     * @return <code>HarvestedItemInfo</code> object for the single item, or
     *         <code>null</code>
     * @throws java.sql.SQLException
     */
    public static HarvestedItemInfo getSingle(Context context, String handle,
            boolean collections) throws SQLException
    {
        // FIXME: Assume Handle is item
        Item i = (Item) HandleManager.resolveToObject(context, handle);

        if (i == null)
        {
            return null;
        }

        // Fill out OAI info item object
        HarvestedItemInfo itemInfo = new HarvestedItemInfo();

        itemInfo.context = context;
        itemInfo.item = i;
        itemInfo.handle = handle;
        itemInfo.withdrawn = i.isWithdrawn();
        itemInfo.datestamp = i.getLastModified();
        itemInfo.itemID = i.getID();

        // Get the sets
        if (collections)
        {
            fillCollections(context, itemInfo);
        }

        return itemInfo;
    }

    /**
     * Fill out the containers field of the HarvestedItemInfo object
     *
     * @param context
     *            DSpace context
     * @param itemInfo
     *            HarvestedItemInfo object to fill out
     * @throws java.sql.SQLException
     */
    private static void fillCollections(Context context,
            HarvestedItemInfo itemInfo) throws SQLException
    {
        // Get the collection Handles from DB
        TableRowIterator colRows = DatabaseManager.query(context,
                        "SELECT handle.handle FROM handle, collection2item WHERE handle.resource_type_id= ? " +
                        "AND collection2item.collection_id=handle.resource_id AND collection2item.item_id = ? ",
                        Constants.COLLECTION, itemInfo.itemID);

        try
        {
            // Chuck 'em in the itemInfo object
            itemInfo.collectionHandles = new LinkedList();

            while (colRows.hasNext())
            {
                TableRow r = colRows.next();
                itemInfo.collectionHandles.add(r.getStringColumn("handle"));
            }
        }
        finally
        {
            if (colRows != null)
            {
                colRows.close();
            }
        }
    }


    /**
     * Convert a String to a java.sql.Timestamp object
     *
     * @param t The timestamp String
     * @param selfGenerated Is this a self generated timestamp (e.g. it has .999 on the end)
     * @return The converted Timestamp
     * @throws java.text.ParseException
     */
    private static Timestamp toTimestamp(String t, boolean selfGenerated) throws ParseException
    {
        SimpleDateFormat df;
        
        // Choose the correct date format based on string length
        if (t.length() == 10)
        {
            df = new SimpleDateFormat("yyyy-MM-dd");
        }
        else if (t.length() == 20)
        {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        }
        else if (selfGenerated)
        {
            df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        }
        else {
            // Not self generated, and not in a guessable format
            throw new ParseException("", 0);
        }
        
        // Parse the date
        df.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        return new Timestamp(df.parse(t).getTime());
    }

    /**
     * Does the item allow anonymous access ? ie. authorizedGroups must include id=0.
     */
    private static boolean anonAccessAllowed(Context context, HarvestedItemInfo itemInfo) throws SQLException
    {
        Group[] authorizedGroups = AuthorizeManager.getAuthorizedGroups(context, itemInfo.item, Constants.READ);

        for (Group authorizedGroup : authorizedGroups)
        {
            if (authorizedGroup.getID() == Group.ANONYMOUS_ID)
            {
                return true;
            }
        }

        return false;
    }
}
