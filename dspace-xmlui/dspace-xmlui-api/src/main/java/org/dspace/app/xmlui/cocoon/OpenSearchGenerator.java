/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.apache.cocoon.util.HashUtil;

import org.apache.cocoon.xml.dom.DOMStreamer;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.ExpiresValidity;

import org.dspace.app.util.OpenSearch;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.FeedUtils;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;
import org.dspace.sort.SortOption;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * Generate an OpenSearch-compliant search results document for DSpace, either
 * a community or collection or the whole repository.
 *
 * Once thing that has been modified from DSpace's JSP implementation is what
 * is placed inside an item's description, we've changed it so that the list
 * of metadata fields is scanned until a value is found and the first one
 * found is used as the description. This means that we look at the abstract,
 * description, alternative title, title, etc... to and the first metadata
 * value found is used.
 *
 * I18N: Feed's are internationalized, meaning that they may contain references
 * to messages contained in the global messages.xml file using cocoon's i18n
 * schema. However the library used to build the feeds does not understand
 * this schema to work around this limitation I created a little hack. It
 * basically works like this, when text that needs to be localized is put into
 * the feed it is always mangled such that a prefix is added to the messages's
 * key. Thus if the key were "xmlui.feed.text" then the resulting text placed
 * into the feed would be "I18N:xmlui.feed.text". After the library is finished
 * and produced it's final result the output is traversed to find these
 * occurrences and replace them with proper cocoon i18n elements.
 *
 * @author Richard Rodgers
 */

public class OpenSearchGenerator extends AbstractGenerator
                implements CacheableProcessingComponent, Recyclable
{
    /** Cache of this object's validity */
    private ExpiresValidity validity = null;
    
        /** The results requested format */
        private String format = null;
    /** the search query string */
    private String query = null;
    /** optional search scope (= handle of container) or null */
    private String scope = null;
    /** optional sort specification */
    private int sort = 0;
    /** sort order, see SortOption **/
    private String sortOrder = null;
    /** results per page */
    private int rpp = 0;
    /** first result index */
    private int start = 0;
    /** request type */
    private String type = null;
    
    /** the results document (cached) */
    private Document resultsDoc = null;
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        StringBuffer key = new StringBuffer("key:");
        if (scope != null)
        {
            key.append(scope);
        }
        key.append(query);
        if (format != null)
        {
            key.append(format);
        }
        key.append(sort);
        key.append(start);
        key.append(rpp);
        key.append(sortOrder);
        return HashUtil.hash(key.toString());
    }

    /**
     * Generate the cache validity object.
     *
     */
    public SourceValidity getValidity()
    {
        if (this.validity == null)
        {
                long expiry = System.currentTimeMillis() +
                    ConfigurationManager.getLongProperty("websvc.opensearch.validity") * 60 * 60 * 1000;
                this.validity = new ExpiresValidity(expiry);
        }
        return this.validity;
    }
      
    /**
     * Setup configuration for this request
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);
        
        Request request = ObjectModelHelper.getRequest(objectModel);
        this.query = request.getParameter("query");
        if (query == null)
        {
            query = "";
        }
        query = URLDecoder.decode(query, "UTF-8");
        this.format = request.getParameter("format");
        if (format == null || format.length() == 0)
        {
            format = "atom";
        }
        this.scope = request.getParameter("scope");
        String srt = request.getParameter("sort_by");
        this.sort = (srt == null || srt.length() == 0) ? 0 : Integer.valueOf(srt);
        String order = request.getParameter("order");
        this.sortOrder = (order == null || order.length() == 0 || order.toLowerCase().startsWith("asc")) ?
                         SortOption.ASCENDING : SortOption.DESCENDING;
        String st = request.getParameter("start");
        this.start = (st == null || st.length() == 0) ? 0 : Integer.valueOf(st);
        String pp = request.getParameter("rpp");
        this.rpp = (pp == null || pp.length() == 0) ? 0 : Integer.valueOf(pp);
        try
        {
                this.type = par.getParameter("type");
        }
        catch(ParameterException e)
        {
                this.type = "results";
        }
    }
    
    
    /**
     * Generate the search results document.
     */
    public void generate() throws IOException, SAXException, ProcessingException
    {
        Document retDoc = null;
        try
        {
                if (type != null && type.equals("description"))
                {
                        retDoc = OpenSearch.getDescriptionDoc(scope);
                }
                else if (resultsDoc != null)
                {
                        // use cached document if available
                        retDoc = resultsDoc;
                }
                else
                {
                        Context context = ContextUtil.obtainContext(objectModel);
                        QueryArgs qArgs = new QueryArgs();

                // can't start earlier than 0 in the results!
                if (start < 0)
                {
                    start = 0;
                }
                qArgs.setStart(start);
                
                if (rpp > 0)
                {
                    qArgs.setPageSize(rpp);
                }
                        qArgs.setSortOrder(sortOrder);
                
                if (sort > 0)
                {
                        try
                        {
                                qArgs.setSortOption(SortOption.getSortOption(sort));
                        }
                        catch(Exception e)
                        {
                                // invalid sort id - do nothing
                        }
                }
                qArgs.setSortOrder(sortOrder);

                // If there is a scope parameter, attempt to dereference it
                // failure will only result in its being ignored
                DSpaceObject container = null;
                if ((scope != null) && !scope.equals(""))
                {
                        container = HandleManager.resolveToObject(context, scope);
                }

                qArgs.setQuery(query);

                // Perform the search
                QueryResults qResults = null;
                if (container == null)
                {
                        qResults = DSQuery.doQuery(context, qArgs);
                }
                else if (container instanceof Collection)
                {
                    qResults = DSQuery.doQuery(context, qArgs, (Collection)container);
                }
                else if (container instanceof Community)
                {
                    qResults = DSQuery.doQuery(context, qArgs, (Community)container);
                }
                else
                {
                    throw new IllegalStateException("Invalid container for search context");
                }
                
                // now instantiate the results
                DSpaceObject[] results = new DSpaceObject[qResults.getHitHandles().size()];
                for (int i = 0; i < qResults.getHitHandles().size(); i++)
                {
                    String myHandle = qResults.getHitHandles().get(i);
                    DSpaceObject dso = HandleManager.resolveToObject(context, myHandle);
                    if (dso == null)
                    {
                        throw new SQLException("Query \"" + query
                                + "\" returned unresolvable handle: " + myHandle);
                    }
                    results[i] = dso;
                }
                        resultsDoc = OpenSearch.getResultsDoc(format, query, qResults,
                                                                      container, results, FeedUtils.i18nLabels);
                        FeedUtils.unmangleI18N(resultsDoc);
                        retDoc = resultsDoc;
                }
               
                if (retDoc != null)
                {
                        DOMStreamer streamer = new DOMStreamer(contentHandler, lexicalHandler);
                        streamer.stream(retDoc);
                }
                }
                catch (IOException e)
        {
            throw new SAXException(e);
        }
                catch (SQLException sqle)
                {
                        throw new SAXException(sqle);
                }
    }
    
    /**
     * Recycle
     */
    
    public void recycle()
    {
        this.format = null;
        this.query = null;
        this.scope = null;
        this.sort = 0;
        this.rpp = 0;
        this.start = 0;
        this.type = null;
        this.sortOrder = null;
        this.resultsDoc = null;
        this.validity = null;
        super.recycle();
    }
}
