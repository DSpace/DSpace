/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoverySearchFilter;

/**
 * Created with IntelliJ IDEA.
 * User: kevin
 * Date: 03/10/13
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
public class SolrServiceSpellIndexingPlugin implements SolrServiceIndexPlugin {

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters) {
        if(dso instanceof Item){
            Item item = (Item) dso;
            Metadatum[] Metadatums = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(item.getType());
            for (Metadatum Metadatum : Metadatums) {
                String field = Metadatum.schema + "." + Metadatum.element;
                String unqualifiedField = field;

                String value = Metadatum.value;

                if (value == null || StringUtils.equals(value, MetadataValue.PARENT_PLACEHOLDER_VALUE))
                {
                    continue;
                }

                if (Metadatum.qualifier != null && !Metadatum.qualifier.trim().equals(""))
                {
                    field += "." + Metadatum.qualifier;
                }

                if(!toIgnoreMetadataFields.contains(field)){
                    document.addField("a_spell", Metadatum.value);
                }
            }
        }
    }
}
