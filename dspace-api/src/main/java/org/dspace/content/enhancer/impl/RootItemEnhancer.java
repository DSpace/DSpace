/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A general-purpose item enhancer that enhance for root items based on configurable metadata fields.
 *
 * @author Adamo Fapohunda (AdamF42 - adamo.fapohunda at 4science.com)
 */
public class RootItemEnhancer extends RelatedEntityItemEnhancer {
    private static final Logger log = LoggerFactory.getLogger(RootItemEnhancer.class);

    @Autowired
    private ItemService itemService;

    private String rootMetadataField;
    private String sourceItemMetadataField;

    public void setRootMetadataField(String rootMetadataField) {
        this.rootMetadataField = rootMetadataField;
        this.setRelatedItemMetadataFields(List.of(rootMetadataField));
    }

    public void setSourceItemMetadataField(String sourceItemMetadataField) {
        this.sourceItemMetadataField = sourceItemMetadataField;
        this.setSourceItemMetadataFields(List.of(sourceItemMetadataField));
    }

    @Override
    public boolean performEnhancement(Context context, Item item) {
        try {
            if (isRootItem(item)) {
                String source = itemService.getMetadata(item, sourceItemMetadataField);
                if (source != null && !source.isEmpty()) {
                    addVirtualField(context, item, source, item.getID().toString(), null, Choices.CF_ACCEPTED);
                    addVirtualSourceField(context, item, item.getID().toString());
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error("Error enhancing item {}: {}", item.getID(), e.getMessage(), e);
        }
        return false;
    }

    @Override
    protected Map<String, List<MetadataValueDTO>> getToBeVirtualMetadata(Context context, Item item) {
        Map<String, List<MetadataValueDTO>> tobeVirtualMetadataMap = new HashMap<String, List<MetadataValueDTO>>();

        if (isRootItem(item)) {
            List<MetadataValueDTO> tobeVirtualMetadata = new ArrayList<>();
            String source = itemService.getMetadata(item, sourceItemMetadataField);
            if (source != null && !source.isEmpty()) {
                MetadataValueDTO virtual = new MetadataValueDTO();
                virtual.setSchema(VIRTUAL_METADATA_SCHEMA);
                virtual.setElement(VIRTUAL_METADATA_ELEMENT);
                virtual.setQualifier(getVirtualQualifier());
                virtual.setValue(source);
                virtual.setAuthority(item.getID().toString());
                virtual.setConfidence(Choices.CF_ACCEPTED);
                tobeVirtualMetadata.add(virtual);
                tobeVirtualMetadataMap.put(item.getID().toString(), tobeVirtualMetadata);
            }
        } else {
            return super.getToBeVirtualMetadata(context, item);
        }

        return tobeVirtualMetadataMap;
    }

    private boolean isRootItem(Item item) {
        String rootMetadataFieldValue = itemService.getMetadata(item, rootMetadataField);
        return rootMetadataFieldValue == null || rootMetadataFieldValue.isEmpty();
    }

}
