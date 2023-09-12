/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.clarin.ClarinItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Plugin for indexing the Items community. It helps search the Item by the community.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinSolrItemsCommunityIndexPlugin implements SolrServiceIndexPlugin {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ClarinSolrItemsCommunityIndexPlugin.class);

    @Autowired(required = true)
    protected ClarinItemService clarinItemService;

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {
        if (indexableObject instanceof IndexableItem) {
            Item item = ((IndexableItem) indexableObject).getIndexedObject();

            Community owningCommunity = clarinItemService.getOwningCommunity(context, item);
            String communityName = Objects.isNull(owningCommunity) ? " " : owningCommunity.getName();

            document.addField("items_owning_community", communityName);
        }
    }
}
