/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements virtual field processing for split pagenumber range information.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldDate implements VirtualField {

    private final ItemService itemService;

    @Autowired
    public VirtualFieldDate(ItemService itemService) {
        this.itemService = itemService;
    }

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        // Check to see if the virtual field is already in the cache
        // - processing is quite intensive, so we generate all the values on first
        // request
        if (fieldCache.containsKey(fieldName)) {
            return new String[] { fieldCache.get(fieldName) };
        }

        String[] virtualFieldName = fieldName.split("\\.");
        String qualifier = virtualFieldName[1];

        if (qualifier.equals("date")) {
            // Get the citation from the item
            List<MetadataValue> dcvs = itemService.getMetadataByMetadataString(item, fieldName);
            if (dcvs != null && dcvs.size() > 0) {
                fieldCache.put("virtual.date.year", dcvs.get(0).getValue().substring(0, 4));
                if (dcvs.get(0).getValue().length() > 4) {
                    fieldCache.put("virtual.date.month", dcvs.get(0).getValue().substring(5, 7));
                }
                // Return the value of the virtual field (if any)
                if (fieldCache.containsKey(fieldName)) {
                    return new String[] { fieldCache.get(fieldName) };
                }
            }
        }
        return null;
    }
}