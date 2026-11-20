/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Builds the values at specific place.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class VirtualFieldAtPlace implements VirtualField {

    private final ItemService itemService;

    @Autowired
    public VirtualFieldAtPlace(ItemService itemService) {
        this.itemService = itemService;
    }

    public String[] getMetadata(Context context, Item item, String fieldName) {
        String[] virtualFieldName = fieldName.split("\\.", 4);
        if (virtualFieldName.length != 4) {
            return new String[] {};
        }

        String metadataField = virtualFieldName[2].replaceAll("-", ".");
        int place = Integer.parseInt(virtualFieldName[3]);
        List<MetadataValue> values = itemService.getMetadataByMetadataString(item, metadataField);
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }

        return new String[] { values.get(place).getValue() };
    }

}
