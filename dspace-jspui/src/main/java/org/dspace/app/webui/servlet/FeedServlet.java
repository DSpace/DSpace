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

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Description;
import com.sun.syndication.feed.rss.Image;
import com.sun.syndication.feed.rss.TextInput;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.sort.SortOption;
import org.dspace.sort.SortException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.search.Harvest;
import org.dspace.uri.ResolvableIdentifier;
import org.dspace.uri.IdentifierFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * Servlet for handling requests for a syndication feed. The URI of the collection
 * or community is extracted from the URL, e.g: <code>/feed/rss_1.0/xyz:1234/5678</code>.
 * Currently supports only RSS feed formats.
 * 
 * @author Ben Bosman, Richard Rodgers, James Rutherford
 * @version $Revision$
 */
public class FeedServlet extends DSpaceServlet
{
	//	key for site-wide feed
	public static final String SITE_FEED_KEY = "site";
	
	// one hour in milliseconds
	private static final long HOUR_MSECS = 60 * 60 * 1000;
    /** log4j category */
    private static Logger log = Logger.getLogger(FeedServlet.class);
    private String clazz = "org.dspace.app.webui.servlet.FeedServlet";

    
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
    
    // localized resource bundle
    private static ResourceBundle labels = null;
    
    //default fields to display in item description
    private static String defaultDescriptionFields = "dc.title, dc.contributor.author, dc.contributor.editor, dc.description.abstract, dc.description";

    
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
        String uri = null;

        if(labels==null)
        {    
            // Get access to the localized resource bundle
            Locale locale = request.getLocale();
            labels = ResourceBundle.getBundle("Messages", locale);
        }
        
        if (path != null)
        {
            // substring(1) is to remove initial '/'
            path = path.substring(1);
            int split = path.indexOf("/");
            if (split != -1)
            {
            	feedType = path.substring(0,split);
            	uri = path.substring(split+1);
            }
        }

        DSpaceObject dso = null;
        
        //as long as this is not a site wide feed, 
        //attempt to retrieve the Collection or Community object
        if(!uri.equals(SITE_FEED_KEY))
        { 	
        	// Determine if the URI is a valid reference
            ResolvableIdentifier di = IdentifierFactory.resolve(context, uri);
            //ExternalIdentifierDAO dao = ExternalIdentifierDAOFactory.getInstance(context);
            //ExternalIdentifier identifier = dao.retrieve(uri);
            //ObjectIdentifier oi = identifier.getObjectIdentifier();
            dso = di.getObject(context);
        }
        
        if (! enabled || (dso != null && 
        	(dso.getType() != Constants.COLLECTION && dso.getType() != Constants.COMMUNITY)) )
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
            // Cache key is URI
        	CacheFeed cFeed = (CacheFeed)feedCache.get(uri);
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
        		cache(uri, new CacheFeed(channel));
        	}
        }
        
        // set the feed to the requested type & return it
        channel.setFeedType(feedType);
        WireFeedOutput feedWriter = new WireFeedOutput();
        try
        {
        	response.setContentType("text/xml; charset=UTF-8");
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
        try {
        	return (Harvest.harvest(context, dso, startDate, endDate,
        		                0, 1, false, false, false).size() > 0);
        }
        catch (ParseException pe)
        {
        	// This should never get thrown as we have generated the dates ourselves
        	return false;
        }
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
    	try
    	{
    		// container-level elements  	
    		String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
    		String type = null;
    		String description = null;
    		String title = null;
    		Bitstream logo = null;
    		// browse scope
    		// BrowseScope scope = new BrowseScope(context);
    		
    		// new method of doing the browse:
    		String idx = ConfigurationManager.getProperty("recent.submissions.sort-option");
    		if (idx == null)
    		{
    			throw new IOException("There is no configuration supplied for: recent.submissions.sort-option");
    		}
    		BrowseIndex bix = BrowseIndex.getItemBrowseIndex();
    		if (bix == null)
    		{
    			throw new IOException("There is no browse index with the name: " + idx);
    		}
    		
    		BrowserScope scope = new BrowserScope(context);
    		scope.setBrowseIndex(bix);
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(idx))
                    scope.setSortBy(so.getNumber());
            }
            scope.setOrder(SortOption.DESCENDING);
    		
    		// the feed
    		Channel channel = new Channel();
    		
    		//Special Case: if DSpace Object passed in is null, 
    		//generate a feed for the entire DSpace site!
    		if(dso == null)
    		{
    			channel.setTitle(ConfigurationManager.getProperty("dspace.name"));
    			channel.setLink(dspaceUrl);
    			channel.setDescription(labels.getString(clazz + ".general-feed.description"));
    		}
    		else //otherwise, this is a Collection or Community specific feed
    		{
    			if (dso.getType() == Constants.COLLECTION)
    			{
    				type = labels.getString(clazz + ".feed-type.collection");
    				Collection col = (Collection)dso;
    				description = col.getMetadata("short_description");
    				title = col.getMetadata("name");
    				logo = col.getLogo();
    				// scope.setScope(col); 
    				scope.setBrowseContainer(col); 
    			}
    			else if (dso.getType() == Constants.COMMUNITY)
    			{
    				type = labels.getString(clazz + ".feed-type.community");
    				Community comm = (Community)dso;
    				description = comm.getMetadata("short_description");
    				title = comm.getMetadata("name");
    				logo = comm.getLogo();
    				// scope.setScope(comm); 
    				scope.setBrowseContainer(comm); 
    			}
    			
                        String objectUrl = "";
                        if (dso.getExternalIdentifier() != null)
                        {
                                objectUrl = ConfigurationManager.getBooleanProperty("webui.feed.localresolve")
                                    ? IdentifierFactory.getURL(dso).toString()
                                    : dso.getExternalIdentifier().getURI().toString();
                        }
                        else
                        {
                                // If no external identifier is available, use the local URL
                                objectUrl = IdentifierFactory.getURL(dso).toString();
                        }

    			// put in container-level data
    			channel.setDescription(description);
    			channel.setLink(objectUrl);
    			//build channel title by passing in type and title
    			String channelTitle = MessageFormat.format(labels.getString(clazz + ".feed.title"),
    					new Object[]{type, title});
    			channel.setTitle(channelTitle);
    			
    			//if collection or community has a logo
    			if (logo != null)
    			{
    				// we use the path to the logo for this, the logo itself cannot
    				// be contained in the rdf. Not all RSS-viewers show this logo.
    				Image image = new Image();
    				image.setLink(objectUrl);
    				image.setTitle(labels.getString(clazz + ".logo.title"));
    				image.setUrl(dspaceUrl + "/retrieve/" + logo.getID());
    				channel.setImage(image);
    			}
    		}
    		
    		// this is a direct link to the search-engine of dspace. It searches
    		// in the current collection. Since the current version of DSpace
    		// can't search within collections anymore, this works only in older
    		// version until this bug is fixed.
    		TextInput input = new TextInput();
    		input.setLink(dspaceUrl + "/simple-search");
    		input.setDescription( labels.getString(clazz + ".search.description") );
    		
    		String searchTitle = ""; 
    		
    		//if a "type" of feed was specified, build search title off that
    		if(type!=null)
    		{
    			searchTitle = MessageFormat.format(labels.getString(clazz + ".search.title"),
    					new Object[]{type});
    		}
    		else //otherwise, default to a more generic search title
    		{
    			searchTitle = labels.getString(clazz + ".search.title.default");
    		}
    		
    		input.setTitle(searchTitle);
    		input.setName(labels.getString(clazz + ".search.name"));
    		channel.setTextInput(input);
    		
    		// gather & add items to the feed.
    		scope.setResultsPerPage(itemCount);
    		
    		BrowseEngine be = new BrowseEngine(context);
    		BrowseInfo bi = be.browseMini(scope);
    		Item[] results = bi.getBrowseItemResults(context);
    		List items = new ArrayList();
    		for (int i = 0; i < results.length; i++)
    		{
    			items.add(itemFromDSpaceItem(context, results[i]));
    		}
    		
    		channel.setItems(items);

            // If the description is null, replace it with an empty string
            // to avoid a FeedException
            if (channel.getDescription() == null)
                channel.setDescription("");

            return channel;
    	}
        catch (SortException se)
        {
            log.error("caught exception: ", se);
            throw new IOException(se.getMessage());
        }
    	catch (BrowseException e)
    	{
    		log.error("caught exception: ", e);
    		throw new IOException(e.getMessage());
    	}
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
        
        //get the title and date fields
        String titleField = ConfigurationManager.getProperty("webui.feed.item.title");
        if (titleField == null)
        {
            titleField = "dc.title";
        }
        
        String dateField = ConfigurationManager.getProperty("webui.feed.item.date");
        if (dateField == null)
        {
            dateField = "dc.date.issued";
        }   
        
        //Set item URI
    	String link = ConfigurationManager.getBooleanProperty("webui.feed.localresolve")
            ? IdentifierFactory.getURL(dspaceItem).toString()
            : dspaceItem.getExternalIdentifier().getURI().toString();

        rssItem.setLink(link);
        
        //get first title
        String title = null;
        try
        {
            title = dspaceItem.getMetadata(titleField)[0].value;
           
        }
        catch (ArrayIndexOutOfBoundsException e)
        { 
            title = labels.getString(clazz + ".notitle");
        }
        rssItem.setTitle(title);
        
        // We put some metadata in the description field. This field is
        // displayed by most RSS viewers
        String descriptionFields = ConfigurationManager
                                        .getProperty("webui.feed.item.description");

        if (descriptionFields == null)
        {     
            descriptionFields = defaultDescriptionFields;
        }
        
        //loop through all the metadata fields to put in the description
        StringBuffer descBuf = new StringBuffer();  
        StringTokenizer st = new StringTokenizer(descriptionFields, ",");

        while (st.hasMoreTokens())
        {
            String field = st.nextToken().trim();
            boolean isDate = false;
         
            // Find out if the field should rendered as a date
            if (field.indexOf("(date)") > 0)
            {
                field = field.replaceAll("\\(date\\)", "");
                isDate = true;
            }

            
            //print out this field, along with its value(s)
            DCValue[] values = dspaceItem.getMetadata(field);
           
            if(values != null && values.length>0)
            {
                //as long as there is already something in the description
                //buffer, print out a few line breaks before the next field
                if(descBuf.length() > 0)
                {
                    descBuf.append("\n<br/>");
                    descBuf.append("\n<br/>");
                }
                    
                String fieldLabel = null;
                try
                {
                    fieldLabel = labels.getString("metadata." + field);
                }
                catch(java.util.MissingResourceException e) {}
                
                if(fieldLabel !=null && fieldLabel.length()>0)
                    descBuf.append(fieldLabel + ": ");
                
                for(int i=0; i<values.length; i++)
                {    
                    String fieldValue = values[i].value;
                    if(isDate)
                        fieldValue = (new DCDate(fieldValue)).toString();
                    descBuf.append(fieldValue);
                    if (i < values.length - 1)
                    {
                        descBuf.append("; ");
                    }
                }
            }
            
        }//end while   
        Description descrip = new Description();
        descrip.setValue(descBuf.toString());
        rssItem.setDescription(descrip);
            
        
        // set date field
        String dcDate = null;
        try
        {
            dcDate = dspaceItem.getMetadata(dateField)[0].value;
           
        }
        catch (ArrayIndexOutOfBoundsException e)
        { 
        }
        if (dcDate != null)
        {
            rssItem.setPubDate((new DCDate(dcDate)).toDate());
        }
        
        return rssItem;
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
