/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import org.apache.log4j.Logger;
import org.dspace.app.webui.discovery.DiscoverySearchRequestProcessor;
import org.dspace.app.webui.search.LuceneSearchRequestProcessor;
import org.dspace.app.webui.search.SearchProcessorException;
import org.dspace.app.webui.search.SearchRequestProcessor;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.browse.*;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.core.PluginConfigurationError;
import org.dspace.core.PluginManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Servlet for editing and deleting (expunging) items
 * 
 * @version $Revision$
 */
public class ItemMapServlet extends DSpaceServlet
{
    private SearchRequestProcessor internalLogic;

    /** Logger */
    private static Logger log = Logger.getLogger(ItemMapServlet.class);

    public void init()
    {
        try
        {
            internalLogic = (SearchRequestProcessor) PluginManager
                    .getSinglePlugin(SearchRequestProcessor.class);
        }
        catch (PluginConfigurationError e)
        {
            log.warn(
                    "ItemMapServlet not properly configurated, please configure the SearchRequestProcessor plugin",
                    e);
        }
        if (internalLogic == null)
        {   // Discovery is the default search provider since DSpace 4.0
            internalLogic = new DiscoverySearchRequestProcessor();
        }
    }

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws java.sql.SQLException,
            javax.servlet.ServletException, java.io.IOException,
            AuthorizeException
    {
        doDSPost(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws java.sql.SQLException,
            javax.servlet.ServletException, java.io.IOException,
            AuthorizeException
    {
    	String jspPage = null;
    	
    	// get with a collection ID means put up browse window
    	int myID = UIUtil.getIntParameter(request, "cid");
    	
    	// get collection
    	Collection myCollection = Collection.find(context, myID);
    	
    	// authorize check
    	AuthorizeManager.authorizeAction(context, myCollection,
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
    		Map<Integer, Item> myItems = new HashMap<Integer, Item>(); // # for the browser
    		Map<Integer, Collection> myCollections = new HashMap<Integer, Collection>(); // collections for list
    		Map<Integer, Integer> myCounts = new HashMap<Integer, Integer>(); // counts for each collection
    		
    		// get all items from that collection, add them to a hash
    		ItemIterator i = myCollection.getItems();
    		try
            {
                // iterate through the items in this collection, and count how many
                // are native, and how many are imports, and which collections they
                // came from
                while (i.hasNext())
                {
                    Item myItem = i.next();

                    // get key for hash
                    Integer myKey = Integer.valueOf(myItem.getID());

                    if (myItem.isOwningCollection(myCollection))
                    {
                        count_native++;
                    }
                    else
                    {
                        count_import++;
                    }

                    // is the collection in the hash?
                    Collection owningCollection = myItem.getOwningCollection();
                    Integer cKey = Integer.valueOf(owningCollection.getID());

                    if (myCollections.containsKey(cKey))
                    {
                        Integer x = myCounts.get(cKey);
                        int myCount = x.intValue() + 1;

                        // increment count for that collection
                        myCounts.put(cKey, Integer.valueOf(myCount));
                    }
                    else
                    {
                        // store and initialize count
                        myCollections.put(cKey, owningCollection);
                        myCounts.put(cKey, Integer.valueOf(1));
                    }

                    // store the item
                    myItems.put(myKey, myItem);
                }
            }
            finally
            {
                if (i != null)
                {
                    i.close();
                }
            }
            
            // remove this collection's entry because we already have a native
    		// count
    		myCollections.remove(Integer.valueOf(myCollection.getID()));
    		
    		// sort items - later
    		// show page
    		request.setAttribute("collection", myCollection);
    		request.setAttribute("count_native", Integer.valueOf(count_native));
    		request.setAttribute("count_import", Integer.valueOf(count_import));
    		request.setAttribute("items", myItems);
    		request.setAttribute("collections", myCollections);
    		request.setAttribute("collection_counts", myCounts);
    		request
    		.setAttribute("all_collections", Collection
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
    		String[] itemIDs = request.getParameterValues("item_ids");
    		String message = "remove";
    		LinkedList<String> removedItems = new LinkedList<String>();
    		
                if (itemIDs == null)
                {
                        message = "none-removed";
                }
                else
                {
    	 		for (int j = 0; j < itemIDs.length; j++)
	    		{
    				int i = Integer.parseInt(itemIDs[j]);
	    			removedItems.add(itemIDs[j]);
    			
    				Item myItem = Item.find(context, i);
    			
    				// make sure item doesn't belong to this collection
    				if (!myItem.isOwningCollection(myCollection))
    				{
    					myCollection.removeItem(myItem);
    					try
    					{
    						IndexBrowse ib = new IndexBrowse(context);
                            ib.indexItem(myItem);
    					}
    					catch (BrowseException e)
    					{
    						log.error("caught exception: ", e);
    						throw new ServletException(e);
    					}
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
    		String[] itemIDs = request.getParameterValues("item_ids");
    		String message = "added";
    		LinkedList<String> addedItems = new LinkedList<String>();
    		
    		
    		if (itemIDs == null)
    		{
    			message = "none-selected";
    		}
    		else
    		{
    			for (int j = 0; j < itemIDs.length; j++)
    			{
    				int i = Integer.parseInt(itemIDs[j]);
    				
    				Item myItem = Item.find(context, i);
    				
    				if (AuthorizeManager.authorizeActionBoolean(context, myItem, Constants.READ))
    				{
    					// make sure item doesn't belong to this collection
    					if (!myItem.isOwningCollection(myCollection))
    					{
    						myCollection.addItem(myItem);
    						try
    	    				{
    	    					IndexBrowse ib = new IndexBrowse(context);
    	    					ib.indexItem(myItem);
    	    				}
    	    				catch (BrowseException e)
    	    				{
    	    					log.error("caught exception: ", e);
    	    					throw new ServletException(e);
    	    				}
    						addedItems.add(itemIDs[j]);
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
    		int t = UIUtil.getIntParameter(request, "t");
    		
    		Collection targetCollection = Collection.find(context, t);
    		
    		// now find all imported items from that collection
    		// seemingly inefficient, but database should have this query cached
            Map<Integer, Item> items = new HashMap<Integer, Item>();
    		ItemIterator i = myCollection.getItems();
            try
            {
                while (i.hasNext())
                {
                    Item myItem = i.next();

                    if (myItem.isOwningCollection(targetCollection))
                    {
                        Integer myKey = Integer.valueOf(myItem.getID());
                        items.put(myKey, myItem);
                    }
                }
            }
            finally
            {
                if (i != null)
                {
                    i.close();
                }
            }
    		
            request.setAttribute("collection", myCollection);
    		request.setAttribute("browsetext", targetCollection
    				.getMetadata("name"));
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
