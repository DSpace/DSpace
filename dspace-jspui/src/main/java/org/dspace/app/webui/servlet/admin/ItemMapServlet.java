/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.webui.discovery.DiscoverySearchRequestProcessor;
import org.dspace.app.webui.search.SearchProcessorException;
import org.dspace.app.webui.search.SearchRequestProcessor;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginConfigurationError;
import org.dspace.core.factory.CoreServiceFactory;

/**
 * Servlet for editing and deleting (expunging) items
 * 
 * @version $Revision$
 */
public class ItemMapServlet extends DSpaceServlet
{
    private transient SearchRequestProcessor internalLogic;

    private final transient CollectionService collectionService
             = ContentServiceFactory.getInstance().getCollectionService();
    
    private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();

    /** Logger */
    private static final Logger log = Logger.getLogger(ItemMapServlet.class);

    public ItemMapServlet()
    {
    	try
        {
            internalLogic = (SearchRequestProcessor) CoreServiceFactory.getInstance().getPluginService()
                    .getSinglePlugin(SearchRequestProcessor.class);
        }
        catch (PluginConfigurationError e)
        {
            log.warn(
                    "ItemMapServlet not properly configured -- please configure the SearchRequestProcessor plugin",
                    e);
        }
        if (internalLogic == null)
        {   // Discovery is the default search provider since DSpace 4.0
            internalLogic = new DiscoverySearchRequestProcessor();
        }
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws java.sql.SQLException,
            javax.servlet.ServletException, java.io.IOException,
            AuthorizeException
    {
        doDSPost(context, request, response);
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws java.sql.SQLException,
            javax.servlet.ServletException, java.io.IOException,
            AuthorizeException
    {
    	String jspPage = null;
    	
    	// get with a collection ID means put up browse window
    	UUID myID = UIUtil.getUUIDParameter(request, "cid");
    	
    	// get collection
    	Collection myCollection = collectionService.find(context, myID);
    	
    	// authorize check
    	authorizeService.authorizeAction(context, myCollection,
                Constants.COLLECTION_ADMIN);
    	
    	String action = request.getParameter("action");
    	
    	if (action == null)
    	{
    		action = "";
    	}
    	
    	// Defined non-empty value shows that 'Cancel' has been pressed
    	String cancel = request.getParameter("cancel");
    	
    	if (cancel == null)
    	{
    		cancel = "";
    	}
    	
    	if (action.equals("") || !cancel.equals(""))
    	{
    		// get with no action parameter set means to put up the main page
    		// which is statistics and some command buttons to add/remove items
    		//
    		// also holds for interruption by pressing 'Cancel'
    		int count_native = 0; // # of items owned by this collection
    		int count_import = 0; // # of virtual items
    		Map<UUID, Item> myItems = new HashMap<>(); // # for the browser
    		Map<UUID, Collection> myCollections = new HashMap<>(); // collections for list
    		Map<UUID, Integer> myCounts = new HashMap<>(); // counts for each collection
    		
    		// get all items from that collection, add them to a hash
    		Iterator<Item> i = itemService.findAllByCollection(context, myCollection);
            // iterate through the items in this collection, and count how many
            // are native, and how many are imports, and which collections they
            // came from
            while (i.hasNext())
            {
                Item myItem = i.next();

                // get key for hash
                UUID myKey = myItem.getID();

                if (itemService.isOwningCollection(myItem, myCollection))
                {
                    count_native++;
                }
                else
                {
                    count_import++;
                }

                // is the collection in the hash?
                Collection owningCollection = myItem.getOwningCollection();
                UUID cKey = owningCollection.getID();

                if (myCollections.containsKey(cKey))
                {
                    Integer x = myCounts.get(cKey);
                    int myCount = x + 1;

                    // increment count for that collection
                    myCounts.put(cKey, myCount);
                }
                else
                {
                    // store and initialize count
                    myCollections.put(cKey, owningCollection);
                    myCounts.put(cKey, 1);
                }

                // store the item
                myItems.put(myKey, myItem);
            }
    		
            // remove this collection's entry because we already have a native
    		// count
    		myCollections.remove(myCollection.getID());
    		
    		// sort items - later
    		// show page
    		request.setAttribute("collection", myCollection);
    		request.setAttribute("count_native", count_native);
    		request.setAttribute("count_import", count_import);
    		request.setAttribute("items", myItems);
    		request.setAttribute("collections", myCollections);
    		request.setAttribute("collection_counts", myCounts);
    		request
    		.setAttribute("all_collections", collectionService
    				.findAll(context));
    		
            request.setAttribute("searchIndices",
                    internalLogic.getSearchIndices());
            request.setAttribute("prefixKey", internalLogic.getI18NKeyPrefix());
    		// show this page when we're done
    		jspPage = "itemmap-main.jsp";
    		
    		// show the page
    		JSPManager.showJSP(request, response, jspPage);
    	}
    	else if (action.equals("Remove"))
    	{
    		// get item IDs to remove
    		List<UUID> itemIDs = Util.getUUIDParameters(request, "item_ids");
    		String message = "remove";
    		LinkedList<UUID> removedItems = new LinkedList<>();
    		
                if (itemIDs == null)
                {
                        message = "none-removed";
                }
                else
                {
    	 		for (UUID i : itemIDs)
	    		{
	    			removedItems.add(i);
    			
    				Item myItem = itemService.find(context, i);
    			
    				// make sure item doesn't belong to this collection
    				if (!itemService.isOwningCollection(myItem, myCollection))
    				{
    					collectionService.removeItem(context, myCollection, myItem);
    				}
    			}
		}
    		
    		request.setAttribute("message", message);
    		request.setAttribute("collection", myCollection);
    		request.setAttribute("processedItems", removedItems);
    		
    		// show this page when we're done
    		jspPage = "itemmap-info.jsp";
    		
    		// show the page
    		JSPManager.showJSP(request, response, jspPage);
    	}
    	else if (action.equals("Add"))
    	{
    		// get item IDs to add
    		List<UUID> itemIDs = Util.getUUIDParameters(request, "item_ids");
    		String message = "added";
    		LinkedList<UUID> addedItems = new LinkedList<>();
    		
    		
    		if (itemIDs == null)
    		{
    			message = "none-selected";
    		}
    		else
    		{
    			for (UUID i : itemIDs)
    			{
    				Item myItem = itemService.find(context, i);
    				
    				if (authorizeService.authorizeActionBoolean(context, myItem, Constants.READ))
    				{
    					// make sure item doesn't belong to this collection
    					if (!itemService.isOwningCollection(myItem, myCollection))
    					{
    						collectionService.addItem(context, myCollection, myItem);
    						addedItems.add(i);
    					}
    				}
    			}
    		}
    		
    		request.setAttribute("message", message);
    		request.setAttribute("collection", myCollection);
    		request.setAttribute("processedItems", addedItems);
    		
    		// show this page when we're done
    		jspPage = "itemmap-info.jsp";
    		
    		// show the page
    		JSPManager.showJSP(request, response, jspPage);
    	}
    	else if (action.equals("search"))
    	{
            request.setAttribute("collection", myCollection);
            try
            {
                internalLogic.doItemMapSearch(context, request, response);
            }
            catch (SearchProcessorException e)
            {
                log.error(e.getMessage(), e);
                throw new ServletException(e.getMessage(), e);
            }
        }
    	else if (action.equals("browse"))
    	{
    		// target collection to browse
    		UUID t = UIUtil.getUUIDParameter(request, "t");
    		
    		Collection targetCollection = collectionService.find(context, t);
    		
    		// now find all imported items from that collection
    		// seemingly inefficient, but database should have this query cached
            Map<UUID, Item> items = new HashMap<>();
    		Iterator<Item> i = itemService.findAllByCollection(context, myCollection);
            while (i.hasNext())
            {
                Item myItem = i.next();

                if (itemService.isOwningCollection(myItem, targetCollection))
                {
                    items.put(myItem.getID(), myItem);
                }
            }
    		
            request.setAttribute("collection", myCollection);
    		request.setAttribute("browsetext", targetCollection
    				.getName());
    		request.setAttribute("items", items);
    		request.setAttribute("browsetype", "Remove");
    		
    		// show this page when we're done
    		jspPage = "itemmap-browse.jsp";
    		
    		// show the page
    		JSPManager.showJSP(request, response, jspPage);
    	}
    	
    	context.complete();
    }
}
