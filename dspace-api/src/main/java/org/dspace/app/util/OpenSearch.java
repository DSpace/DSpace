/*
 * OpenSearch.java
 *
 * Version: $Revision: 1.20 $
 *
 * Date: $Date: 2005/08/25 17:20:27 $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved. 
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
package org.dspace.app.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.w3c.dom.Document;

import org.jdom.DocType;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.DOMOutputter;
import org.jdom.output.XMLOutputter;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.dspace.search.QueryResults;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.atom.Category;
import com.sun.syndication.feed.atom.Content;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Link;
import com.sun.syndication.feed.atom.Person;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Description;
import com.sun.syndication.feed.rss.Image;
import com.sun.syndication.io.WireFeedOutput;
import com.sun.syndication.feed.module.opensearch.OpenSearchModule;
import com.sun.syndication.feed.module.opensearch.entity.OSQuery;
import com.sun.syndication.feed.module.opensearch.impl.OpenSearchModuleImpl;

/**
 * Utility Class with static methods for producing OpenSearch-compliant search results,
 * and the OpenSearch description document.
 * <p>
 * OpenSearch is a specification for describing and advertising search-engines
 * and their result formats. Commonly, RSS and Atom formats are used, which
 * the current implementation supports, as is HTML (used directly in browsers).
 * NB: this is baseline OpenSearch, no extensions currently supported.
 * </p>
 * <p>
 * The value of the "scope" parameter should either be absent (which means no
 * scope restriction), or the handle of a community or collection.
 * </p>
 * 
 * @author Richard Rodgers
 *
 */
public class OpenSearch
{
    // are open search queries enabled?
    private static boolean enabled = false;
    // supported results formats
    private static List<String> formats = null;
    // Namespaces used
    private static final String osNs = "http://a9.com/-/spec/opensearch/1.1/";
    private static final String atNs = "http://www.w3.org/2005/Atom";
    private static final String dcNS = "http://purl.org/dc/elements/1.1/";
    // DC field to use for feed title
    private static String titleSelector = null;
    // DC field to use for feed date
    private static String dateSelector = null;
    // DC field(s) to use in feed description
    private static List<String> descripSelectors = null;
    // Dc field to use for author (Atom format only)
    private static String authorSelector = null;
    // base search UI URL
    private static String uiUrl = null;
    // base search service URL
    private static String svcUrl = null;
    private static SimpleDateFormat RFC822DF = 
    	new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);
    
    static
    {
    	enabled = ConfigurationManager.getBooleanProperty("websvc.opensearch.enable");
        svcUrl = ConfigurationManager.getProperty("dspace.url") + "/" +
                 ConfigurationManager.getProperty("websvc.opensearch.svccontext");
        uiUrl = ConfigurationManager.getProperty("dspace.url") + "/" +
                ConfigurationManager.getProperty("websvc.opensearch.uicontext");

    	// read rest of config info if enabled
    	formats = new ArrayList<String>();
    	if (enabled)
    	{
    		String fmtsStr = ConfigurationManager.getProperty("websvc.opensearch.formats");
    		if ( fmtsStr != null )
    		{
    			for (String fmt : fmtsStr.split(","))
    			{
    				formats.add(fmt);
    			}
    		}
    	}
    	// setup field mappings
        titleSelector = ConfigurationManager.getProperty("webui.feed.item.title");
        if (titleSelector == null)
        {
            titleSelector = "dc.title";
        }
        dateSelector = ConfigurationManager.getProperty("webui.feed.item.date");
        if (dateSelector == null)
        {
            dateSelector = "dc.date.issued";
        }
        authorSelector = ConfigurationManager.getProperty("webui.feed.item.author");
        if (authorSelector == null)
        {
            authorSelector = "dc.contributor.author";
        }
        descripSelectors = new ArrayList<String>();
        String descFields = ConfigurationManager.getProperty("webui.feed.item.description");
        if (descFields == null)
        {     
        	descFields = "dc.title, dc.contributor.author, dc.contributor.editor, dc.description.abstract, dc.description";
        }
        for (String field : descFields.split(","))
        {
        	descripSelectors.add(field.trim());
        }
    }
    
    /**
     * Returns list of supported formats
     * 
     * @return list of format names - 'rss', 'atom' or 'html'
     */
    public static List<String> getFormats()
    {
    	return formats;
    }
    
    /**
     * Returns list of metadata selectors used to compose the description element
     * 
     * @return selector list - format 'schema.element[.qualifier]'
     */
    public static List<String> getDescriptionSelectors()
    {
    	return descripSelectors;
    }
    
    /**
     * Returns a mime-type associated with passed format
     * 
     * @param format the results document format (rss, atom, html)
     * @return content-type mime-type
     */
    public static String getContentType(String format)
    {
    	return "html".equals(format) ? "text/html" : 
    									"application/" + format + "+xml; charset=UTF-8";
    }
    
    /**
     * Returns the OpenSearch service document appropriate for given scope
     * 
     * @param scope - null for entire repository, or handle or community or collection
     * @return document the service document
     * @throws IOException
     */
    public static Document getDescriptionDoc(String scope) throws IOException
    {
    	return jDomToW3(getServiceDocument(scope));
    }
    
    /**
     * Returns OpenSearch Servic Document as a string
     * 
     * @param scope - null for entire repository, or handle or community or collection
     * @return service document as a string
     */
    public static String getDescription(String scope)
    {
    	return new XMLOutputter().outputString(getServiceDocument(scope));
    }
    
    /**
     * Returns a formatted set of search results as a string
     * 
     * @param format results format - html, rss or atom
     * @param query - the search query
     * @param qResults - the query results to be formatted
     * @param scope - search scope, null or community/collection handle
     * @param results the retreived DSpace objects satisfying search
     * @param labels labels to apply - format specific
     * @return formatted search results
     * @throws IOException
     */
    public static String getResultsString(String format, String query, QueryResults qResults,
    		                          	  DSpaceObject scope, DSpaceObject[] results,
    		                          	  Map<String, String> labels) throws IOException
    {
        // Encode results in requested format
        if ("rss".equals(format))
        {
        	return new XMLOutputter().outputString(rssFeed(query, qResults, scope, results, labels));
        }
        else if ("atom".equals(format))
        {
        	WireFeed feed = generateFeed(query, qResults, scope, results, labels);
            if (feed == null)
            {
            	throw new IOException("No feed");
            }
            try
            {
            	return new WireFeedOutput().outputString(feed);
            }
            catch(Exception exp)
            {
            	throw new IOException("Unable to generate feed");
            }
        }
        else
        {
        	throw new IOException("Invalid format: " + format);
        }
    }
    
    /**
     * Returns a formatted set of search results as a document
     * 
     * @param format results format - html, rss or atom
     * @param query - the search query
     * @param qResults - the query results to be formatted
     * @param scope - search scope, null or community/collection handle
     * @param results the retreived DSpace objects satisfying search
     * @param labels labels to apply - format specific
     * @return formatted search results
     * @throws IOException
     */
    public static Document getResultsDoc(String format, String query, QueryResults qResults,
    		                          DSpaceObject scope, DSpaceObject[] results, Map<String, String> labels)
                                      throws IOException
    {
        // Encode results in requested format
        if ("rss".equals(format))
        {
        	return jDomToW3(rssFeed(query, qResults, scope, results, labels));
        }
        else if ("atom".equals(format))
        {
        	WireFeed feed = generateFeed(query, qResults, scope, results, labels);
        	if (feed == null)
        	{
        		throw new IOException("No feed");
        	}
        	try
        	{
        		return new WireFeedOutput().outputW3CDom(feed);
        	}
        	catch(Exception exp)
        	{
        		throw new IOException("Unable to output feed");
        	}
        }
        else
        {
        	throw new IOException("Invalid format: " + format);
        }
    }
    
    /**
     * Generates an Atom feed from search results
     * 
     * @param query - the search query
     * @param qResults - the query results to be formatted
     * @param scope - search scope, null or community/collection handle
     * @param results the retreived DSpace objects satisfying search
     * @param labels labels to apply - format specific
     * @return Atom-formatted search results
     */
    private static WireFeed generateFeed(String query, QueryResults qResults, DSpaceObject scope,
    		                             DSpaceObject[] results, Map<String, String> labels)
    {
    	WireFeed feed = atomFeed(query, qResults, scope, results, labels);
  
        if (feed != null)
        {
        	// inject opensearch markup
        	OpenSearchModule osMod = openSearchMarkup(query, qResults);
        	List modules = feed.getModules();
        	modules.add(osMod);
        	feed.setModules(modules);
        }
    	return feed;
    }
    
    
    /**
     * Generates an RSS 2.0 feed from search results
     * 
     * @param query - the search query
     * @param qResults - the query results to be formatted
     * @param scope - search scope, null or community/collection handle
     * @param results the retreived DSpace objects satisfying search
     * @param labels labels to apply - format specific
     * @return Atom-formatted search results
     */
    private static org.jdom.Document rssFeed(String query, QueryResults qResults, DSpaceObject scope,
            							DSpaceObject[] results, Map<String, String> labels)
    {
    	Namespace atomNs = Namespace.getNamespace("atom", atNs);
    	Namespace openNs = Namespace.getNamespace("opensearch", osNs);
    	Element rss = new Element("rss").setAttribute("version", "2.0");
    	rss.addNamespaceDeclaration(atomNs);
    	rss.addNamespaceDeclaration(openNs);
    	Element chan = new Element("channel");
    	chan.addContent(new Element("title").setText(getDSOTitle(scope, labels)));
    	chan.addContent(new Element("link").setText(resolveUrl(scope)));
    	chan.addContent(new Element("description").setText(getDSODescription(scope, labels, true)));
    	// NB: this element in a different namespace
    	Element linkEl = new Element("link", atomNs);
    	linkEl.setAttribute("rel", "search");
    	linkEl.setAttribute("type", "application/opensearchdescription+xml");
    	linkEl.setText(svcUrl + "description.xml");
    	chan.addContent(linkEl);
    	// OpenSearch elements
    	chan.addContent(new Element("totalResults", openNs).setText(String.valueOf(qResults.getHitCount())));
    	chan.addContent(new Element("startIndex", openNs).setText(String.valueOf(qResults.getStart())));
    	chan.addContent(new Element("resultsPerPage", openNs).setText(String.valueOf(qResults.getPageSize())));
    	Element qr = new Element("Query", openNs);
    	qr.setAttribute("role", "request");
    	qr.setAttribute("searchTerms", query);
    	int sp = 1 + (qResults.getStart() / qResults.getPageSize());
    	qr.setAttribute("startPage", String.valueOf(sp));
    	chan.addContent(qr);
    	
    	// if collection or community has a logo, toss it in
    	if (scope != null)
    	{
    		Bitstream logo = getDSOLogo(scope);
    		if (logo != null)
    		{
    			Element image = new Element("image");
    			image.addContent(new Element("title").setText(labels.get("logo.title")));
    			image.addContent(new Element("link").setText(resolveUrl(scope)));
    			image.addContent(new Element("url").setText(getLogoUrl(logo, labels.get("uitype"))));
    			chan.addContent(image);
    		}
    	}

    	for (DSpaceObject result : results)
    	{
    		chan.addContent(rssItemFromResult(result, labels));
    	}
    	rss.addContent(chan);
    	return new org.jdom.Document(rss);
    }
    
    /**
     * Maps DSpace Object fields to RSS item fields. Returns completed RSS Item.
     * Assigned fields: title, link, description and pubdate if available
     * 
     * @param result	DSpace data object
     * @param labels 	i18n localization tags
     * @return an object representing a feed entry
     */
    private static Element rssItemFromResult(DSpaceObject result, Map<String, String> labels)
    {
       	Element item = new Element("item");
       	item.addContent(new Element("title").setText(getDSOTitle(result, labels)));
       	item.addContent(new Element("link").setText(resolveUrl(result)));
    	item.addContent(new Element("description").setText(getDSODescription(result, labels, true)));
        // set date field if applicable and available
        if (result.getType() == Constants.ITEM)
        {
        	try
        	{
        		String dcDate = ((Item)result).getMetadata(dateSelector)[0].value;
        		String fmtDate = RFC822DF.format(new DCDate(dcDate).toDate());
        		item.addContent(new Element("pubDate").setText(fmtDate));       
        	}
        	catch (ArrayIndexOutOfBoundsException e)
        	{
        		// ignore - optional field
        	}
        }
		return item;
    }
    
    /**
     * Create a WireFeed object for search results
     * 
     * @param query - the search query
     * @param qRes - the query results to be formatted
     * @param container - search scope, null or community/collection handle
     * @param results the retreived DSpace objects satisfying search
     * @param labels labels to apply - format specific
     * @return Atom 1.0 formatted WireFeed
     */
    private static WireFeed atomFeed(String query, QueryResults qRes, DSpaceObject container,
    		                  DSpaceObject[] results, Map<String, String> labels)
	{
    	Feed feed = new Feed();
    	feed.setFeedType("atom_1.0");
    	feed.setTitle(getDSOTitle(container, labels));
    	List<Link> otherLinks = new ArrayList<Link>();
    	addLink(otherLinks, null, null, resolveUrl(container));
    	feed.setOtherLinks(otherLinks);
    	List<Person> auths = new ArrayList<Person>();
    	Person author = new Person();
    	author.setName(ConfigurationManager.getProperty("dspace.name"));
    	auths.add(author);
    	feed.setAuthors(auths);
    	feed.setId("urn:uuid:" + UUID.randomUUID().toString());
    	feed.setUpdated(new Date());
		// if collection or community has a logo, toss it in
    	if (container != null)
    	{
    		Bitstream logo = getDSOLogo(container);
    		if (logo != null)
    		{
    			feed.setLogo(getLogoUrl(logo, labels.get("uitype")));
    		}
    	}
    	// make useful links
    	int hits = qRes.getHitCount();
    	int start = qRes.getStart();
    	int pageSize = qRes.getPageSize();
    	List<Link> altLinks = new ArrayList<Link>();
    	addLink(altLinks, "alternate", "text/html", uiUrl + queryString(query, start, false));
    	addLink(altLinks, "self", "application/atom+xml", svcUrl + queryString(query, start, true)); 
    	addLink(altLinks, "first", "application/atom+xml", svcUrl + queryString(query, 0, true));
    	int next = Math.min(start + pageSize, hits - pageSize);
    	addLink(altLinks, "next", "application/atom+xml", svcUrl + queryString(query, next, true));
    	int prev = Math.max(0, start - pageSize);
    	addLink(altLinks, "previous", "application/atom+xml", svcUrl + queryString(query, prev, true));
    	addLink(altLinks, "last", "application/atom+xml", svcUrl + queryString(query, hits - pageSize, true));
    	addLink(altLinks, "search", "application/opensearchdescription+xml", svcUrl + "description.xml");    	
    	feed.setAlternateLinks(altLinks);

    	List<Entry> atomEntries = new ArrayList<Entry>();
    	for (DSpaceObject result : results)
    	{
    		atomEntries.add(entryFromResult(result, labels));
    	}
    	feed.setEntries(atomEntries);
    	return feed;
	}
    
    /**
     * Maps DSpace Item fields to Atom Entry fields. Returns completed Atom Entry.
     * Assigned fields: title, link, description and pub date if available
     * 
     * @param url        DSpace object URL
     * @param result	 DSpace data object
     * @return an object representing an Atom feed entry
     */
    private static Entry entryFromResult(DSpaceObject result, Map<String, String> labels)
    {
        Entry entry = new Entry();
        
        entry.setTitle(getDSOTitle(result, labels));
        
        List<Link> links = new ArrayList<Link>();
        addLink(links, null, null, resolveUrl(result));
        entry.setOtherLinks(links);
        
        entry.setId("urn:uuid:" + UUID.randomUUID().toString());       
        entry.setUpdated(new Date());
        
        // populate with additional metadata
        // important caveat - this will only use DC metadata
        // it is not configurable for other schema (yet)
        if (titleSelector.startsWith("dc"))
        {
        	Item item = (Item)result;
        	// authors
        	List<Person> authors = new ArrayList<Person>();
        	DCValue[] dcAuths = item.getMetadata(authorSelector);
        	for (DCValue dcAuth : dcAuths)
        	{
        		Person auth = new Person();
        		auth.setName(dcAuth.value);
        		authors.add(auth);
        	}
        	if (authors.size() > 0)
        	{
        		entry.setAuthors(authors);
        	}
        	// dc type & subjects (map to Atom category)
        	List<Category> categories = new ArrayList<Category>();
        	DCValue[] dcTypes = item.getDC("type", Item.ANY, Item.ANY);
        	for (DCValue dcType : dcTypes)
        	{
            	Category cat = new Category();
            	cat.setTerm(dcType.value);
            	cat.setScheme(dcNS + "type");
            	cat.setLabel("type");
            	categories.add(cat);
        	}
        	DCValue[] dcSubjects = item.getDC("subject", Item.ANY, Item.ANY);
        	for (DCValue dcSubj : dcSubjects)
        	{
            	Category cat = new Category();
            	cat.setTerm(dcSubj.value);
            	cat.setScheme(dcNS + "subject");
            	String label = dcSubj.qualifier;
            	if (label == null)
            	{
            		label = "subject";
            	}
            	cat.setLabel(label);
            	categories.add(cat);
        	}
        	if (categories.size() > 0)
        	{
        		entry.setCategories(categories);
        	}
        	// rights
        	DCValue[] dcRights = item.getDC("rights", Item.ANY, Item.ANY);
        	StringBuffer rightsSB = new StringBuffer();
        	for (DCValue dcRight : dcRights)
        	{
        		rightsSB.append(dcRight.value);
                rightsSB.append(";");
        	}
        	String rights = rightsSB.toString();
        	if (rights.length() > 0)
        	{
        		entry.setRights(rights);
        	}
        }
        
        Content content = new Content();
        content.setType(Content.TEXT);
        content.setValue(getDSODescription(result, labels, false));
        entry.setSummary(content);          
        
        // set date field if applicable and available
        if (result.getType() == Constants.ITEM)
        {
        	try
        	{
        		String dcDate = ((Item)result).getMetadata(dateSelector)[0].value;
        		entry.setPublished((new DCDate(dcDate)).toDate());          
        	}
        	catch (ArrayIndexOutOfBoundsException e)
        	{
        		// ignore - optional field
        	}
        }      
        return entry;
    }
    
    // compose query string
    private static String queryString(String query, int start, boolean atom)
    {
    	StringBuffer sb = new StringBuffer();
    	sb.append("?query=");
    	sb.append(query);
    	sb.append("&start=");
    	sb.append(start);
    	if (atom)
    	{
    		sb.append("&format=atom");
    	}
    	return sb.toString();
    }
    
    // create a link
    private static void addLink(List<Link> list, String rel, String type, String href)
    {
    	Link link = new Link();
    	link.setRel(rel);
    	link.setType(type);
    	link.setHref(href);
    	list.add(link);
    }
    
    /**
     * Composes a logo URL in a platform-specifc manner
     * 
     * @param logo bitstream
     * @param uiType xmlui or jspui
     * @return Logo URL as a string
     */
    private static String getLogoUrl(Bitstream logo, String uiType)
    {
		String url = ConfigurationManager.getProperty("dspace.url");
		if ("xmlui".equals(uiType))
		{
			url += "/bitstream/id/" + logo.getID() + "/?sequence=-1";
		}
		else
		{
			url += "/retreive/" + logo.getID();
		}
		return url;
    }
    
    /*
     * Generates the OpenSearch elements which are added to the RSS or Atom feeds as foreign markup
     * wrapped in a module
     * 
     * @param query the search query
     * @param qRes the search results
     * @return module
     */
    private static OpenSearchModule openSearchMarkup(String query, QueryResults qRes)
    { 
    	OpenSearchModule osMod = new OpenSearchModuleImpl();
    	osMod.setTotalResults(qRes.getHitCount());
    	osMod.setStartIndex(qRes.getStart());
    	osMod.setItemsPerPage(qRes.getPageSize());
    	OSQuery osq = new OSQuery();
    	osq.setRole("request");
    	osq.setSearchTerms(query);
    	osq.setStartPage(1 + (qRes.getStart() / qRes.getPageSize()));
    	osMod.addQuery(osq);
    	return osMod;
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
    private static String resolveUrl(DSpaceObject dso)
    {
    	if (dso == null)
    	{
    		return ConfigurationManager.getProperty("dspace.url");
    	}
    	
		if (ConfigurationManager.getBooleanProperty("webui.feed.localresolve"))
		{	
			return ConfigurationManager.getProperty("dspace.url") + "/handle/" + dso.getHandle();
		}
		else
		{
			return HandleManager.getCanonicalForm(dso.getHandle()); 
		}
    }
    
    /**
     * Obtains logo for a collection or community
     * @param dso the DSpace Object
     * @return log bitstream
     */
    private static Bitstream getDSOLogo(DSpaceObject dso)
    {
    	Bitstream logo = null;
        if (dso.getType() == Constants.COLLECTION)
    	{
    		logo = ((Collection)dso).getLogo();
    	}
    	else if (dso.getType() == Constants.COMMUNITY)
    	{
    		logo = ((Community)dso).getLogo();
    	}   	
    	return logo;
    }
    
    /**
     * Returns a title appropriate to the type of Object
     * @param dso DSpace Object
     * @param labels
     * @return Name for the object
     */
    private static String getDSOTitle(DSpaceObject dso, Map<String, String> labels)
    {
        String title = null;
        try
        {
        	if (dso == null)
        	{
        		title = ConfigurationManager.getProperty("dspace.name");
        	}
        	else if (dso.getType() == Constants.ITEM)
        	{
        		title = ((Item)dso).getMetadata(titleSelector)[0].value;
        	}
        	else if (dso.getType() == Constants.COLLECTION)
        	{
        		title = ((Collection)dso).getMetadata("name");
        	}
        	else if (dso.getType() == Constants.COMMUNITY)
        	{
        		title = ((Community)dso).getMetadata("name");
        	}
        }
        catch (ArrayIndexOutOfBoundsException e)
        { 
            title = labels.get("notitle");
        }
        return title;
    }
    
    /**
     * Returns a description appropriate to Object-type
     * @param dso DSpace Object
     * @param labels
     * @param markup
     * @return description of object
     */
    private static String getDSODescription(DSpaceObject dso, Map<String, String> labels, boolean markup)
    {
        String description = null;
        if (dso == null)
        {
        	description = labels.get("general-feed.description");
        }
        else if (dso.getType() == Constants.ITEM)
        {
        	description = getDescription((Item)dso, labels, markup);
        }
        else if (dso.getType() == Constants.COLLECTION)
        {
        	description = ((Collection)dso).getMetadata("short_description");
        }
        else if (dso.getType() == Constants.COMMUNITY)
        {
        	description = ((Community)dso).getMetadata("short_description");
        }
        return description;
    }
    
    /**
     * Returns a description consisting of a concatenation of selected metadata fields.
     * @param item the DSpace Item
     * @return a description string
     */
    private static String getDescription(Item item, Map<String, String> labels, boolean markup)
    {
        //loop through all the metadata fields to put in the description
        StringBuffer descBuf = new StringBuffer();  
        for (String selector : descripSelectors)
        {
            boolean isDate = false;         
            // Find out if the field should rendered as a date
            if (selector.indexOf("(date)") > 0)
            {
                selector = selector.replaceAll("\\(date\\)", "");
                isDate = true;
            }
           
            //print out this field, along with its value(s)
            DCValue[] values = item.getMetadata(selector);        
            if(values != null && values.length>0)
            {
                //as long as there is already something in the description
                //buffer, print out a few line breaks before the next field
                if(markup && descBuf.length() > 0)
                {
                    descBuf.append("\n<br/>");
                    descBuf.append("\n<br/>");
                }
                    
                String fieldLabel = labels.get("metadata." + selector);
                if(fieldLabel !=null && fieldLabel.length()>0)
                {
                    descBuf.append(fieldLabel + ": ");
                }
                
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
        }
        return descBuf.toString();
    }
    
    /**
     * Returns as a document the OpenSearch service document
     * 
     * @param scope - null for the entire repository, or a collection/community handle
     * @return Service Document
     */
    private static org.jdom.Document getServiceDocument(String scope)
    {
    	Namespace ns = Namespace.getNamespace(osNs);
        Element root = new Element("OpenSearchDescription", ns);
        root.addContent(new Element("ShortName", ns).setText(ConfigurationManager.getProperty("websvc.opensearch.shortname")));
        root.addContent(new Element("LongName", ns).setText(ConfigurationManager.getProperty("websvc.opensearch.longname")));
        root.addContent(new Element("Description", ns).setText(ConfigurationManager.getProperty("websvc.opensearch.description")));
        root.addContent(new Element("InputEncoding", ns).setText("UTF-8"));
        root.addContent(new Element("OutputEncoding", ns).setText("UTF-8"));
        // optional elements
        String sample = ConfigurationManager.getProperty("websvc.opensearch.samplequery");
        if (sample != null && sample.length() > 0)
        {
        	Element sq = new Element("Query", ns).setAttribute("role", "example");
        	root.addContent(sq.setAttribute("searchTerms", sample));
        }
        String tags = ConfigurationManager.getProperty("websvc.opensearch.tags");
        if (tags != null && tags.length() > 0)
        {
        	root.addContent(new Element("Tags", ns).setText(tags));
        }
        String contact = ConfigurationManager.getProperty("mail.admin");
        if (contact != null && contact.length() > 0)
        {
        	root.addContent(new Element("Contact", ns).setText(contact));
        }
        String faviconUrl = ConfigurationManager.getProperty("websvc.opensearch.faviconurl");
        if (faviconUrl != null && faviconUrl.length() > 0)
        {
        	String dim = String.valueOf(16);
        	String type = faviconUrl.endsWith("ico") ? "image/vnd.microsoft.icon" : "image/png";
        	Element fav = new Element("Image", ns).setAttribute("height", dim).setAttribute("width", dim).
        	                setAttribute("type", type).setText(faviconUrl);
        	root.addContent(fav);
        }
        // service URLs
        for (String format : formats)
        {
        	Element url = new Element("Url", ns).setAttribute("type", getContentType(format));
        	StringBuffer template = new StringBuffer();
        	if ("html".equals(format))
        	{
        		template.append(uiUrl);
        	}
        	else
        	{
        		template.append(svcUrl);
        	}
            template.append("?query={searchTerms}");
        	if(! "html".equals(format))
        	{
               	template.append("&start={startIndex?}&rpp={count?}&format=");
               	template.append(format);
        	}
        	if (scope != null)
        	{
        		template.append("&scope=");
        		template.append(scope);
        	}
        	url.setAttribute("template", template.toString());
        	root.addContent(url);
        }
        return new org.jdom.Document(root);
    }
    
    /**
     * Converts a JDOM document to a W3C one
     * @param jdomDoc
     * @return W3C Document object
     * @throws IOException
     */
    private static Document jDomToW3(org.jdom.Document jdomDoc) throws IOException
    {
        DOMOutputter domOut = new DOMOutputter();
        try
        {
        	return domOut.output(jdomDoc);
        }
        catch(JDOMException jde)
        {
        	throw new IOException("JDOM output exception");
        }
    }
}
