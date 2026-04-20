/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.AccessStatus;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;


/**
 * This plugin enables the indexing of access status for the item as a filter
 * and keyword
 * @author paulo-graca
 *
 */
public class SolrServiceIndexAccessStatusPlugin implements SolrServiceIndexPlugin {

    AccessStatusService accessStatusService = AccessStatusServiceFactory.getInstance().getAccessStatusService();

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {
        if (indexableObject instanceof IndexableItem) {
            Item item = ((IndexableItem) indexableObject).getIndexedObject();
            String accessStatus;

            try {
                accessStatus = retrieveItemAccessStatus(context,item);
            } catch (SQLException e) {
                accessStatus = DefaultAccessStatusHelper.UNKNOWN;
            }

            // _keyword and _filter because
            // they are needed in order to work as a facet and filter.
            document.addField("access_status_keyword", accessStatus);
            document.addField("access_status_filter", accessStatus);

        }
    }

    /**
     * Checks whether the given item has a bundle with the name ORIGINAL
     * containing at least one bitstream.
     *
     * @param item to check
     * @return String with one of the following controlled values
        EMBARGO = "embargo"
        METADATA_ONLY = "metadata.only"
        OPEN_ACCESS = "open.access"
        RESTRICTED = "restricted"
        UNKNOWN = "unknown"
     */
    private String retrieveItemAccessStatus(Context context, Item item) throws SQLException {
        AccessStatus accessStatus = accessStatusService.getAccessStatus(context, item);
        return accessStatus.getStatus();
    }
}
