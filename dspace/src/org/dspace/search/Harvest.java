/*
 * Harvest.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Utility class for extracting information about items, possibly just within a
 * certain community or collection, that have been created, modified or
 * withdrawn within a particular range of dates.
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class Harvest
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(Harvest.class);
    
    /**
     * Obtain information about items that have been created, modified or
     * withdrawn within a given date range.  You can also specify 'offset'
     * and 'limit' so that a big harvest can be split up into smaller sections.
     * <P>
     * Note that dates are passed in the standard ISO8601 format used by
     * DSpace (and OAI-PMH).<P>
     * FIXME: Assumes all in_archive items have public metadata
     *
     * @param context     DSpace context
     * @param scope       a Collection, or <code>null</code>
     *                    indicating the scope is all of DSpace
     * @param startDate   start of date range, or <code>null</code>
     * @param endDate     end of date range, or <code>null</code>
     * @param offset      for a partial harvest, the point in the overall list
     *                    of matching items to start at.  0 means just start
     *                    at the beginning.
     * @param limit       the number of matching items to return in a partial
     *                    harvest.  Specify 0 to return the whole list
     *                    (or the rest of the list if an offset was specified.)
     * @param items       if <code>true</code> the <code>item</code> field of
     *                    each <code>HarvestedItemInfo</code> object is
     *                    filled out
     * @param collections if <code>true</code> the <code>collectionHandles</code>
     *                    field of each <code>HarvestedItemInfo</code> object
     *                    is filled out
     * @param withdrawn   If <code>true</code>, information about withdrawn
     *                    items is included
     * @return  List of <code>HarvestedItemInfo</code> objects
     */
    public static List harvest(Context context,
        Collection scope,
        String startDate,
        String endDate,
        int offset,
        int limit,
        boolean items,
        boolean collections,
        boolean withdrawn)
        throws SQLException
    {
        // SQL to add to the list of tables after the SELECT
        String scopeTableSQL = "";

        // SQL to add to the WHERE clause of the query
        String scopeWhereSQL = "";
                
        if (scope != null)
        {
            scopeTableSQL = ", collection2item";
            scopeWhereSQL = " AND collection2item.collection_id=" +
                scope.getID() +
                " AND collection2item.item_id=handle.resource_id";
        }
        
        // Put together our query.  Note there is no need for an
        // "in_archive=true" condition, we are using the existence of
        // Handles as our 'existence criterion'.
        String query = 
            "SELECT handle.handle, handle.resource_id, item.withdrawn, item.last_modified FROM handle, item" +
            scopeTableSQL + " WHERE handle.resource_type_id=" + Constants.ITEM +
            " AND handle.resource_id=item.item_id" + scopeWhereSQL;
        
        if (startDate != null)
        {
            query = query + " AND item.last_modified >= '" + startDate + "'";
        }
        
        if (endDate != null)
        {
        	/* If the end date has seconds precision, e.g.:
             * 
             *    2004-04-29T13:45:43Z
             * 
             * we need to add 999 milliseconds to this.  This is because
             * SQL TIMESTAMPs have millisecond precision, and so might
             * have a value:
             * 
             *    2004-04-29T13:45:43.952Z
             * 
             * and so <= '2004-04-29T13:45:43Z' would not pick this up.
             * Reading things out of the database, TIMESTAMPs are rounded
             * down, so the above value would be read as
             * '2004-04-29T13:45:43Z', and therefore a caller would expect
             * <= '2004-04-29T13:45:43Z' to include that value.
             * 
             * Got that? ;-)
             */ 
            if (endDate.length() == 20)
            {
            	endDate = endDate.substring(0, 19) + ".999Z";
            }
            
            query = query + " AND item.last_modified <= '" + endDate + "'";
        }

        if (withdrawn == false)
        {
            // Exclude withdrawn items
            query = query + " AND withdrawn=false";
        }
        
        // Order by item ID, so that for a given harvest the order will be
        // consistent.  This is so that big harvests can be broken up into
        // several smaller operations (e.g. for OAI resumption tokens.)
        query = query + " ORDER BY handle.resource_id";
        
        log.debug(LogManager.getHeader(context,
            "harvest SQL",
            query));

        // Execute
        TableRowIterator tri = DatabaseManager.query(context, query);
        List infoObjects = new LinkedList();
        int index = 0;
        
        // Process results of query into HarvestedItemInfo objects
        while (tri.hasNext())
        {
            TableRow row = tri.next();

            /*
             * This conditional ensures that we only process items within
             * any constraints specified by 'offset' and 'limit' parameters.
             */
            if (index >= offset &&
               (limit == 0 || index < offset + limit))
            {
                HarvestedItemInfo itemInfo = new HarvestedItemInfo();

                itemInfo.handle = row.getStringColumn("handle");
                itemInfo.itemID = row.getIntColumn("resource_id");
                // Put datestamp in ISO8601
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

                infoObjects.add(itemInfo);
            }

            index++;
        }

        return infoObjects;
    }


    /**
     * Get harvested item info for a single item.  <code>item</code> field in
     * returned <code>HarvestedItemInfo</code> object is always filled out.
     *
     * @param context     DSpace context
     * @param handle      Prefix-less Handle of item
     * @param collections if <code>true</code> the <code>collectionHandles</code>
     *                    field of the <code>HarvestedItemInfo</code> object
     *                    is filled out
     *
     * @return  <code>HarvestedItemInfo</code> object for the single item, or
     *          <code>null</code>
     */
    public static HarvestedItemInfo getSingle(Context context,
        String handle,
        boolean collections)
        throws SQLException
    {
        // FIXME: Assume Handle is item
        Item i = (Item) HandleManager.resolveToObject(context, handle);

        if (i == null)
        {
            return null;
        }
        
        // Fill out OAI info item object
        HarvestedItemInfo itemInfo = new HarvestedItemInfo();

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
     * @param context  DSpace context
     * @param itemInfo HarvestedItemInfo object to fill out
     */
    private static void fillCollections(Context context,
        HarvestedItemInfo itemInfo)
        throws SQLException
    {
        // Get the collection Handles from DB
        TableRowIterator colRows = DatabaseManager.query(
            context,
            "SELECT handle.handle FROM handle, collection2item WHERE handle.resource_type_id=" +
				Constants.COLLECTION + " AND collection2item.collection_id=handle.resource_id AND collection2item.item_id = " +
				itemInfo.itemID);

        // Chuck 'em in the itemInfo object
        itemInfo.collectionHandles = new LinkedList();
        
        while (colRows.hasNext())
        {
        	TableRow r = colRows.next();
        	itemInfo.collectionHandles.add(r.getStringColumn("handle"));
        }
    }
}
