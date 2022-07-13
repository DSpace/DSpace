/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;

/**
 * The class defines a location index strategy used to collect items
 * information related to firstId and secondId items.
 * 
 * Collection and Community names are added to the document index in a
 * "parentlocation_s" field.
 */
public class ItemLocationDedupServiceIndexPlugin implements SolrDedupServiceIndexPlugin {

    private static final Logger log = LogManager.getLogger(ItemLocationDedupServiceIndexPlugin.class);

    @Override
    public void additionalIndex(Context context, UUID firstId, UUID secondId, SolrInputDocument document) {

        internal(context, firstId, document);
        if (firstId != secondId) {
            internal(context, secondId, document);
        }
    }

    private void internal(Context context, UUID itemId, SolrInputDocument document) {
        try {

            Item item = ContentServiceFactory.getInstance().getItemService().find(context, itemId);
            if (item == null) {
                // found a zombie reference in solr, ignore it
                return;
            }
            List<Community> communities = ContentServiceFactory.getInstance().getItemService().getCommunities(context,
                    item);
            List<Collection> collections = item.getCollections();

            // now put those into strings
            for (Community community : communities) {
                document.addField("parentlocation_s", community.getName());
            }

            for (Collection collection : collections) {
                document.addField("parentlocation_s", collection.getName());
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

}
