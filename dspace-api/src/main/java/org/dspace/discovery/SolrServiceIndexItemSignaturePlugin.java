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
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Indexes item "signatures" used for duplicate detection
 *
 * @author Kim Shepherd
 */
public class SolrServiceIndexItemSignaturePlugin implements SolrServiceIndexPlugin {

    @Autowired
    ConfigurationService configurationService;

    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(SolrServiceIndexItemSignaturePlugin.class);

    /**
     * Index the normalised name of the item to a _signature field
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
        if (idxObj instanceof IndexableItem) {
            indexItemSignature(context, ((IndexableItem) idxObj).getIndexedObject(), document);
        } else if (idxObj instanceof IndexableWorkspaceItem) {
            WorkspaceItem workspaceItem = ((IndexableWorkspaceItem) idxObj).getIndexedObject();
            if (workspaceItem != null) {
                Item item = workspaceItem.getItem();
                if (item != null) {
                    indexItemSignature(context, item, document);
                }
            }
        } else if (idxObj instanceof IndexableWorkflowItem) {
            WorkflowItem workflowItem = ((IndexableWorkflowItem) idxObj).getIndexedObject();
            if (workflowItem != null) {
                Item item = workflowItem.getItem();
                if (item != null) {
                    indexItemSignature(context, item, document);
                }
            }
        }
    }

    /**
     * Add the actual signature field to the given solr doc
     *
     * @param context DSpace context
     * @param item DSpace item
     * @param document Solr document
     */
    private void indexItemSignature(Context context, Item item, SolrInputDocument document) {
        if (item != null) {
            // Construct signature object
            String signature = item.getName();
            if (signature != null) {
                if (configurationService.getBooleanProperty("duplicate.signature.normalise.lowercase")) {
                    signature = signature.toLowerCase(context.getCurrentLocale());
                }
                if (configurationService.getBooleanProperty("duplicate.signature.normalise.whitespace")) {
                    signature = signature.replaceAll("\\s+", "");
                }
                // Add the field to the document
                document.addField("item_signature", signature);
            }
        }
    }
}
