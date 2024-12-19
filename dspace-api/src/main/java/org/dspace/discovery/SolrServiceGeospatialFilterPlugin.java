/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This plugin adds indicators to the solr document to determine if the item
 * has geospatial metadata
 *
 * The facet is added to Discovery in the usual way (create a searchFilter bean
 * and add it to the expected place) just with an empty list of used metadata
 * fields because there are none.
 *
 * @author Kim Shepherd
 */

public class SolrServiceGeospatialFilterPlugin implements SolrServiceIndexPlugin {

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    ItemService itemService;
    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {
        if (indexableObject instanceof IndexableItem) {
            Item item = ((IndexableItem) indexableObject).getIndexedObject();
            // Get configured field name
            String geospatialField = configurationService.getProperty("discovery.filter.geospatial.field");
            if (geospatialField == null) {
                return;
            }
            String[] fieldParts = geospatialField.split("\\.", 3);
            if (fieldParts.length < 2) {
                return;
            }
            boolean hasGeospatialMetadata = itemService.getMetadataFirstValue(item,
                    fieldParts[0], fieldParts[1], fieldParts.length > 2 ? fieldParts[2] : null, Item.ANY) != null;

            // also add _keyword and _filter because
            // they are needed in order to work as a facet and filter.
            document.addField("has_geospatial_metadata", hasGeospatialMetadata);
            document.addField("has_geospatial_metadata_keyword", hasGeospatialMetadata);
            document.addField("has_geospatial_metadata_filter", hasGeospatialMetadata);
        }
    }
}
