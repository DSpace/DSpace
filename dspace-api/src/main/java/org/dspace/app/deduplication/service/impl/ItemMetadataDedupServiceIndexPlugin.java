/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;

/**
 * The class defines a metadata index strategy used to collect metadata
 * information related to firstId and secondId items.
 * 
 * Metadata values are added to the document index in a field as configured in
 * the bean.
 */
public class ItemMetadataDedupServiceIndexPlugin implements SolrDedupServiceIndexPlugin {

    private static final Logger log = Logger.getLogger(ItemMetadataDedupServiceIndexPlugin.class);

    private List<String> metadata;

    private String field;

    private Integer itemType;

    @Override
    public void additionalIndex(Context context, UUID firstId, UUID secondId, SolrInputDocument document) {

        internal(context, firstId, document);
        if (firstId != secondId) {
            internal(context, secondId, document);
        }

    }

    private void internal(Context context, UUID itemId, SolrInputDocument document) {
        try {

            Item item = ContentServiceFactory.getInstance().getItemService().find(context, itemId);

            if (item == null) {
                // found a zombie reference in solr, ignore it
                return;
            } else {
                EntityType type = getEntityType(context);

                if (type != null && type.getID() != item.getType()) {
                    // filter the value
                    return;
                }
            }

            for (String meta : metadata) {
                for (MetadataValue mm : ContentServiceFactory.getInstance().getItemService()
                        .getMetadataByMetadataString(item, meta)) {
                    if (StringUtils.isNotEmpty(field)) {
                        document.addField(field, mm.getValue());
                    } else {
                        document.addField(mm.getMetadataField().toString('.') + "_s", mm.getValue());
                    }
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public List<String> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<String> metadata) {
        this.metadata = metadata;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setType(Integer itemType) {
        this.itemType = itemType;
    }

    private EntityType getEntityType(Context context) {
        try {
            if (itemType != null) {
                EntityTypeService entityService = ContentServiceFactory.getInstance().getEntityTypeService();
                EntityType type = entityService.find(context, itemType);

                return type;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("The bean attribute Type is not valid", e);

            return null;
        }
    }

}
