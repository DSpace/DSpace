/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
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
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Discovery (Solr) driver implementing ItemCountDAO interface to look up item
 * count information in communities and collections. Counts are cached and
 * refreshed on startup (in a background thread) and periodically via a
 * scheduled cron job. If the cache is not yet loaded when a request comes in,
 * a synchronous refresh is triggered as fallback.
 *
 * The background init and cron refresh only run when both
 * {@code webui.strengths.show} and {@code webui.strengths.cache} are true.
 */
public class ItemCountDAOSolr implements ItemCountDAO {

    private static final Logger log = LogManager.getLogger(ItemCountDAOSolr.class);

    @Autowired
    private SearchService searchService;

    @Autowired
    private ConfigurationService configurationService;

    private volatile Map<String, Integer> communitiesCount;
    private volatile Map<String, Integer> collectionsCount;

    @PostConstruct
    public void init() {
        if (!isCacheEnabled()) {
            log.info("Item count cache disabled (webui.strengths.show/cache), skipping background init");
            return;
        }
        Thread thread = new Thread(() -> {
            log.info("Starting initial item count cache load in background thread");
            refreshCounts();
        }, "ItemCountCacheInit");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public int getCount(Context context, DSpaceObject dso) {
        if (!isLoaded()) {
            log.warn("Item count cache not yet loaded, triggering synchronous refresh");
            refreshCounts();
        }

        Integer val = null;
        if (dso instanceof Collection) {
            val = collectionsCount.get(dso.getID().toString());
        } else if (dso instanceof Community) {
            val = communitiesCount.get(dso.getID().toString());
        }

        return val != null ? val : 0;
    }

    /**
     * Refresh the cached counts by querying Solr.
     * Called on startup, by the scheduled cron job, and as synchronous fallback.
     * Skips execution if the cache is not enabled by configuration.
     */
    public void refreshCounts() {
        if (!isCacheEnabled()) {
            return;
        }
        log.info("Refreshing community/collection item counts from Solr");

        Map<String, Integer> newCommunitiesCount = new HashMap<>();
        Map<String, Integer> newCollectionsCount = new HashMap<>();

        DiscoverQuery query = new DiscoverQuery();
        query.setFacetMinCount(1);
        query.addFacetField(new DiscoverFacetField("location.comm",
                DiscoveryConfigurationParameters.TYPE_STANDARD, -1,
                DiscoveryConfigurationParameters.SORT.COUNT));
        query.addFacetField(new DiscoverFacetField("location.coll",
                DiscoveryConfigurationParameters.TYPE_STANDARD, -1,
                DiscoveryConfigurationParameters.SORT.COUNT));
        query.addFilterQueries("search.resourcetype:" + IndexableItem.TYPE);
        query.addFilterQueries("NOT(discoverable:false)");
        query.addFilterQueries("withdrawn:false");
        query.addFilterQueries("archived:true");
        query.setMaxResults(0);

        try {
            Context context = new Context(Context.Mode.READ_ONLY);
            try {
                DiscoverResult sResponse = searchService.search(context, query);
                List<FacetResult> commCount = sResponse.getFacetResult("location.comm");
                List<FacetResult> collCount = sResponse.getFacetResult("location.coll");
                for (FacetResult c : commCount) {
                    newCommunitiesCount.put(c.getAsFilterQuery(), (int) c.getCount());
                }
                for (FacetResult c : collCount) {
                    newCollectionsCount.put(c.getAsFilterQuery(), (int) c.getCount());
                }
                this.communitiesCount = Collections.unmodifiableMap(newCommunitiesCount);
                this.collectionsCount = Collections.unmodifiableMap(newCollectionsCount);
                log.info("Item counts refreshed: {} communities, {} collections",
                        newCommunitiesCount.size(), newCollectionsCount.size());
            } finally {
                context.complete();
            }
        } catch (SearchServiceException e) {
            log.error("Could not refresh Community/Collection Item Counts from Solr: ", e);
        } catch (Exception e) {
            log.error("Error creating context for item count refresh: ", e);
        }
    }

    /**
     * @return true if the cache should be used, based on configuration
     */
    private boolean isCacheEnabled() {
        return configurationService.getBooleanProperty("webui.strengths.show", false)
            && configurationService.getBooleanProperty("webui.strengths.cache", true);
    }

    /**
     * @return true if counts have been loaded at least once
     */
    public boolean isLoaded() {
        return communitiesCount != null && collectionsCount != null;
    }
}
