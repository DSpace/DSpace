/*
 * ItemMapServlet.java
 *
 * Version: $$
 *
 * Date: $$
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
package org.dspace.app.webui.servlet.admin;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Servlet for editing and deleting (expunging) items
 * 
 * @version $Revision: 1.6 $
 */
public class ItemMapServlet extends DSpaceServlet
{
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

        if (action.equals(""))
        {
            // get with no action parameter set means to put up the main page
            // which is statistics and some command buttons to add/remove items
            int count_native = 0; // # of items owned by this collection
            int count_import = 0; // # of virtual items
            Map myItems = new HashMap(); // # for the browser
            Map myCollections = new HashMap(); // collections for list
            Map myCounts = new HashMap(); // counts for each collection

            // get all items from that collection, add them to a hash
            ItemIterator i = myCollection.getItems();

            // iterate through the items in this collection, and count how many
            // are native, and how many are imports, and which collections they
            // came from
            while (i.hasNext())
            {
                Item myItem = i.next();

                // get key for hash
                Integer myKey = new Integer(myItem.getID());

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
                Integer cKey = new Integer(owningCollection.getID());

                if (myCollections.containsKey(cKey))
                {
                    Integer x = (Integer) myCounts.get(cKey);
                    int myCount = x.intValue() + 1;

                    // increment count for that collection
                    myCounts.put(cKey, new Integer(myCount));
                }
                else
                {
                    // store and initialize count
                    myCollections.put(cKey, owningCollection);
                    myCounts.put(cKey, new Integer(1));
                }

                // store the item
                myItems.put(myKey, myItem);
            }

            // remove this collection's entry because we already have a native
            // count
            myCollections.remove(new Integer(myCollection.getID()));

            // sort items - later
            // show page
            request.setAttribute("collection", myCollection);
            request.setAttribute("count_native", new Integer(count_native));
            request.setAttribute("count_import", new Integer(count_import));
            request.setAttribute("items", myItems);
            request.setAttribute("collections", myCollections);
            request.setAttribute("collection_counts", myCounts);
            request
                    .setAttribute("all_collections", Collection
                            .findAll(context));

            // show this page when we're done
            jspPage = "itemmap-main.jsp";

            // show the page
            JSPManager.showJSP(request, response, jspPage);
        }
        /*
         * else if( action.equals("add") ) { int itemID =
         * UIUtil.getIntParameter(request, "item_id"); String handle =
         * (String)request.getParameter("handle"); boolean error = true; Item
         * itemToAdd = null;
         * 
         * if( itemID > 0 ) { itemToAdd = Item.find(context, itemID);
         * 
         * if( itemToAdd != null ) error = false; } else if(handle != null &&
         * !handle.equals("")) { DSpaceObject
         * dso=HandleManager.resolveToObject(context, handle);
         * 
         * if(dso != null && dso.getType() == Constants.ITEM) { itemToAdd =
         * (Item)dso; error = false; } }
         * 
         * //FIXME: error handling! if( !error ) { String myTitle =
         * itemToAdd.getDC("title",null,Item.ANY)[0].value; String ownerName =
         * itemToAdd.getOwningCollection().getMetadata("name");
         *  // hook up item, but first, does it belong already? TableRowIterator
         * tri = DatabaseManager.query(context, "collection2item", "SELECT
         * collection2item.* FROM collection2item WHERE " + "collection_id=" +
         * myCollection.getID() + " AND item_id=" + itemToAdd.getID());
         * 
         * if(tri.hasNext()) { request.setAttribute("message", "Item is already
         * part of that collection!"); } else { // Create mapping
         * myCollection.addItem( itemToAdd );
         *  // set up a nice 'done' message request.setAttribute("message",
         * "Item added successfully: <br> " + myTitle + " <br> From Collection:
         * <br> " + ownerName);
         *  }
         * 
         * request.setAttribute("collection", myCollection);
         *  // show this page when we're done jspPage = "itemmap-info.jsp";
         *  // show the page JSPManager.showJSP(request, response, jspPage); }
         * else { // Display an error } } else if( action.equals("Add Entire
         * Collection") ) { int targetID = UIUtil.getIntParameter(request,
         * "collection2import");
         * 
         * Collection targetCollection = Collection.find(context, targetID);
         *  // get all items from that collection and add them if not // already
         * added
         *  // get all items to be added ItemIterator i =
         * targetCollection.getItems(); Map toAdd = new HashMap(); String
         * message = "";
         * 
         * while( i.hasNext() ) { Item myItem = i.next();
         * 
         * toAdd.put(new Integer(myItem.getID()), myItem); }
         *  // now see what we already have, removing dups from the 'toAdd' list
         * i = myCollection.getItems();
         * 
         * while( i.hasNext() ) { Item myItem = i.next(); Integer myKey = new
         * Integer(myItem.getID());
         *  // remove works even if key isn't present toAdd.remove(myKey); }
         *  // what's left in toAdd should be added Iterator addKeys =
         * toAdd.keySet().iterator();
         * 
         * while( addKeys.hasNext() ) { Item myItem =
         * (Item)toAdd.get(addKeys.next()); myCollection.addItem(myItem);
         * message += " <br> Added item ID: " + myItem.getID(); }
         * 
         * request.setAttribute("message", message);
         * request.setAttribute("collection", myCollection);
         *  // show this page when we're done jspPage = "itemmap-info.jsp";
         *  // show the page JSPManager.showJSP(request, response, jspPage); }
         */
        else if (action.equals("Remove"))
        {
            // get item IDs to remove
            String[] itemIDs = request.getParameterValues("item_ids");
            String message = "";

            for (int j = 0; j < itemIDs.length; j++)
            {
                int i = Integer.parseInt(itemIDs[j]);
                message += ("<br>Remove item " + i);

                Item myItem = Item.find(context, i);

                // make sure item doesn't belong to this collection
                if (!myItem.isOwningCollection(myCollection))
                {
                    myCollection.removeItem(myItem);
                }
            }

            request.setAttribute("message", message);
            request.setAttribute("collection", myCollection);

            // show this page when we're done
            jspPage = "itemmap-info.jsp";

            // show the page
            JSPManager.showJSP(request, response, jspPage);
        }
        else if (action.equals("Add"))
        {
            // get item IDs to add
            String[] itemIDs = request.getParameterValues("item_ids");
            String message = "";

            if (itemIDs == null)
            {
                message = "No items selected, none added.";
            }
            else
            {
                for (int j = 0; j < itemIDs.length; j++)
                {
                    int i = Integer.parseInt(itemIDs[j]);

                    Item myItem = Item.find(context, i);

                    if (AuthorizeManager.authorizeActionBoolean(context,
                            myItem, Constants.READ))
                    {
                        // make sure item doesn't belong to this collection
                        if (!myItem.isOwningCollection(myCollection))
                        {
                            myCollection.addItem(myItem);
                            message += ("<br>Added item " + i);
                        }
                    }
                }
            }

            request.setAttribute("message", message);
            request.setAttribute("collection", myCollection);

            // show this page when we're done
            jspPage = "itemmap-info.jsp";

            // show the page
            JSPManager.showJSP(request, response, jspPage);
        }
        else if (action.equals("Search Authors"))
        {
            // find all items with a matching author string and not currently in
            // this collection
            // sorting by date would be ideal...
            String myQuery = (String) request.getParameter("namepart");

            TableRowIterator tri = DatabaseManager
                    .query(
                            context,
                            "SELECT * from ItemsByAuthor WHERE sort_author like '%"
                                    + myQuery.toLowerCase()
                                    + "%' AND item_id NOT IN (SELECT item_id FROM collection2item WHERE collection_id="
                                    + myCollection.getID() + ")");

            Map items = new HashMap();

            while (tri.hasNext())
            {
                TableRow tr = tri.next();

                // now instantiate and pass items to 'Add' page
                int itemID = tr.getIntColumn("item_id");

                Item myItem = Item.find(context, itemID);

                // only put on list if you can read item
                if (AuthorizeManager.authorizeActionBoolean(context, myItem,
                        Constants.READ))
                {
                    items.put(new Integer(itemID), myItem);
                }
            }

            request.setAttribute("collection", myCollection);
            request.setAttribute("browsetext", "Items matching author '"
                    + myQuery + "'");
            request.setAttribute("items", items);
            request.setAttribute("browsetype", new String("Add"));

            jspPage = "itemmap-browse.jsp";
            JSPManager.showJSP(request, response, jspPage);
        }
        else if (action.equals("browse"))
        {
            // target collection to browse
            int t = UIUtil.getIntParameter(request, "t");

            Collection targetCollection = Collection.find(context, t);

            // now find all imported items from that collection
            // seemingly inefficient, but database should have this query cached
            ItemIterator i = myCollection.getItems();
            Map items = new HashMap();

            while (i.hasNext())
            {
                Item myItem = i.next();

                if (myItem.isOwningCollection(targetCollection))
                {
                    Integer myKey = new Integer(myItem.getID());
                    items.put(myKey, myItem);
                }
            }

            request.setAttribute("collection", myCollection);
            request.setAttribute("browsetext", targetCollection
                    .getMetadata("name"));
            request.setAttribute("items", items);
            request.setAttribute("browsetype", new String("Remove"));

            // show this page when we're done
            jspPage = "itemmap-browse.jsp";

            // show the page
            JSPManager.showJSP(request, response, jspPage);
        }

        context.complete();
    }
}