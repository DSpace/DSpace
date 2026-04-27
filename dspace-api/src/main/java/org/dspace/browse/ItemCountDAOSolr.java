/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SolrServiceItemCountIndexPlugin;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Discovery (Solr) driver implementing ItemCountDAO interface to look up item
 * count information in communities and collections.
 * <p>
 * Delegates Solr read operations to {@link SolrServiceItemCountIndexPlugin}.
 * <p>
 * The {@link #refreshCounts()} method re-indexes all communities and collections,
 * which triggers {@link SolrServiceItemCountIndexPlugin#additionalIndex} to
 * compute and store item counts from the database.
 * <p>
 * It is called:
 * <ul>
 *   <li>At startup in a background thread (non-blocking)</li>
 *   <li>Periodically via a scheduled cron job</li>
 * </ul>
 * Both the background init and the cron refresh only run when
 * {@code webui.strengths.show} and {@code webui.strengths.cache} are both true.
 */
public class ItemCountDAOSolr implements ItemCountDAO {

    private static final Logger log = LogManager.getLogger(ItemCountDAOSolr.class);

    @Autowired
    private SolrServiceItemCountIndexPlugin itemCountIndexPlugin;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public int getCount(Context context, DSpaceObject dso) {
        String uniqueId;
        if (dso instanceof Collection) {
            uniqueId = IndexableCollection.TYPE + "-" + dso.getID().toString();
        } else if (dso instanceof Community) {
            uniqueId = IndexableCommunity.TYPE + "-" + dso.getID().toString();
        } else {
            return 0;
        }
        return itemCountIndexPlugin.readItemCount(uniqueId);
    }

    /**
     * Refresh item counts by re-indexing all communities and collections.
     * This triggers {@link SolrServiceItemCountIndexPlugin#additionalIndex}
     * which computes the count from the database and writes it to the Solr document.
     * Called by the scheduled cron job.
     */
    public void refreshCounts() {
        if (!isCacheEnabled()) {
            return;
        }
        log.info("Refreshing community/collection item counts by re-indexing");

        try {
            Context context = new Context(Context.Mode.READ_ONLY);
            try {
                List<Community> communities = communityService.findAll(context);
                for (Community community : communities) {
                    indexingService.indexContent(context, new IndexableCommunity(community), true, false);
                }

                List<Collection> collections = collectionService.findAll(context);
                for (Collection collection : collections) {
                    indexingService.indexContent(context, new IndexableCollection(collection), true, false);
                }

                indexingService.commit();
                log.info("Item counts refreshed: {} communities, {} collections re-indexed",
                    communities.size(), collections.size());
            } finally {
                context.complete();
            }
        } catch (SQLException e) {
            log.error("Database error during item count refresh: ", e);
        } catch (Exception e) {
            log.error("Error during item count refresh: ", e);
        }
    }

    private boolean isCacheEnabled() {
        return configurationService.getBooleanProperty("webui.strengths.show", false)
            && configurationService.getBooleanProperty("webui.strengths.cache", true);
    }
}
