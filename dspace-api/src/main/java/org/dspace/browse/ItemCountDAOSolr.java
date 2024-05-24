/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.FacetResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Discovery (Solr) driver implementing ItemCountDAO interface to look up item
 * count information in communities and collections. Caching operations are
 * intentionally not implemented because Solr already is our cache.
 */
public class ItemCountDAOSolr implements ItemCountDAO {
    /**
     * Log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemCountDAOSolr.class);

    /**
     * Hold the communities item count obtained from SOLR after the first query. This only works
     * well if the ItemCountDAO lifecycle is bound to the request lifecycle as
     * it is now. If we switch to a Spring-based instantiation we should mark
     * this bean as prototype
     **/
    private Map<String, Integer> communitiesCount = null;

    /**
     * Hold the collection item count obtained from SOLR after the first query
     **/
    private Map<String, Integer> collectionsCount = null;


    /**
     * Solr search service
     */
    @Autowired
    protected SearchService searchService;

    /**
     * Get the count of the items in the given container.
     *
     * @param context DSpace context
     * @param dso DspaceObject
     * @return count
     */
    @Override
    public int getCount(Context context, DSpaceObject dso) {
        loadCount(context);
        Integer val = null;
        if (dso instanceof Collection) {
            val = collectionsCount.get(dso.getID().toString());
        } else if (dso instanceof Community) {
            val = communitiesCount.get(dso.getID().toString());
        }

        if (val != null) {
            return val;
        } else {
            return 0;
        }
    }

    /**
     * make sure that the counts are actually fetched from Solr (if haven't been
     * cached in a Map yet)
     *
     * @param context DSpace Context
     */
    private void loadCount(Context context) {
        if (communitiesCount != null || collectionsCount != null) {
            return;
        }

        communitiesCount = new HashMap<>();
        collectionsCount = new HashMap<>();

        DiscoverQuery query = new DiscoverQuery();
        query.setFacetMinCount(1);
        query.addFacetField(new DiscoverFacetField("location.comm",
                                                   DiscoveryConfigurationParameters.TYPE_STANDARD, -1,
                                                   DiscoveryConfigurationParameters.SORT.COUNT));
        query.addFacetField(new DiscoverFacetField("location.coll",
                                                   DiscoveryConfigurationParameters.TYPE_STANDARD, -1,
                                                   DiscoveryConfigurationParameters.SORT.COUNT));
        query.addFilterQueries("search.resourcetype:" + IndexableItem.TYPE);    // count only items
        query.addFilterQueries("NOT(discoverable:false)");  // only discoverable
        query.addFilterQueries("withdrawn:false");  // only not withdrawn
        query.addFilterQueries("archived:true");  // only archived
        query.setMaxResults(0);

        DiscoverResult sResponse;
        try {
            sResponse = searchService.search(context, query);
            List<FacetResult> commCount = sResponse.getFacetResult("location.comm");
            List<FacetResult> collCount = sResponse.getFacetResult("location.coll");
            for (FacetResult c : commCount) {
                communitiesCount.put(c.getAsFilterQuery(), (int) c.getCount());
            }
            for (FacetResult c : collCount) {
                collectionsCount.put(c.getAsFilterQuery(), (int) c.getCount());
            }
        } catch (SearchServiceException e) {
            log.error("Could not initialize Community/Collection Item Counts from Solr: ", e);
        }
    }
}
