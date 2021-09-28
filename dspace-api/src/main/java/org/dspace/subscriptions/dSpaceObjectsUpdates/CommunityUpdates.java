/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions.dSpaceObjectsUpdates;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;



/**
 * Class which will be used to find
 * all community objects updated related with subscribed DSO
 *
 * @author Alba Aliu
 */
public class CommunityUpdates implements DSpaceObjectUpdates {
    private SearchService searchService;

    @Override
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency)
            throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addFilterQueries("search.resourcetype:" + Item.class.getSimpleName());
        discoverQuery.addFilterQueries("location.comm:(" + dSpaceObject.getID() + ")");
        discoverQuery.addFilterQueries("lastModified_dt:" + this.findLastFrequency(frequency));
        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        return discoverResult.getIndexableObjects();
    }

    public CommunityUpdates(SearchService searchService) {
        this.searchService = searchService;
    }
}
