/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;

/**
 * Plugin for indexing the Items community. It helps search the Item by the community.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinSolrItemsCommunityIndexPlugin implements SolrServiceIndexPlugin {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ClarinSolrItemsCommunityIndexPlugin.class);

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {
        if (indexableObject instanceof IndexableItem) {
            Item item = ((IndexableItem) indexableObject).getIndexedObject();

            String owningCommunity = this.getOwningCommunity(context, item);
            document.addField("items_owning_community", owningCommunity);
        }
    }

    private String getOwningCommunity(Context context, Item item) {
        if (Objects.isNull(item)) {
            return " ";
        }
        Collection owningCollection = item.getOwningCollection();
        try {
            List<Community> communities = owningCollection.getCommunities();
            if (CollectionUtils.isEmpty(communities)) {
                log.error("Community list of the owning collection is empty.");
                return " ";
            }

            // First community is the owning community.
            Community owningCommunity = communities.get(0);
            if (Objects.isNull(owningCommunity)) {
                log.error("Owning community is null.");
                return " ";
            }

            return owningCommunity.getName();
        } catch (SQLException e) {
            log.error("Cannot getOwningCommunity for the Item: " + item.getID() + ", because: " + e.getSQLState());
        }

        return " ";
    }
}
