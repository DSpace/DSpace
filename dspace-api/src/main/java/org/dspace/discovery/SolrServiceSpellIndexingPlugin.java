/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kevin
 * Date: 03/10/13
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
public class SolrServiceSpellIndexingPlugin implements SolrServiceIndexPlugin {

    @Override
    public void additionalIndex(Context context, DSpaceObject dso, SolrInputDocument document) {
        if(dso instanceof Item){
            Item item = (Item) dso;
            Metadatum[] dcValues = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            List<String> toIgnoreMetadataFields = SearchUtils.getIgnoredMetadataFields(item.getType());
            for (Metadatum dcValue : dcValues) {
                String field = dcValue.schema + "." + dcValue.element;
                String unqualifiedField = field;

                String value = dcValue.value;

                if (value == null)
                {
                    continue;
                }

                if (dcValue.qualifier != null && !dcValue.qualifier.trim().equals(""))
                {
                    field += "." + dcValue.qualifier;
                }

                if(!toIgnoreMetadataFields.contains(field)){
                    document.addField("a_spell", dcValue.value);
                }
            }
        }
    }
}
