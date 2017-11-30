/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.opensearch;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.xml.dom.DOMStreamer;
import org.dspace.app.util.OpenSearch;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.FeedUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;
import org.xml.sax.SAXException;


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
public class StandardOpenSearchGenerator extends AbstractOpenSearchGenerator
                implements CacheableProcessingComponent, Recyclable
{

    /**
     * Generate the search results document.
     * Params have been parsed in superclass's setup() method
     */
    public void generate() throws IOException, SAXException, ProcessingException
    {
        try
        {
            // create a new search only if there isn't a cached one
            if (resultsDoc == null)
            {
                Context context = ContextUtil.obtainContext(objectModel);
                QueryArgs qArgs = new QueryArgs();
                qArgs.setQuery(query);
                qArgs.setStart(start);
                qArgs.setPageSize(rpp);

                if(sort != null)
                {
                    qArgs.setSortOption(sort);
                    qArgs.setSortOrder(sortOrder);
                }

                // Perform the search
                QueryResults qResults = null;
                if (scope == null)
                {
                    qResults = DSQuery.doQuery(context, qArgs);
                }
                else if (scope.getType() == Constants.COLLECTION)
                {
                    qResults = DSQuery.doQuery(context, qArgs, (Collection) scope);
                }
                else if (scope.getType() == Constants.COMMUNITY)
                {
                    qResults = DSQuery.doQuery(context, qArgs, (Community) scope);
                }
                else
                {
                    throw new IllegalStateException("Invalid container for search context (container type is "+scope.getClass().getName()+")");
                }

                // now instantiate the results
                DSpaceObject[] results = new DSpaceObject[qResults.getHitHandles().size()];
                for (int i = 0; i < qResults.getHitHandles().size(); i++)
                {
                    String myHandle = qResults.getHitHandles().get(i);
                    DSpaceObject dso = HandleManager.resolveToObject(context, myHandle);
                    if (dso == null)
                    {
                        throw new SQLException("Query \"" + query + "\" returned unresolvable handle: " + myHandle);
                    }
                    results[i] = dso;
                }

                // generates the OpenSearch result
                resultsDoc = OpenSearch.getResultsDoc(format, query, qResults.getHitCount(), qResults.getStart(), qResults.getPageSize(), scope, results, FeedUtils.i18nLabels);
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
    }

}
