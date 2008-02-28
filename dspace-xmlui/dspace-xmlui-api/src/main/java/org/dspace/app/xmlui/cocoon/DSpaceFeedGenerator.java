/*
 * DSpaceFeedGenerator.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/01/10 04:28:19 $
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

package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.apache.cocoon.util.HashUtil;
import org.apache.cocoon.xml.dom.DOMStreamer;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseItem;
import org.dspace.browse.BrowserScope;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
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
import org.dspace.handle.HandleManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Description;
import com.sun.syndication.feed.rss.Image;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;

/**
 * 
 * Generate a syndication feed for DSpace, either a community or collection 
 * or the whole repository. This code was adapted from the syndication found 
 * in DSpace's JSP implementation, "org.dspace.app.webui.servlet.FeedServlet".
 *
 * Once thing that has been modified from DSpace's JSP implementation is what 
 * is placed inside an item's description, we've changed it so that the list 
 * of metadata fields is scanned until a value is found and the first one 
 * found is used as the description. This means that we look at the abstract, 
 * description, alternative title, title, etc... to and the first metadata 
 * value found is used.
 *
 * I18N: Feed's are internationalized, meaning that they may contain refrences
 * to messages contained in the global messages.xml file using cocoon's i18n
 * schema. However the library used to build the feeds does not understand 
 * this schema to work around this limitation I created a little hack. It 
 * basicaly works like this, when text that needs to be localized is put into 
 * the feed it is allways mangled such that a prefix is added to the messages's 
 * key. Thus if the key were "xmlui.feed.text" then the resulting text placed 
 * into the feed would be "I18N:xmlui.feed.text". After the library is finished 
 * and produced it's final result the output is traversed to find these 
 * occurances ande replace them with proper cocoon i18n elements.
 * 
 * 
 * 
 * @author Scott Phillips, Ben Bosman, Richard Rodgers
 */

public class DSpaceFeedGenerator extends AbstractGenerator 
		implements Configurable, CacheableProcessingComponent, Recyclable
{
    private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

	/** The feed's requested format */
	private String format = null;
	
	/** The feed's scope, null if no scope */
	private String handle = null;
	
    /** number of DSpace items per feed */
    private static int itemCount = 0;
    
	/**	default fields to display in item description */
    private static String defaultDescriptionFields = "dc.description.abstract, dc.description, dc.title.alternative, dc.title";

    
    /** The prefix used to differentate i18n keys */
    private static final String I18N_PREFIX = "I18N:";
    
    /** Cocoon's i18n namespace */
    private static final String I18N_NAMESPACE = "http://apache.org/cocoon/i18n/2.1";
    
    
    /** Cache of this object's validitity */
    private DSpaceValidity validity = null;
    
    /** The cache of recently submitted items */
    private java.util.List<BrowseItem> recentSubmissionItems;
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
    	String key = "key:" + this.handle + ":" + this.format;
    	return HashUtil.hash(key);
    }

    /**
     * Generate the cache validity object.
     * 
     * The validity object will include the collection being viewed and 
     * all recently submitted items. This does not include the community / collection
     * hierarch, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
    		try
    		{
    			DSpaceValidity validity = new FeedValidity();
    			
    			Context context = ContextUtil.obtainContext(objectModel);

    			DSpaceObject dso = null;
    			
    			if (handle != null && !handle.contains("site"))
    				dso = HandleManager.resolveToObject(context, handle);
    			
    			validity.add(dso);
    			
    			// add reciently submitted items
    			for(BrowseItem item : getRecientlySubmittedItems(context,dso))
    			{
    				validity.add(item);
    			}

    			this.validity = validity.complete();
    		}
	        catch (Exception e)
	        {
	            // Just ignore all errors and return an invalid cache.
	        }
    	}
    	return this.validity;
    }
    
    
    
    /**
     * Setup component wide configuration
     */
    public void configure(Configuration conf) throws ConfigurationException
    {
    	itemCount = ConfigurationManager.getIntProperty("webui.feed.items");
    }
    
    
    /**
     * Setup configuration for this request
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);
        
        
        this.format = par.getParameter("feedFormat", null);
        this.handle = par.getParameter("handle",null);
    }
    
    
    /**
     * Generate the syndication feed.
     */
    public void generate() throws IOException, SAXException, ProcessingException
    {
		try {
			Context context = ContextUtil.obtainContext(objectModel);
			DSpaceObject dso = null;
			
			if (handle != null && !handle.contains("site"))
			{
				dso = HandleManager.resolveToObject(context, handle);
				
				if (dso == null)
				{
					// If we were unable to find a handle then return page not found.
					throw new ResourceNotFoundException("Unable to find DSpace object matching the given handle: "+handle);
				}
				
				if (!(dso.getType() == Constants.COLLECTION || dso.getType() == Constants.COMMUNITY))
				{
					// The handle is valid but the object is not a container.
					throw new ResourceNotFoundException("Unable to syndicate DSpace object: "+handle);
				}
				
			}
    	
			Channel channel = generateFeed(context, dso);
			
			// set the feed to the requested type & return it
			channel.setFeedType(this.format);
			
	        WireFeedOutput feedWriter = new WireFeedOutput();
	        Document dom = feedWriter.outputW3CDom(channel);
	        unmangleI18N(dom);
	        DOMStreamer streamer = new DOMStreamer(contentHandler, lexicalHandler);
	        streamer.stream(dom);

		}
		catch (IllegalArgumentException iae)
		{
			throw new ResourceNotFoundException("Syndication feed format, '"+this.format+"', is not supported.");
		}
		catch (IOException e) 
        {
            throw new SAXException(e);
        }
		catch (FeedException fe) 
		{
			throw new SAXException(fe);
		}
		catch (SQLException sqle) 
		{
			throw new SAXException(sqle);
		}
    	
    }
    
    /**
     * Mange the i18n key into a text value that is later 
     * unmangled into a true i18n element for later localization.
     * 
     * @param key The i18n message key
     * @return A mangled text string encoding the key.
     */
    private String mangleI18N(String key)
    {
    	return I18N_PREFIX + key;
    }
    
    /**
     * Scan the document and replace any text nodes that begin 
     * with the i18n prefix with an actual i18n element that
     * can be processesed by the i18n transformer.
     * 
     * @param dom
     */
    private void unmangleI18N(Document dom)
    {
    	
    	NodeList elementNodes = dom.getElementsByTagName("*");
        
        for (int i = 0; i < elementNodes.getLength(); i++)
        {
        	NodeList textNodes = elementNodes.item(i).getChildNodes();
        	
        	for (int j = 0; j < textNodes.getLength(); j++)
	        {
        		
        		Node oldNode = textNodes.item(j);
        		// Check to see if the node is a text node, its value is not null, and it starts with the i18n prefix.
        		if (oldNode.getNodeType() == Node.TEXT_NODE && oldNode.getNodeValue() != null && oldNode.getNodeValue().startsWith(I18N_PREFIX))
	        	{
        			Node parent = oldNode.getParentNode();
        			String key = oldNode.getNodeValue().substring(I18N_PREFIX.length());
        			
        			Element newNode = dom.createElementNS(I18N_NAMESPACE, "text");
        			newNode.setAttribute("key", key);
        			newNode.setAttribute("catalogue", "default");

        			parent.replaceChild(newNode,oldNode);
	        	}

	        }
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
    	// container-level elements  	
    	String description = null;
    	String title = null;
    	Bitstream logo = null;
    	// the feed
    	Channel channel = new Channel();
    	
    	//Special Case: if DSpace Object passed in is null, 
    	//generate a feed for the entire DSpace site!
    	if(dso == null)
    	{
    		channel.setTitle(ConfigurationManager.getProperty("dspace.name"));
    		channel.setLink(resolveURL(null));
    		channel.setDescription(mangleI18N("xmlui.feed.general_description"));
    	}
    	else //otherwise, this is a Collection or Community specific feed
    	{
    		if (dso.getType() == Constants.COLLECTION)
	    	{
	    		Collection col = (Collection)dso;
	           	description = col.getMetadata("short_description");
	           	title = col.getMetadata("name");
	           	logo = col.getLogo();
	        }
	    	else if (dso.getType() == Constants.COMMUNITY)
	    	{
	    		Community comm = (Community)dso;
	           	description = comm.getMetadata("short_description");
	           	title = comm.getMetadata("name");
	           	logo = comm.getLogo();
	    	}
	    	
    		String objectUrl = resolveURL(dso);
  
			// put in container-level data
	        channel.setDescription(description);
	        channel.setLink(objectUrl);
	        channel.setTitle(title);
	        
	        //if collection or community has a logo
	        if (logo != null)
	    	{
	    		// we use the path to the logo for this, the logo itself cannot
	    	    // be contained in the rdf. Not all RSS-viewers show this logo.
	    		Image image = new Image();
	    		image.setLink(objectUrl);
	    		image.setTitle(mangleI18N("xmlui.feed.logo_title"));
	    		image.setUrl(resolveURL(null) + "/retrieve/" + logo.getID());
	    	    channel.setImage(image);
	    	}
    	}
		   		    	
    	// add reciently submitted items
    	List<com.sun.syndication.feed.rss.Item> items = 
    		new ArrayList<com.sun.syndication.feed.rss.Item>();
    	
		for(BrowseItem item : getRecientlySubmittedItems(context,dso))
		{
			items.add(itemFromDSpaceItem(context, item));
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
    		                                                     BrowseItem dspaceItem)
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
        
        //Set item handle
    	String itHandle = resolveURL(dspaceItem);

        rssItem.setLink(itHandle);
        
        //get first title
        String title = null;
        try
        {
            // FIXME: replace with this line once dspace 1.4.1 is released:
            //title = dspaceItem.getMetadata(titleField)[0].value;
            title = getMetadata(dspaceItem,titleField)[0].value;
           
        }
        catch (ArrayIndexOutOfBoundsException e)
        { 
            title = mangleI18N("xmlui.feed.untitled");
        }
        rssItem.setTitle(title);
        
        // Traverse the description fields untill we find one.
        String descriptionFields = 
        	ConfigurationManager.getProperty("webui.feed.item.description");

        if (descriptionFields == null)
        {     
            descriptionFields = defaultDescriptionFields;
        }
        
        //loop through all the metadata fields to put in the description
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
            //FIXME: replace with this line once dspace 1.4.1 is released:
            //DCValue[] values = dspaceItem.getMetadata(field);
            DCValue[] values = getMetadata(dspaceItem,field);
           
            if(values != null && values.length>0)
            {  
            	// We've found one, only take the first one if there
            	// are more than one.
                String fieldValue = values[0].value;
                if(isDate)
                    fieldValue = (new DCDate(fieldValue)).toString();
               
                Description descrip = new Description();
                descrip.setValue(fieldValue);
                rssItem.setDescription(descrip);
                
                // Once we've found one we can stop looking for more.
                break;
            }
            
        }//end while   
        // set date field
        String dcDate = null;
        try
        {
        	// FIXME: replace with this line once dspace 1.4.1 is released:
        	//dcDate = dspaceItem.getMetadata(dateField)[0].value;
        	dcDate = getMetadata(dspaceItem,dateField)[0].value;
           
        }
        catch (ArrayIndexOutOfBoundsException e)
        { 
        	// Ignore
        }
        
        if (dcDate != null)
        {
            rssItem.setPubDate((new DCDate(dcDate)).toDate());
        }
        
        return rssItem;
    }
    
    
    @SuppressWarnings("unchecked")
    private java.util.List<BrowseItem> getRecientlySubmittedItems(Context context, DSpaceObject dso) 
            throws SQLException
    {
    	if (recentSubmissionItems != null)
    		return recentSubmissionItems;

        String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
    	BrowserScope scope = new BrowserScope(context);
    	if (dso instanceof Collection)
    		scope.setCollection((Collection) dso);
    	else if (dso instanceof Community)
    		scope.setCommunity((Community) dso);
    	scope.setResultsPerPage(itemCount);

    	// FIXME Exception handling
    	try
    	{
            scope.setBrowseIndex(BrowseIndex.getItemBrowseIndex());
            for (SortOption so : SortOption.getSortOptions())
            {
                if (so.getName().equals(source))
                {
                    scope.setSortBy(so.getNumber());
                    scope.setOrder(SortOption.DESCENDING);
                }
            }

            BrowseEngine be = new BrowseEngine(context);
    		this.recentSubmissionItems = be.browse(scope).getResults();
    	}
    	catch (BrowseException bex)
    	{
    		log.error("Caught browse exception", bex);
    	}
        catch (SortException e)
        {
            log.error("Caught sort exception", e);
        }
        
    	return this.recentSubmissionItems;
    }
    
    /**
     * Return a url to the DSpace object, either use the official
     * handle for the item or build a url based upon the current server.
     * 
     * If the dspaceobject is null then a local url to the repository is generated.
     * 
     * @param dso The object to refrence, null if to the repository.
     * @return
     */
    private String resolveURL(DSpaceObject dso)
    {
    	if (dso == null)
    	{
    		// If no object given then just link to the whole repository, 
    		// since no offical handle exists so we have to use local resolution.
    		Request request = ObjectModelHelper.getRequest(objectModel);
			
			String url = (request.isSecure()) ? "https://" : "http://";
			url += ConfigurationManager.getProperty("dspace.hostname");
			url += ":" + request.getServerPort();
			url += request.getContextPath();
			return url;	
    	}
    	
		if (ConfigurationManager.getBooleanProperty("webui.feed.localresolve"))
		{
			Request request = ObjectModelHelper.getRequest(objectModel);
			
			String url = (request.isSecure()) ? "https://" : "http://";
			url += ConfigurationManager.getProperty("dspace.hostname");
			url += ":" + request.getServerPort();
			url += request.getContextPath();
			url += "/handle/" + dso.getHandle();
			return url;	
		}
		else
		{
			return HandleManager.getCanonicalForm(dso.getHandle()); 
		}
    }
    
    
    /** 
     * Recycle
     */
    
    public void recycle()
    {
    	this.format = null;
    	this.handle = null;
    	this.validity = null;
        this.recentSubmissionItems = null;
    	super.recycle();
    }
    
    /**
     * Extend the standard DSpaceValidity object to support assumed 
     * caching. Since feeds will constantly be requested we want to 
     * assume that a feed is still valid instead of checking it 
     * against the database anew everytime.
     * 
     * This validity object will assume that a cache is still valid, 
     * without rechecking it, for 24 hours.
     *
     */
    private class FeedValidity extends DSpaceValidity 
    {
		private static final long serialVersionUID = 1L;

		/**
    	 * How long should the cache assumed to be valid for, 
    	 * milliseconds * seconds * minutes * hours
    	 */
    	private static final long CACHE_AGE = 1000 * 60 * 60 * 24;
    	
    	/** When the cache's validity expires */
    	private long expires = 0;
    	
    	/**
         * When the validity is completed record a timestamp to check later.
         */
        public DSpaceValidity complete() 
        {    
        	this.expires = System.currentTimeMillis() + CACHE_AGE;
        	
        	return super.complete();
        }
        
        
        /**
         * Determine if the cache is still valid
         */
        public int isValid()
        {
            // Return true if we have a hash.
            if (this.completed)
            {
            	if (System.currentTimeMillis() < this.expires)
            	{
            		// If the cache hasn't expired the just assume that it is still valid.
            		return SourceValidity.VALID;
            	}
            	else
            	{
            		// The cache is past its age
            		return SourceValidity.UNKNOWN;
            	}
            }
            else
            {
            	// This is an error, state. We are being asked whether we are valid before
            	// we have been initialized.
                return SourceValidity.INVALID;
            }
        }

        /**
         * Determine if the cache is still valid based 
         * upon the other validity object.
         * 
         * @param other 
         *          The other validity object.
         */
        public int isValid(SourceValidity otherValidity)
        {
            if (this.completed)
            {
                if (otherValidity instanceof FeedValidity)
                {
                    FeedValidity other = (FeedValidity) otherValidity;
                    if (hash == other.hash)
                    {	
                    	// Update both cache's expiration time.
                    	this.expires = System.currentTimeMillis() + CACHE_AGE;
                    	other.expires = System.currentTimeMillis() + CACHE_AGE;
                    	
                        return SourceValidity.VALID;
                    }
                }
            }

            return SourceValidity.INVALID;
        }

    }
    
    
    
    /**
     * FIXME: This is a work around method, all uses of this private 
     * method method should use just call the getMetadata(string) method 
     * directly on an item. This method has been added to CVS head before
     * for the DSpace 1.4.1 release.
     * 
     * Retrieve metadata field values from a given metadata string
     * of the form <schema prefix>.<element>[.<qualifier>|.*]
     *
     * @param item
     * 			  The item to get metedata from. 
     * @param mdString
     *            The metadata string of the form
     *            <schema prefix>.<element>[.<qualifier>|.*]
     * @throws SQLException 
     */
    private static DCValue[] getMetadata(BrowseItem item, String mdString) throws SQLException
    {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");
        
        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens())
        {
            tokens[i] = dcf.nextToken().toLowerCase().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];
        
        DCValue[] values;
        if ("*".equals(qualifier))
        {
            values = item.getMetadata(schema, element, Item.ANY, Item.ANY);
        }
        else if ("".equals(qualifier))
        {
            values = item.getMetadata(schema, element, null, Item.ANY);
        }
        else
        {
            values = item.getMetadata(schema, element, qualifier, Item.ANY);
        }
        
        return values;
    }
    
}
