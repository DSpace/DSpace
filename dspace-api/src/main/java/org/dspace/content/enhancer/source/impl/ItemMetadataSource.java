/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.source.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.enhancer.source.MetadataSource;
import org.dspace.content.enhancer.source.RawMetadataValue;
import org.dspace.content.service.ItemService;

/**
 * Reads metadata values from an {@link Item} through the {@link ItemService}.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ItemMetadataSource implements MetadataSource {

    private final Item item;
    private final ItemService itemService;

    /**
     * Create a metadata source for the given item.
     *
     * @param item        the item whose metadata is read
     * @param itemService the service used to read the item metadata
     */
    public ItemMetadataSource(Item item, ItemService itemService) {
        this.item = item;
        this.itemService = itemService;
    }

    @Override
    public List<RawMetadataValue> getValues(String field) {
        if (field == null) {
            return Collections.emptyList();
        }

        return itemService.getMetadataByMetadataString(item, field).stream()
                          .map(ItemMetadataSource::toRawMetadataValue)
                          .collect(Collectors.toList());
    }

    /**
     * Map a persisted metadata value to a raw metadata value.
     *
     * @param metadataValue the persisted metadata value
     * @return the corresponding raw metadata value
     */
    private static RawMetadataValue toRawMetadataValue(MetadataValue metadataValue) {
        return new RawMetadataValue(metadataValue.getValue(), metadataValue.getLanguage(),
                                    metadataValue.getAuthority(), metadataValue.getConfidence());
    }

}
