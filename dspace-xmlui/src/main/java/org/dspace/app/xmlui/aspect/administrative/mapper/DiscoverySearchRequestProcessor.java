/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.administrative.mapper;

import java.io.IOException;
import java.util.List;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;

/**
 * Search using the Discovery index provider.
 *
 * @author mwood
 */
public class DiscoverySearchRequestProcessor
        implements SearchRequestProcessor
{
    @Override
    public List<DSpaceObject> doItemMapSearch(Context context, String queryString,
            Collection collection)
            throws IOException
    {
        DiscoverQuery query = new DiscoverQuery();
        query.setQuery(queryString);
        query.addFilterQueries("-location:l"+collection.getID());

        DiscoverResult results = null;
        try {
            results = SearchUtils.getSearchService().search(context, query);
        } catch (SearchServiceException ex) {
            throw new IOException(ex); // Best we can do with the interface method's signature
        }

        return results.getDspaceObjects();
    }
}
