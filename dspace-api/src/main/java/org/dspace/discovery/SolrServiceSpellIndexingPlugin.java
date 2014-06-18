/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

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
            DCValue[] dcValues = ((Item) dso).getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (DCValue dcValue : dcValues) {
                document.addField("a_spell", dcValue.value);

            }
        }

    }
}
