/*
 * Harvest.java
 *
 * Version: $Revision: 4889 $
 *
 * Date: $Date: 2010-05-05 15:07:23 -0400 (Wed, 05 May 2010) $
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
package org.dspace.search;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.eperson.Group;

/**
 * Utility class for extracting information about items, possibly just within a
 * certain community or collection, that have been created, modified or
 * withdrawn within a particular range of dates.
 * 
 * @author Robert Tansley
 * @version $Revision: 4889 $
 */
public class Harvest
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(Harvest.class);
    
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
     * @throws SQLException
     * @throws ParseException If the date is not in a supported format
     */
    public static List harvest(Context context, DSpaceObject scope,
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
        // about of input data points. To accomidate this while still 
        // providing type safty we build a list of parameters to be 
        // plugged into the query at the database level.
        List parameters = new ArrayList();
        
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
        		parameters.add(new Integer(scope.getID()));
        	}
        	else if (scope.getType() == Constants.COMMUNITY)
        	{
        		query += " AND communities2item.community_id= ? " +
						 " AND communities2item.item_id=handle.resource_id";
        		parameters.add(new Integer(scope.getID()));
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
        
        if (withdrawn == false)
        {
            // Exclude withdrawn items
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
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
        
        // Execute
        Object[] parametersArray = parameters.toArray();
        TableRowIterator tri = DatabaseManager.query(context, query, parametersArray);
        List infoObjects = new LinkedList();
        int index = 0;
        int itemCounter = 0;

        try
        {
            // Process results of query into HarvestedItemInfo objects
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                /**
                 * If we are looking for public-only items, we need to scan all objects
                 * for permissions in order to properly calculate the offset
                 */
                if ((!nonAnon) && (index < offset))
                {
                    HarvestedItemInfo itemInfo = new HarvestedItemInfo();
                    itemInfo.itemID = row.getIntColumn("resource_id");
                    itemInfo.item = Item.find(context, itemInfo.itemID);
                    Group[] authorizedGroups = AuthorizeManager.getAuthorizedGroups(context, itemInfo.item, Constants.READ);
                        boolean added = false;
                        for (int i = 0; i < authorizedGroups.length; i++)
                        {
                            if ((authorizedGroups[i].getID() == 0) && (!added))
                            {
                                added = true;
                            }
                        }
                        if (!added)
                        {
                            offset++;
                        }
                }

                /*
                 * This conditional ensures that we only process items within any
                 * constraints specified by 'offset' and 'limit' parameters.
                 */
                else if ((index >= offset) && ((limit == 0) || (itemCounter < limit)))
                {
                    HarvestedItemInfo itemInfo = new HarvestedItemInfo();

                    itemInfo.context = context;
                    itemInfo.handle = row.getStringColumn("handle");
                    itemInfo.itemID = row.getIntColumn("resource_id");
                    itemInfo.datestamp = row.getDateColumn("last_modified");
                    itemInfo.withdrawn = row.getBooleanColumn("withdrawn");

                    if (collections)
                    {
                        fillCollections(context, itemInfo);
                    }

                    if (items)
                    {
                        // Get the item
                        itemInfo.item = Item.find(context, itemInfo.itemID);
                    }

                    if ((nonAnon) || (itemInfo.item == null) || (withdrawn && itemInfo.withdrawn))
                    {
                        infoObjects.add(itemInfo);
                        itemCounter++;
                    } else
                    {
                        Group[] authorizedGroups = AuthorizeManager.getAuthorizedGroups(context, itemInfo.item, Constants.READ);
                        boolean added = false;
                        for (int i = 0; i < authorizedGroups.length; i++)
                        {
                            if ((authorizedGroups[i].getID() == 0) && (!added))
                            {
                                infoObjects.add(itemInfo);
                                added = true;
                                itemCounter++;
                            }
                        }
                    }
                }

                index++;
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
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
     * @throws SQLException
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
     * @throws SQLException
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
                colRows.close();
        }
    }

    
    /**
     * Convert a String to a java.sql.Timestamp object
     * 
     * @param t The timestamp String
     * @param selfGenerated Is this a self generated timestamp (e.g. it has .999 on the end)
     * @return The converted Timestamp
     * @throws ParseException 
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
}
