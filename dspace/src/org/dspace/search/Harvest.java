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

import org.dspace.administer.DCType;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
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
    
    /** ID of the date.available DC type */
    private static int dateAvailableDCTypeID = -1;
    
    /**
     * Obtain information about items that have been created, modified or
     * withdrawn within a given date range.
     * FIXME: Assumes all in_archive items have date.available set,
     * and have public metadata
     *
     * @param context     DSpace context
     * @param scope       a Community or Collection, or <code>null</code>
     *                    indicating the scope is all of DSpace
     * @param startDate   start of date range, or <code>null</code>
     * @param endDate     end of date range, or <code>null</code>
     * @param items       if <code>true</code> the <code>item</code> field of
     *                    each <code>HarvestedItemInfo</code> object is
     *                    filled out
     * @param containers  if <code>true</code> the <code>containers</code>
     *                    field of each <code>HarvestedItemInfo</code> object
     *                    is filled out
     * @param withdrawn   If <code>true</code>, information about withdrawn
     *                    items is included
     * @return  List of <code>HarvestedItemInfo</code> objects
     */
    public static List harvest(Context context,
        DSpaceObject scope,
        String startDate,
        String endDate,
        boolean items,
        boolean containers,
        boolean withdrawn)
        throws SQLException
    {
        int dcTypeID = getDateAvailableTypeID(context);
        
        // SQL to add to the list of tables after the SELECT
        String scopeTableSQL = "";

        // SQL to add to the WHERE clause of the query
        String scopeWhereSQL = "";
                
        if (scope != null)
        {
            if (scope.getType() == Constants.COMMUNITY)
            {
                // Getting things within a community
                scopeTableSQL = ", community2item";
                scopeWhereSQL = " AND community2item.community_id=" +
                    scope.getID() +
                    " AND community2item.item_id=handle.resource_id";
            }
            else if (scope.getType() == Constants.COLLECTION)
            {
                scopeTableSQL = ", collection2item";
                scopeWhereSQL = " AND collection2item.collection_id=" +
                    scope.getID() +
                    " AND collection2item.item_id=handle.resource_id";
            }
            // Theoretically, no other possibilities, won't bother to check
        }
        
        // Put together our query
        String query = 
            "SELECT handle.handle, handle.resource_id, dcvalue.text_value FROM handle, dcvalue, item" +
            scopeTableSQL + " WHERE handle.resource_type_id=" + Constants.ITEM +
            " AND handle.resource_id=dcvalue.item_id  AND dcvalue.item_id=item.item_id AND item.withdrawn=false AND dcvalue.dc_type_id=" +
            dcTypeID + scopeWhereSQL;
        
        if (startDate != null)
        {
            query = query + " AND dcvalue.text_value >= '" + startDate + "'";
        }
        
        if (endDate != null)
        {
            query = query + " AND dcvalue.text_value <= '" + endDate + "'";
        }

        log.debug(LogManager.getHeader(context,
            "harvest SQL",
            query));

        // Execute
        TableRowIterator tri = DatabaseManager.query(context, query);
        List infoObjects = new LinkedList();
        
        // Process results of query into HarvestedItemInfo objects
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            HarvestedItemInfo itemInfo = new HarvestedItemInfo();
            
            itemInfo.handle = row.getStringColumn("handle");
            itemInfo.itemID = row.getIntColumn("resource_id");
            itemInfo.datestamp = row.getStringColumn("text_value");
            itemInfo.withdrawn = false;

            if (containers)
            {
                fillContainers(context, itemInfo);
            }

            if (items)
            {
                // Get the item
                itemInfo.item = Item.find(context, itemInfo.itemID);
            }
            
            infoObjects.add(itemInfo);
        }

        // Add information about deleted items if necessary
        if (withdrawn)
        {
            // Put together our query
            query = 
                "SELECT handle.handle, handle.resource_id, item.withdrawal_date FROM handle, item" +
                scopeTableSQL + " WHERE handle.resource_type_id=" + Constants.ITEM +
                " AND handle.resource_id=item.item_id  AND item.withdrawn=true"
                + scopeWhereSQL;

            if (startDate != null)
            {
                query = query + " AND item.withdrawal_date >= '" + startDate + "'";
            }

            if (endDate != null)
            {
                query = query + " AND item.withdrawal_date <= '" + endDate + "'";
            }

            log.debug(LogManager.getHeader(context,
                "harvest SQL (withdrawals)",
                query));

            // Execute
            tri = DatabaseManager.query(context, query);

            // Process results of query into HarvestedItemInfo objects
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                HarvestedItemInfo itemInfo = new HarvestedItemInfo();

                itemInfo.handle = row.getStringColumn("handle");
                itemInfo.itemID = row.getIntColumn("resource_id");
                itemInfo.datestamp = row.getStringColumn("withdrawal_date");
                itemInfo.withdrawn = true;
                
                if (containers)
                {
                    fillContainers(context, itemInfo);
                }

                // Won't fill out item objects for withdrawn items
                infoObjects.add(itemInfo);
            }
        }

        return infoObjects;
    }


    /**
     * Get harvested item info for a single item.  <code>item</code> field in
     * returned <code>HarvestedItemInfo</code> object is always filled out.
     *
     * @param context     DSpace context
     * @param handle      Prefix-less Handle of item
     * @param containers  if <code>true</code> the <code>containers</code>
     *                    field of each <code>HarvestedItemInfo</code> object
     *                    is filled out
     *
     * @return  <code>HarvestedItemInfo</code> object for the single item, or
     *          <code>null</code>
     */
    public static HarvestedItemInfo getSingle(Context context,
        String handle,
        boolean containers)
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

        DCDate withdrawalDate = i.getWithdrawalDate();

        if (withdrawalDate == null)
        {
            // FIXME: Assume data.available is there
            DCValue[] dateAvail =
                i.getDC("date", "available", Item.ANY);
            itemInfo.datestamp = dateAvail[0].value;
            itemInfo.withdrawn = false;
        }
        else
        {
            itemInfo.datestamp = withdrawalDate.toString();
            itemInfo.withdrawn = true;
        }
        
        itemInfo.itemID = i.getID();

        // Get the sets
        if (containers)
        {
            fillContainers(context, itemInfo);
        }
               
        return itemInfo;
    }
    

    /**
     * Fill out the containers field of the HarvestedItemInfo object
     *
     * @param context  DSpace context
     * @param itemInfo HarvestedItemInfo object to fill out
     */
    private static void fillContainers(Context context,
        HarvestedItemInfo itemInfo)
        throws SQLException
    {
        // Get the containers (communities/collections)
        List containerRows = DatabaseManager.query(
            context,
            "SELECT community2collection.community_id, community2collection.collection_id FROM community2collection, collection2item WHERE community2collection.collection_id=collection2item.collection_id AND collection2item.item_id=" +
            itemInfo.itemID).toList();

        itemInfo.containers = new int[containerRows.size()][2];

        for (int i = 0; i < containerRows.size(); i++)
        {
            TableRow r = (TableRow) containerRows.get(i);
            itemInfo.containers[i][0] = r.getIntColumn("community_id");
            itemInfo.containers[i][1] = r.getIntColumn("collection_id");
        }
    }
    
    
    /**
     * Get the type ID of the "date.available" DC type.  This gets the ID
     * once then stores it to avoid repeat queries.
     *
     * @param context  DSpace context
     * @return  the ID of the date.available DC type.
     */
    private static int getDateAvailableTypeID(Context context)
        throws SQLException
    {
        if (dateAvailableDCTypeID == -1)
        {
            DCType type = DCType.findByElement(context, "date", "available");
            dateAvailableDCTypeID = type.getID();
        }
        
        return dateAvailableDCTypeID;
    }
}
