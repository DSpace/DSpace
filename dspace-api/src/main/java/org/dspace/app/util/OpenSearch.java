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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

import org.w3c.dom.Document;

import org.jdom.DocType;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.DOMOutputter;
import org.jdom.output.XMLOutputter;

import org.apache.log4j.Logger;

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
import com.sun.syndication.io.FeedException;

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
    private static final Logger log = Logger.getLogger(OpenSearch.class);

    // are open search queries enabled?
    private static boolean enabled = false;
    // supported results formats
    private static List<String> formats = null;
    // Namespaces used
    private static final String osNs = "http://a9.com/-/spec/opensearch/1.1/";

    // base search UI URL
    private static String uiUrl = null;
    // base search service URL
    private static String svcUrl = null;
    
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
            try
            {
            return getResults(format, query, qResults, scope, results, labels).outputString();
            }
        catch (FeedException e)
            {
            log.error(e.toString(), e);
            	throw new IOException("Unable to generate feed");
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
        	try
        	{
            return getResults(format, query, qResults, scope, results, labels).outputW3CDom();
        	}
        catch (FeedException e)
        	{
            log.error(e.toString(), e);
            throw new IOException("Unable to generate feed");
        	}
    
        }
    
    private static SyndicationFeed getResults(String format, String query, QueryResults qResults,
                                          DSpaceObject scope, DSpaceObject[] results, Map<String, String> labels)
    {
        // Encode results in requested format
        if ("rss".equals(format))
            format = "rss_2.0";
        else if ("atom".equals(format))
            format = "atom_1.0";
    	
        SyndicationFeed feed = new SyndicationFeed(labels.get(SyndicationFeed.MSG_UITYPE));
        feed.populate(null, scope, results, labels);
        feed.setType(format);
        feed.addModule(openSearchMarkup(query, qResults));
    	return feed;
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
        try
        {
            osq.setSearchTerms(URLEncoder.encode(query, "UTF-8"));
        	}
        catch(UnsupportedEncodingException e)
        	{
            log.error(e);
        	}
        osq.setStartPage(1 + (qRes.getStart() / qRes.getPageSize()));
        osMod.addQuery(osq);
        return osMod;
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
