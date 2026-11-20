/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Objects;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.MapConverters;
import org.dspace.util.SimpleMapConverter;

/**
 * Implementation of {@link VirtualField} that maps the key present in the field name
 * with a value present on a specific file properties.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldMapConverter implements VirtualField {

    private ItemService itemService;

    private final MapConverters mapConverters;

    public VirtualFieldMapConverter(ItemService itemService, MapConverters mapConverters) {
        this.itemService = itemService;
        this.mapConverters = mapConverters;
    }

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String[] virtualFieldName = fieldName.split("\\.");
        if (virtualFieldName.length != 4) {
            throw new IllegalArgumentException("Invalid virtual field name for map converter: " + fieldName);
        }

        SimpleMapConverter mapConverter = mapConverters.getConverter(virtualFieldName[2])
            .orElseThrow(() -> new IllegalArgumentException("No MapConverter found for field name: " + fieldName));

        String metadataField = virtualFieldName[3].replaceAll("-", ".");
        return itemService.getMetadataByMetadataString(item, metadataField).stream()
                .map(metadataValue -> mapConverter.getValue(
                        mapConverter.isUseAuthority() ? metadataValue.getAuthority() : metadataValue.getValue()))
                .filter(Objects::nonNull).toArray(String[]::new);
    }

}
