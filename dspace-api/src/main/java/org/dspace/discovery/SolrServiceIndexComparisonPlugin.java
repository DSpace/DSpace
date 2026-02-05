/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.utils.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.DuplicateDetectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Indexes special normalised values used for comparing items, to be used in e.g. basic duplicate detection
 *
 * @author Kim Shepherd
 */
public class SolrServiceIndexComparisonPlugin implements SolrServiceIndexPlugin {

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    ItemService itemService;
    @Autowired
    DuplicateDetectionService duplicateDetectionService;

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(SolrServiceIndexComparisonPlugin.class);

    /**
     * Index the normalised name of the item to a solr field
     *
     * @param context DSpace context
     * @param idxObj the indexable item
     * @param document the Solr document
     */
    @Override
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {
        // Immediately return if this feature is not configured
        if (!configurationService.getBooleanProperty("duplicate.enable", false)) {
            return;
        }
        // Otherwise, continue with item indexing. Handle items, workflow items, and workspace items
        if (idxObj instanceof IndexableItem item3) {
            indexItemComparisonValue(context, item3.getIndexedObject(), document);
        } else if (idxObj instanceof IndexableWorkspaceItem item2) {
            WorkspaceItem workspaceItem = item2.getIndexedObject();
            if (workspaceItem != null) {
                Item item = workspaceItem.getItem();
                if (item != null) {
                    indexItemComparisonValue(context, item, document);
                }
            }
        } else if (idxObj instanceof IndexableWorkflowItem item1) {
            WorkflowItem workflowItem = item1.getIndexedObject();
            if (workflowItem != null) {
                Item item = workflowItem.getItem();
                if (item != null) {
                    indexItemComparisonValue(context, item, document);
                }
            }
        }
    }

    /**
     * Add the actual comparison value field to the given solr doc
     *
     * @param context DSpace context
     * @param item DSpace item
     * @param document Solr document
     */
    private void indexItemComparisonValue(Context context, Item item, SolrInputDocument document) {
        if (item != null) {
            // Build normalised comparison value and add to the document
            String comparisonValue = duplicateDetectionService.buildComparisonValue(context, item);
            if (!StringUtils.isBlank(comparisonValue)) {
                // Add the field to the document
                document.addField(configurationService.getProperty("duplicate.comparison.solr.field",
                        "deduplication_keyword"), comparisonValue);
            }
        }
    }
}
