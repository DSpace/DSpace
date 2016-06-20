/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.dspace.app.util.service.OpenSearchService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.DOMOutputter;
import org.jdom.output.XMLOutputter;

import org.apache.log4j.Logger;

import org.dspace.content.DSpaceObject;

import com.sun.syndication.feed.module.opensearch.OpenSearchModule;
import com.sun.syndication.feed.module.opensearch.entity.OSQuery;
import com.sun.syndication.feed.module.opensearch.impl.OpenSearchModuleImpl;
import com.sun.syndication.io.FeedException;
import java.util.Arrays;
import org.dspace.services.ConfigurationService;

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
public class OpenSearchServiceImpl implements OpenSearchService, InitializingBean
{
    private static final Logger log = Logger.getLogger(OpenSearchServiceImpl.class);

    // are open search queries enabled?
    protected boolean enabled = false;
    // supported results formats
    protected List<String> formats = null;
    // Namespaces used
    protected final String osNs = "http://a9.com/-/spec/opensearch/1.1/";

    // base search UI URL
    protected String uiUrl = null;
    // base search service URL
    protected String svcUrl = null;

    @Autowired(required = true)
    protected HandleService handleService;

    protected OpenSearchServiceImpl() {
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
    	enabled = config.getBooleanProperty("websvc.opensearch.enable");
        svcUrl = config.getProperty("dspace.url") + "/" +
                 config.getProperty("websvc.opensearch.svccontext");
        uiUrl = config.getProperty("dspace.url") + "/" +
                config.getProperty("websvc.opensearch.uicontext");

    	// read rest of config info if enabled
    	formats = new ArrayList<String>();
    	if (enabled)
    	{
    		String[] fmts = config.getArrayProperty("websvc.opensearch.formats");
            formats = Arrays.asList(fmts);
    	}
    }

    @Override
    public List<String> getFormats()
    {
    	return formats;
    }
    
    @Override
    public String getContentType(String format)
    {
    	return "html".equals(format) ? "text/html" : 
    									"application/" + format + "+xml; charset=UTF-8";
    }
    
    @Override
    public Document getDescriptionDoc(String scope) throws IOException
    {
    	return jDomToW3(getServiceDocument(scope));
    }
    
    @Override
    public String getDescription(String scope)
    {
    	return new XMLOutputter().outputString(getServiceDocument(scope));
    }

    @Override
    public String getResultsString(Context context, String format, String query, int totalResults, int start, int pageSize,
    		                          	  DSpaceObject scope, List<DSpaceObject> results,
    		                          	  Map<String, String> labels) throws IOException
    {
        try
        {
            return getResults(context, format, query, totalResults, start, pageSize, scope, results, labels).outputString();
        }
        catch (FeedException e)
        {
            log.error(e.toString(), e);
            	throw new IOException("Unable to generate feed", e);
        }
    }

    @Override
    public Document getResultsDoc(Context context, String format, String query, int totalResults, int start, int pageSize,
    		                          DSpaceObject scope, List<DSpaceObject> results, Map<String, String> labels)
                                      throws IOException
    {
        try
        {
            return getResults(context, format, query, totalResults, start, pageSize, scope, results, labels).outputW3CDom();
        }
        catch (FeedException e)
        {
            log.error(e.toString(), e);
            throw new IOException("Unable to generate feed", e);
        }
    }

    protected SyndicationFeed getResults(Context context, String format, String query, int totalResults, int start, int pageSize,
                                          DSpaceObject scope, List<DSpaceObject> results, Map<String, String> labels)
    {
        // Encode results in requested format
        if ("rss".equals(format))
        {
            format = "rss_2.0";
        }
        else if ("atom".equals(format))
        {
            format = "atom_1.0";
        }
    	
        SyndicationFeed feed = new SyndicationFeed(labels.get(SyndicationFeed.MSG_UITYPE));
        feed.populate(null, context, scope, results, labels);
        feed.setType(format);
        feed.addModule(openSearchMarkup(query, totalResults, start, pageSize));
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
    protected OpenSearchModule openSearchMarkup(String query, int totalResults, int start, int pageSize)
    {
    	OpenSearchModule osMod = new OpenSearchModuleImpl();
    	osMod.setTotalResults(totalResults);
    	osMod.setStartIndex(start);
    	osMod.setItemsPerPage(pageSize);
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
        osq.setStartPage(1 + (start / pageSize));
        osMod.addQuery(osq);
        return osMod;
    }
    
    /**
     * Returns as a document the OpenSearch service document
     * 
     * @param scope - null for the entire repository, or a collection/community handle
     * @return Service Document
     */
    protected org.jdom.Document getServiceDocument(String scope)
    {
        ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();

    	Namespace ns = Namespace.getNamespace(osNs);
        Element root = new Element("OpenSearchDescription", ns);
        root.addContent(new Element("ShortName", ns).setText(config.getProperty("websvc.opensearch.shortname")));
        root.addContent(new Element("LongName", ns).setText(config.getProperty("websvc.opensearch.longname")));
        root.addContent(new Element("Description", ns).setText(config.getProperty("websvc.opensearch.description")));
        root.addContent(new Element("InputEncoding", ns).setText("UTF-8"));
        root.addContent(new Element("OutputEncoding", ns).setText("UTF-8"));
        // optional elements
        String sample = config.getProperty("websvc.opensearch.samplequery");
        if (sample != null && sample.length() > 0)
        {
        	Element sq = new Element("Query", ns).setAttribute("role", "example");
        	root.addContent(sq.setAttribute("searchTerms", sample));
        }
        String tags = config.getProperty("websvc.opensearch.tags");
        if (tags != null && tags.length() > 0)
        {
        	root.addContent(new Element("Tags", ns).setText(tags));
        }
        String contact = config.getProperty("mail.admin");
        if (contact != null && contact.length() > 0)
        {
        	root.addContent(new Element("Contact", ns).setText(contact));
        }
        String faviconUrl = config.getProperty("websvc.opensearch.faviconurl");
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
        	StringBuilder template = new StringBuilder();
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
     * @throws IOException if IO error
     */
    protected Document jDomToW3(org.jdom.Document jdomDoc) throws IOException
    {
        DOMOutputter domOut = new DOMOutputter();
        try
        {
        	return domOut.output(jdomDoc);
        }
        catch(JDOMException jde)
        {
        	throw new IOException("JDOM output exception", jde);
        }
    }

    @Override
    public DSpaceObject resolveScope(Context context, String scope) throws SQLException
    {
        if(scope == null || "".equals(scope))
            return null;

        DSpaceObject dso = handleService.resolveToObject(context, scope);
        if(dso == null || dso.getType() == Constants.ITEM)
            throw new IllegalArgumentException("Scope handle "+scope+" should point to a valid Community or Collection");
        return dso;
    }

}
