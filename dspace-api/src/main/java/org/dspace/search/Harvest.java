/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.eperson.Group;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

import java.sql.SQLException;
import java.text.ParseException;
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

    protected static final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected static final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
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
     * @throws SQLException if database error
     * @throws java.text.ParseException If the date is not in a supported format
     */
    public static List<HarvestedItemInfo> harvest(Context context, DSpaceObject scope,
            String startDate, String endDate, int offset, int limit,
            boolean items, boolean collections, boolean withdrawn,
            boolean nonAnon) throws SQLException, ParseException
    {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addFilterQueries("search.resourcetype:" + Constants.ITEM);

        if (scope != null)
        {
            discoverQuery.addFieldPresentQueries("location:" + scope.getID());
        }

        if (startDate != null)
        {
            discoverQuery.addFilterQueries("lastModified => " + new DCDate(startDate).toString());
        }

        if (endDate != null)
        {
            discoverQuery.addFilterQueries("lastModified <= " + new DCDate(startDate).toString());
        }

        if (!withdrawn)
        {
            discoverQuery.addFilterQueries("archived: true OR withdrawn: false");
        }else{
            discoverQuery.addFilterQueries("archived: true OR withdrawn: true");
        }

        // Order by item ID, so that for a given harvest the order will be
        // consistent. This is so that big harvests can be broken up into
        // several smaller operations (e.g. for OAI resumption tokens.)
        discoverQuery.setSortField("search.resourceid", DiscoverQuery.SORT_ORDER.asc);

        List<HarvestedItemInfo> infoObjects = new LinkedList<HarvestedItemInfo>();

        // Count of items read from the record set that match the selection criteria.
        // Note : Until 'index > offset' the records are not added to the output set.
        int index = 0;

        // Count of items added to the output set.
        int itemCounter = 0;

        try {
            DiscoverResult discoverResult = SearchUtils.getSearchService().search(context, discoverQuery);

            // Process results of query into HarvestedItemInfo objects
            Iterator<DSpaceObject> dsoIterator = discoverResult.getDspaceObjects().iterator();
            while (dsoIterator.hasNext() && ((limit == 0) || (itemCounter < limit)))
            {
                DSpaceObject dso = dsoIterator.next();
                HarvestedItemInfo itemInfo = new HarvestedItemInfo();
                itemInfo.context = context;
                itemInfo.handle = dso.getHandle();
                itemInfo.itemID = dso.getID();
                itemInfo.datestamp = ((Item) dso).getLastModified();
                itemInfo.withdrawn = ((Item) dso).isWithdrawn();

                if (collections)
                {
                    // Add collections data
                    fillCollections(context, itemInfo);
                }

                if (items)
                {
                    // Add the item reference
                    itemInfo.item = itemService.find(context, itemInfo.itemID);
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
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
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
     * @throws SQLException if database error
     */
    public static HarvestedItemInfo getSingle(Context context, String handle,
            boolean collections) throws SQLException
    {
        // FIXME: Assume Handle is item
        Item i = (Item) handleService.resolveToObject(context, handle);

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
     * @throws SQLException if database error
     */
    private static void fillCollections(Context context,
            HarvestedItemInfo itemInfo) throws SQLException
    {
        // Get the collection Handles from DB
        List<Collection> collections = itemInfo.item.getCollections();
        itemInfo.collectionHandles = new ArrayList<>();
        for (Collection collection : collections) {
            itemInfo.collectionHandles.add(collection.getHandle());
        }
    }

    /**
     * Does the item allow anonymous access ? ie. authorizedGroups must include id=0.
     */
    private static boolean anonAccessAllowed(Context context, HarvestedItemInfo itemInfo) throws SQLException
    {
        List<Group> authorizedGroups = authorizeService.getAuthorizedGroups(context, itemInfo.item, Constants.READ);

        for (Group authorizedGroup : authorizedGroups)
        {
            if (authorizedGroup.getName().equals(Group.ANONYMOUS))
            {
                return true;
            }
        }

        return false;
    }
}
