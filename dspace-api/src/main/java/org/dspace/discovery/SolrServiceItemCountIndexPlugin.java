/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableDSpaceObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Solr index plugin that manages the {@code item.count} field on
 * community and collection Solr documents.
 *
 * <p>During indexing ({@link #additionalIndex}), it computes the item count
 * from the database via {@link ItemService} and sets it on the Solr document.</p>
 *
 * <p>{@link #readItemCount(String)} reads the stored count for a single document,
 * called by {@link org.dspace.browse.ItemCountDAOSolr#getCount}.</p>
 *
 * <p>For Item documents the indexing hook is a no-op.</p>
 */
public class SolrServiceItemCountIndexPlugin implements SolrServiceIndexPlugin {

    private static final Logger log = LogManager.getLogger(SolrServiceItemCountIndexPlugin.class);

    public static final String ITEM_COUNT_FIELD = "item.count";

    @Autowired
    private SolrSearchCore solrSearchCore;

    @Autowired
    private ItemService itemService;

    /**
     * Compute and set {@code item.count} during indexing of communities/collections
     * by counting items from the database.
     */
    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        if (!(dso instanceof IndexableDSpaceObject)) {
            return;
        }
        DSpaceObject indexed = ((IndexableDSpaceObject) dso).getIndexedObject();
        try {
            int count;
            if (indexed instanceof Community) {
                count = itemService.countItems(context, (Community) indexed);
            } else if (indexed instanceof Collection) {
                count = itemService.countItems(context, (Collection) indexed);
            } else {
                return;
            }
            if (count > 0) {
                document.addField(ITEM_COUNT_FIELD, count);
            }
        } catch (Exception e) {
            log.warn("Could not compute item.count for {}: {}", dso.getUniqueIndexID(), e.getMessage());
        }
    }

    /**
     * Read the {@code item.count} value from the Solr document identified by
     * the given unique index ID.
     *
     * @param uniqueId the Solr unique ID (e.g. "Community-uuid" or "Collection-uuid")
     * @return the stored item count, or 0 if not found or on error
     */
    public int readItemCount(String uniqueId) {
        try {
            SolrClient solr = solrSearchCore.getSolr();
            if (solr == null) {
                return 0;
            }
            SolrQuery query = new SolrQuery(SearchUtils.RESOURCE_UNIQUE_ID + ":\"" + uniqueId + "\"");
            query.setFields(ITEM_COUNT_FIELD);
            query.setRows(1);
            QueryResponse response = solr.query(query, solrSearchCore.REQUEST_METHOD);
            SolrDocumentList docs = response.getResults();
            if (docs != null && !docs.isEmpty()) {
                SolrDocument existing = docs.get(0);
                Object val = existing.getFieldValue(ITEM_COUNT_FIELD);
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Could not read item.count for {}: {}", uniqueId, e.getMessage());
        }
        return 0;
    }
}
