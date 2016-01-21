/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.MetadataValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kevin
 * Date: 03/10/13
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
public class SolrServiceSpellIndexingPlugin implements SolrServiceIndexPlugin {

    @Autowired(required = true)
    protected ItemService itemService;

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document) {
        if(dso instanceof Item){
            Item item = (Item) dso;
            List<MetadataValue> dcValues = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(item.getType());
            for (MetadataValue dcValue : dcValues) {
                if(!toIgnoreMetadataFields.contains(dcValue.getMetadataField().toString('.'))){
                    document.addField("a_spell", dcValue.getValue());
                }
            }
        }
    }
}
