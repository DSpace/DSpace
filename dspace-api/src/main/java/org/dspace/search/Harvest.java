/*
 * Harvest.java
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
package org.dspace.search;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.ObjectIdentifierMint;
import org.dspace.uri.IdentifierService;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

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
     *            if <code>true</code> the <code>collectionIdentifiers</code>
     *            field of each <code>HarvestedItemInfo</code> object is
     *            filled out
     * @param withdrawn
     *            If <code>true</code>, information about withdrawn items is
     *            included
     * @return List of <code>HarvestedItemInfo</code> objects
     * @throws ParseException If the date is not in a supported format
     */
    public static List harvest(Context context, DSpaceObject scope,
            String startDate, String endDate, int offset, int limit,
            boolean items, boolean collections, boolean withdrawn)
        throws ParseException
    {
        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);

        List<Item> itemList = itemDAO.getItems(scope, startDate, endDate, offset,
                limit, items, collections, withdrawn);

        List infoObjects = new LinkedList();
        int index = 0;

        // Process results of query into HarvestedItemInfo objects
        for (Item item : itemList)
        {
            /*
             * This conditional ensures that we only process items within any
             * constraints specified by 'offset' and 'limit' parameters.
             */
            if ((index >= offset)
                    && ((limit == 0) || (index < (offset + limit))))
            {
                HarvestedItemInfo itemInfo = new HarvestedItemInfo();
                
                itemInfo.context = context;
                itemInfo.identifier = item.getIdentifier();
                itemInfo.itemID = item.getID();
                itemInfo.datestamp = item.getLastModified();
                itemInfo.withdrawn = item.isWithdrawn();

                if (collections)
                {
                    fillCollections(context, itemInfo);
                }

                if (items)
                {
                    itemInfo.item = item;
                }

                infoObjects.add(itemInfo);
                item = null;
            }

            index++;
        }

        return infoObjects;
    }

    /**
     * Get harvested item info for a single item. <code>item</code> field in
     * returned <code>HarvestedItemInfo</code> object is always filled out.
     * 
     * @param context
     *            DSpace context
     * @param identifier
     *            A persistent identifier.
     * @param collections
     *            if <code>true</code> the <code>collectionIdentifiers</code>
     *            field of the <code>HarvestedItemInfo</code> object is filled
     *            out
     * 
     * @return <code>HarvestedItemInfo</code> object for the single item, or
     *         <code>null</code>
     */
    public static HarvestedItemInfo getSingle(Context context,
            ObjectIdentifier identifier, boolean collections)
    {
        // FIXME: Assume identifier is item
        Item i = (Item) IdentifierService.getResource(context, identifier);

        if (i == null)
        {
            return null;
        }

        // Fill out OAI info item object
        HarvestedItemInfo itemInfo = new HarvestedItemInfo();

        itemInfo.context = context;
        itemInfo.item = i;
        itemInfo.identifier = identifier;
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
     */
    private static void fillCollections(Context context,
            HarvestedItemInfo itemInfo)
    {
        CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);

        ObjectIdentifier oi = ObjectIdentifierMint.get(context, itemInfo.itemID, Constants.ITEM);
        // ObjectIdentifier oi = new ObjectIdentifier(itemInfo.itemID, Constants.ITEM);
        Item item = (Item) IdentifierService.getResource(context, oi);

        List<Collection> parents = collectionDAO.getParentCollections(item);

        List<ObjectIdentifier> identifiers =
            new LinkedList<ObjectIdentifier>();

        for (Collection parent : parents)
        {
            identifiers.add(parent.getIdentifier());
        }

        itemInfo.collectionIdentifiers = identifiers;
    }
}
