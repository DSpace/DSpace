/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.ArrayList;
import java.util.List;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

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
            
            List<String> toIgnoreMetadataFields = new ArrayList<String>();
            String ignoreFieldsString = new DSpace().getConfigurationService().getProperty("discovery.index.ignore");
            if (ignoreFieldsString != null) {
                if (ignoreFieldsString.indexOf(",") != -1) {
                    for (int i = 0; i < ignoreFieldsString.split(",").length; i++) {
                        toIgnoreMetadataFields.add(ignoreFieldsString.split(",")[i].trim());
                    }
                } else {
                    toIgnoreMetadataFields.add(ignoreFieldsString);
                }
            }
            Metadatum[] dcValues = ((Item) dso).getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (Metadatum dcValue : dcValues) {
                String field = dcValue.schema + "." + dcValue.element;
                String unqualifiedField = field;

                if (dcValue.value == null) {
                    continue;
                }

                if (dcValue.qualifier != null && !dcValue.qualifier.trim().equals("")) {
                    field += "." + dcValue.qualifier;
                }
                 //We are not indexing provenance, this is useless
                if (toIgnoreMetadataFields != null && (toIgnoreMetadataFields.contains(field) 
                        || toIgnoreMetadataFields.contains(unqualifiedField + "." + Item.ANY))) {
                    continue;
                }
                document.addField("a_spell", dcValue.value);

            }
        }

    }
}
