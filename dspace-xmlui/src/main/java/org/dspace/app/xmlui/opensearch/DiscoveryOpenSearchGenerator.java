/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.opensearch;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.avalon.excalibur.pool.*;
import org.apache.avalon.framework.parameters.*;
import org.apache.cocoon.*;
import org.apache.cocoon.caching.*;
import org.apache.cocoon.environment.*;
import org.apache.cocoon.xml.dom.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.*;
import org.dspace.sort.*;
import org.xml.sax.*;


/**
 * Generate an OpenSearch compliant search results document for DSpace, either scoped by a collection,
 * a community or the whole repository.
 *
 * This class implements the generate() method in order to issue a search using the Discovery search service
 * (Solr based search)
 * Search params are parsed by AbstractOpenSearchGenerator class.

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
 * @author Nestor Oviedo
 */
public class DiscoveryOpenSearchGenerator extends AbstractOpenSearchGenerator
					implements CacheableProcessingComponent, Recyclable
{

    /** the  search service to use */
    private SearchService searchService = null;

    /**
     * Setup the Discovery search service. Other paramas are setup in superclass's methods
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, par);
        
        searchService = SearchUtils.getSearchService();
        if(searchService == null)
            throw new IllegalStateException("Couldn't get a search service instance");
    }
    

    /**
     * Generate the search results document.
     */
    public void generate() throws IOException, SAXException, ProcessingException
    {
        try
        {
            if (resultsDoc == null)
            {
            	Context context = ContextUtil.obtainContext(objectModel);
                DiscoverQuery queryArgs = new DiscoverQuery();

            	// Sets the query
                queryArgs.setQuery(query);
                // start -1 because Solr indexing starts at 0 and OpenSearch
                // indexing starts at 1.
                queryArgs.setStart(start - 1);
                queryArgs.setMaxResults(rpp);

                // we want Items only
            	queryArgs.setDSpaceObjectFilter(Constants.ITEM);
                
                // sort info
                if(sort != null)
                {
                    String sortField = this.searchService.toSortFieldIndex(sort.getMetadata(), sort.getType());
                    if(SortOption.ASCENDING.equals( sortOrder ))
                        queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.asc);
                    else
                        queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.desc);
                }

                DiscoverResult queryResults = null;
                if(scope == null)
                    queryResults = SearchUtils.getSearchService().search(context, queryArgs);
                else
                    queryResults = SearchUtils.getSearchService().search(context, scope, queryArgs);

	            // creates the results array and generates the OpenSearch result
	            List<DSpaceObject> results = queryResults.getDspaceObjects();
	            
	            resultsDoc = openSearchService.getResultsDoc(context, format, query, (int) queryResults.getTotalSearchResults(), start, rpp, scope, results, FeedUtils.i18nLabels);
                FeedUtils.unmangleI18N(resultsDoc);
            }

            // Send the SAX events
            DOMStreamer streamer = new DOMStreamer(contentHandler, lexicalHandler);
            streamer.stream(resultsDoc);
        }
        catch (SQLException sqle)
		{
        	throw new SAXException(sqle);
		}
        catch (SearchServiceException se)
		{
			throw new ProcessingException(se);
		}
    }
    
    /**
     * Recycle
     */
    
    public void recycle()
    {
        this.searchService = null;
        super.recycle();
    }
	
}
