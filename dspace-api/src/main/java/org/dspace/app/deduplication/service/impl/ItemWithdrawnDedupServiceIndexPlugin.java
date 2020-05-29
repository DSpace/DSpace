/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.service.impl;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.util.ItemUtils;

/**
 * The class defines a withdrawn index strategy used to collect items
 * information related to firstId and secondId items.
 * 
 * Withdrawn status is collected in "dedup.withdrawn" field.
 */
public class ItemWithdrawnDedupServiceIndexPlugin implements SolrDedupServiceIndexPlugin {

    private static final Logger log = Logger.getLogger(ItemWithdrawnDedupServiceIndexPlugin.class);

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

            Integer status = ItemUtils.getItemStatus(context, item);
            if (status == 3) {
                document.addField(SolrDedupServiceImpl.RESOURCE_WITHDRAWN_FIELD, true);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

}
