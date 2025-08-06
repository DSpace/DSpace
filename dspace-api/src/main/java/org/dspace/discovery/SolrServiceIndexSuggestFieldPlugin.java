/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Index values for each configured solr suggestion field to a _suggest solr field
 *
 * @author Kim Shepherd
 */
public class SolrServiceIndexSuggestFieldPlugin implements SolrServiceIndexPlugin {
    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger();

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected ConfigurationService configurationService;
    @Autowired(required = true)
    protected ItemService itemService;

    @Override
    public void additionalIndex(Context context, IndexableObject idxObj, SolrInputDocument document) {
        if (idxObj instanceof IndexableItem) {
            Item item = ((IndexableItem) idxObj).getIndexedObject();
            if (item != null) {
                try {
                    // Index all metadata fields configured as suggestion fields
                    String[] suggestionFields = configurationService.getArrayProperty("discovery.suggest.field");
                    for (String suggestionField : suggestionFields) {
                        List<MetadataValue> suggestionValues =
                                itemService.getMetadataByMetadataString(item, suggestionField);
                        List<String> sv = new ArrayList<String>();
                        for (MetadataValue v : suggestionValues) {
                            sv.add(v.getValue());
                        }
                        String docField = suggestionField + "_suggest";
                        document.addField(docField, sv);
                    }
                } catch (Exception e) {
                    log.error("Error while indexing suggestion fields," +
                            "Item: (id " + item.getID() + " name " + item.getName() + ")");
                }
            }
        }
    }
}

