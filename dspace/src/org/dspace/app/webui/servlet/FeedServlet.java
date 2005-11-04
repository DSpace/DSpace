/*
 * FeedServlet.java
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

package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Description;
import com.sun.syndication.feed.rss.Image;
import com.sun.syndication.feed.rss.TextInput;
import com.sun.syndication.io.WireFeedOutput;
import com.sun.syndication.io.FeedException;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Bitstream;
import org.dspace.content.DCValue;
import org.dspace.content.DCDate;
import org.dspace.core.LogManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.browse.Browse;
import org.dspace.browse.BrowseScope;
import org.dspace.handle.HandleManager;
import org.dspace.search.Harvest;

/**
 * Servlet for handling requests for a syndication feed. The Handle of the collection
 * or community is extracted from the URL, e.g: <code>/feed/rss_1.0/1234/5678</code>.
 * Currently supports only RSS feed formats.
 * 
 * @author Ben Bosman, Richard Rodgers
 * @version $Revision$
 */
public class FeedServlet extends DSpaceServlet
{
	// one hour in milliseconds
	private static final long HOUR_MSECS = 60 * 60 * 1000;
    /** log4j category */
    private static Logger log = Logger.getLogger(FeedServlet.class);
    
    // are syndication feeds enabled?
    private static boolean enabled = false;
    // number of DSpace items per feed
    private static int itemCount = 0;
    // optional cache of feeds
    private static Map feedCache = null;
    // maximum size of cache - 0 means caching disabled
    private static int cacheSize = 0;
    // how many days to keep a feed in cache before checking currency
    private static int cacheAge = 0;
    // supported syndication formats
    private static List formats = null;
    
    static
    {
    	enabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    }
    
    public void init()
    {
    	// read rest of config info if enabled
    	if (enabled)
    	{
    		String fmtsStr = ConfigurationManager.getProperty("webui.feed.formats");
    		if ( fmtsStr != null )
    		{
    			formats = new ArrayList();
    			String[] fmts = fmtsStr.split(",");
    			for (int i = 0; i < fmts.length; i++)
    			{
    				formats.add(fmts[i]);
    			}
    		}
    		itemCount = ConfigurationManager.getIntProperty("webui.feed.items");
    		cacheSize = ConfigurationManager.getIntProperty("webui.feed.cache.size");
    		if (cacheSize > 0)
    		{
    			feedCache = new HashMap();
    	   		cacheAge = ConfigurationManager.getIntProperty("webui.feed.cache.age");
    		}
    	}
    }

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
	{
        String path = request.getPathInfo();
        String feedType = null;
        String handle = null;

        if (path != null)
        {
            // substring(1) is to remove initial '/'
            path = path.substring(1);
            int split = path.indexOf("/");
            if (split != -1)
            {
            	feedType = path.substring(0,split);
            	handle = path.substring(split+1);
            }
        }

        // Determine if handle is a valid reference
        DSpaceObject dso = HandleManager.resolveToObject(context, handle);
        if (! enabled || dso == null || 
        	(dso.getType() != Constants.COLLECTION && dso.getType() != Constants.COMMUNITY) )
        {
            log.info(LogManager.getHeader(context, "invalid_id", "path=" + path));
            JSPManager.showInvalidIDError(request, response, path, -1);
            return;
        }
        
        // Determine if requested format is supported
        if( feedType == null || ! formats.contains( feedType ) )
        {
            log.info(LogManager.getHeader(context, "invalid_syndformat", "path=" + path));
            JSPManager.showInvalidIDError(request, response, path, -1);
            return;
        }
        
        // Lookup or generate the feed
        Channel channel = null;
        if (feedCache != null)
        {
            // Cache key is handle
        	CacheFeed cFeed = (CacheFeed)feedCache.get(handle);
        	if (cFeed != null)  // cache hit, but...
        	{
        		// Is the feed current?
        		boolean cacheFeedCurrent = false;
        		if (cFeed.timeStamp + (cacheAge * HOUR_MSECS) < System.currentTimeMillis())
        		{
        			cacheFeedCurrent = true;
        		}
        		// Not current, but have any items changed since feed was created/last checked?
        		else if ( ! itemsChanged(context, dso, cFeed.timeStamp))
        		{
        			// no items have changed, re-stamp feed and use it
          			cFeed.timeStamp = System.currentTimeMillis();
          			cacheFeedCurrent = true;
        		}
        		if (cacheFeedCurrent)
        		{
        			channel = cFeed.access();		
        		}
        	}
        }
        
        // either not caching, not found in cache, or feed in cache not current
        if (channel == null)
        {
        	channel = generateFeed(context, dso);
        	if (feedCache != null)
        	{
        		cache(handle, new CacheFeed(channel));
        	}
        }
        
        // set the feed to the requested type & return it
        channel.setFeedType(feedType);
        WireFeedOutput feedWriter = new WireFeedOutput();
        try
        {
        	feedWriter.output(channel, response.getWriter());
        }
        catch( FeedException fex )
        {
        	throw new IOException(fex.getMessage());
        }
    }
       
    private boolean itemsChanged(Context context, DSpaceObject dso, long timeStamp)
            throws SQLException
    {
        // construct start and end dates
        DCDate dcStartDate = new DCDate( new Date(timeStamp) );
        DCDate dcEndDate = new DCDate( new Date(System.currentTimeMillis()) );

        // convert dates to ISO 8601, stripping the time
        String startDate = dcStartDate.toString().substring(0, 10);
        String endDate = dcEndDate.toString().substring(0, 10);
        
        // this invocation should return a non-empty list if even 1 item has changed
        return (Harvest.harvest(context, dso, startDate, endDate,
        		                0, 1, false, false, false).size() > 0);
    }
    
    /**
     * Generate a syndication feed for a collection or community
     * or community
     * 
     * @param context	the DSpace context object
     * 
     * @param dso		DSpace object - collection or community
     * 
     * @return		an object representing the feed
     */
    private Channel generateFeed(Context context, DSpaceObject dso)
    		throws IOException, SQLException
    {
    	// container-level elements
    	String objectUrl = ConfigurationManager.getBooleanProperty("webui.feed.localresolve")
    		? HandleManager.resolveToURL(context, dso.getHandle())
    		: HandleManager.getCanonicalForm(dso.getHandle());   	
    	String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
    	String type = null;
    	String description = null;
    	String title = null;
    	Bitstream logo = null;
    	// browse scope
    	BrowseScope scope = new BrowseScope(context);
    	// the feed
    	Channel channel = new Channel();
    	
    	if (dso.getType() == Constants.COLLECTION)
    	{
    		type = "collection";
    		Collection col = (Collection)dso;
           	description = col.getMetadata("short_description");
           	title = col.getMetadata("name");
           	logo = col.getLogo();
           	scope.setScope(col); 
        }
    	else if (dso.getType() == Constants.COMMUNITY)
    	{
    		type = "community";
    		Community comm = (Community)dso;
           	description = comm.getMetadata("short_description");
           	title = comm.getMetadata("name");
           	logo = comm.getLogo();
    		scope.setScope(comm);
    	}
    		
		// put in container-level data
        channel.setDescription(description);
        channel.setLink(objectUrl);
        channel.setTitle("DSpace " + type + ": " + title);
    	if (logo != null)
    	{
    		// we use the path to the logo for this, the logo itself cannot
    	    // be contained in the rdf. Not all RSS-viewers show this logo.
    		Image image = new Image();
    		image.setLink(objectUrl);
    		image.setTitle("The Channel Image");
    		image.setUrl(dspaceUrl + "/retrieve/" + logo.getID());
    	    channel.setImage(image);
    	}
    	// this is a direct link to the search-engine of dspace. It searches
    	// in the current collection. Since the current version of DSpace
    	// can't search within collections anymore, this works only in older
    	// version until this bug is fixed.
    	TextInput input = new TextInput();
    	input.setLink(dspaceUrl + "/simple-search");
    	input.setDescription( "Search the Channel" );
    	input.setTitle("The " + type + "'s search engine");
    	input.setName("s");
    	channel.setTextInput(input);
		   		
       	// gather & add items to the feed.
    	scope.setTotal(itemCount);
    	List results = Browse.getLastSubmitted(scope);
    	List items = new ArrayList();
    	for ( int i = 0; i < results.size(); i++ )
    	{
    		items.add( itemFromDSpaceItem(context, (Item)results.get(i)) );
    	}
    	channel.setItems(items);
        
    	return channel;
    }
    
    /**
     * The metadata fields of the given item will be added to the given feed.
     * 
     * @param context	DSpace context object
     * 
     * @param dspaceItem	DSpace Item
     * 
     * @return an object representing a feed entry
     */
    private com.sun.syndication.feed.rss.Item itemFromDSpaceItem(Context context,
    		                                                     Item dspaceItem)
    	throws SQLException
    {
        com.sun.syndication.feed.rss.Item rssItem = 
        	new com.sun.syndication.feed.rss.Item();
        
    	String itHandle = ConfigurationManager.getBooleanProperty("webui.feed.localresolve")
		? HandleManager.resolveToURL(context, dspaceItem.getHandle())
		: HandleManager.getCanonicalForm(dspaceItem.getHandle());

        rssItem.setLink(itHandle);
        
    	String title = getDC(dspaceItem, "title", null, Item.ANY);
    	if (title == null)
    	{
    		title = "no_title";
    	}
        rssItem.setTitle(title);
        // We put some metadata in the description field. This field is
        // displayed by most RSS viewers
        StringBuffer descBuf = new StringBuffer();
        if ( ! "no_title".equals(title) )
        {
        	descBuf.append("title: ");
        	descBuf.append(title);
        	descBuf.append(" ");
        }
        DCValue[] authors = dspaceItem.getDC("contributor", "author", Item.ANY);
        if (authors.length > 0)
        {
        	descBuf.append("authors: ");
            for (int i = 0; i < authors.length; i++)
            {
            	descBuf.append(authors[i].value);
            	if (i < authors.length - 1)
            	{
            		descBuf.append("; ");
            	}
            }
            descBuf.append("\n<br>");
        }
        DCValue[] editors = dspaceItem.getDC("contributor", "editor", Item.ANY);
        if (editors.length > 0)
        {
        	descBuf.append("editors: ");
            for (int i = 0; i < editors.length; i++)
            {
            	descBuf.append(editors[i].value);
            	if (i < editors.length - 1)
            	{
            		descBuf.append("; ");
            	}
            }
            descBuf.append("\n<br>");
        }
        String abstr = getDC(dspaceItem, "description", "abstract", Item.ANY);
        if (abstr != null)
        {
        	descBuf.append("abstract: ");
        	descBuf.append(abstr);
        	descBuf.append("\n<br>");
        }
        String desc = getDC(dspaceItem, "description", null, Item.ANY);
        if (desc != null)
        {
        	descBuf.append("description: ");
        	descBuf.append(desc);
        	descBuf.append("\n<br>");
        }

        Description descrip = new Description();
        descrip.setValue(descBuf.toString());
        rssItem.setDescription(descrip);
            
        String dcDate = getDC(dspaceItem, "date", "issued", Item.ANY);
        if (dcDate == null)
        {
        	dcDate = getDC(dspaceItem, "date", Item.ANY, Item.ANY);
        }
        if (dcDate != null)
        {
        	rssItem.setPubDate((new DCDate(dcDate)).toDate());
        }
        return rssItem;
    }
    
    /**
      * @param item
      *            The item from which the metadata fields are used
      * @param element
      *            The Dublin Core element
      * @param qualifier
      *            The Dublin Core qualifier
      * @param lang
      *            The Dublin Core language
      * @return If there exists a Dublin Core value with the given element,
      *         qualifier and language, return it, else null
      */
     private String getDC(Item item, String element, String qualifier, String lang)
     {
    	 String dcVal = null;
         try
         {
             dcVal = item.getDC(element, qualifier, lang)[0].value;
         }
         catch (ArrayIndexOutOfBoundsException e)
         {
             dcVal = null;
         }
         return dcVal;
     }

    /************************************************
     * private cache management classes and methods *
     ************************************************/
     
	/**
     * Add a feed to the cache - reducing the size of the cache by 1 to make room if
     * necessary. The removed entry has an access count equal to the minumum in the cache.
     * @param feedKey
     *            The cache key for the feed
     * @param newFeed
     *            The CacheFeed feed to be cached
     */ 
    private static void cache(String feedKey, CacheFeed newFeed)
    {
		// remove older feed to make room if cache full
		if (feedCache.size() >= cacheSize)
		{
	    	// cache profiling data
	    	int total = 0;
	    	String minKey = null;
	    	CacheFeed minFeed = null;
	    	CacheFeed maxFeed = null;
	    	
	    	Iterator iter = feedCache.keySet().iterator();
	    	while (iter.hasNext())
	    	{
	    		String key = (String)iter.next();
	    		CacheFeed feed = (CacheFeed)feedCache.get(key);
	    		if (minKey != null)
	    		{
	    			if (feed.hits < minFeed.hits)
	    			{
	    				minKey = key;
	    				minFeed = feed;
	    			}
	    			if (feed.hits >= maxFeed.hits)
	    			{
	    				maxFeed = feed;
	    			}
	    		}
	    		else
	    		{
	    			minKey = key;
	    			minFeed = maxFeed = feed;
	    		}
	    		total += feed.hits;
	    	}
	    	// log a profile of the cache to assist administrator in tuning it
	    	int avg = total / feedCache.size();
	    	String logMsg = "feedCache() - size: " + feedCache.size() +
	    	                " Hits - total: " + total + " avg: " + avg +
	    	                " max: " + maxFeed.hits + " min: " + minFeed.hits;
	    	log.info(logMsg);
	    	// remove minimum hits entry
	    	feedCache.remove(minKey);
		}
    	// add feed to cache
		feedCache.put(feedKey, newFeed);
    }
        
    /**
     * Class to instrument accesses & currency of a given feed in cache
     */  
    private class CacheFeed
	{
    	// currency timestamp
    	public long timeStamp = 0L;
    	// access count
    	public int hits = 0;
    	// the feed
    	private Channel feed = null;
    	
    	public CacheFeed(Channel feed)
    	{
    		this.feed = feed;
    		timeStamp = System.currentTimeMillis();
    	}
    	    	
    	public Channel access()
    	{
    		++hits;
    		return feed;
    	}
	}
}
