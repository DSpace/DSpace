/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.dspace.discovery.IndexingUtils.findDirectlyAuthorizedGroupAndEPersonPrefixedIds;
import static org.dspace.discovery.IndexingUtils.findTransitiveAdminGroupIds;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Indexes policies that yield write access to items.
 *
 * @author Koen Pauwels at atmire.com
 */
public class SolrServiceIndexItemEditorsPlugin implements SolrServiceIndexPlugin {
    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(SolrServiceIndexItemEditorsPlugin.class);

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Override
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {
        if (idxObj instanceof IndexableItem) {
            Item item = ((IndexableItem) idxObj).getIndexedObject();
            if (item != null) {
                try {
                    // Index groups with ADMIN rights on Collections containing the Item, on
                    // Communities containing those Collections, and recursively on any Community containing ssuch a
                    // Community.
                    // TODO: Strictly speaking we should also check for epersons who received admin rights directly,
                    //       without being part of the admin group. Finding them may be a lot slower though.
                    for (Collection collection : item.getCollections()) {
                        for (UUID unprefixedId : findTransitiveAdminGroupIds(context, collection)) {
                            document.addField("edit", "g" + unprefixedId);
                        }
                    }

                    // Index groups and epersons with WRITE or direct ADMIN rights on the Item.
                    List<String> prefixedIds = findDirectlyAuthorizedGroupAndEPersonPrefixedIds(
                        authorizeService, context, item, new int[] {Constants.WRITE, Constants.ADMIN}
                    );
                    for (String prefixedId : prefixedIds) {
                        document.addField("edit", prefixedId);
                    }
                } catch (SQLException e) {
                    log.error(LogHelper.getHeader(context, "Error while indexing resource policies",
                        "Item: (id " + item.getID() + " name " + item.getName() + ")" ));
                }
            }
        }
    }
}
