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
 * Implements virtual field processing for generate an xml element to insert on
 * crosserf xml deposit file.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldCrossrefIssued implements VirtualField {

    private final ItemService itemService;

    @Autowired
    public VirtualFieldCrossrefIssued(ItemService itemService) {
        this.itemService = itemService;
    }

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        // Check to see if the virtual field is already in the cache
        // - processing is quite intensive, so we generate all the values on
        // first request
        if (fieldCache.containsKey(fieldName)) {
            return new String[] { fieldCache.get(fieldName) };
        }
        List<MetadataValue> mds = itemService.getMetadata(item, "dc", "date", "issued", Item.ANY);

        String element = "";
        if (mds != null && mds.size() > 0) {
            String[] tmp = mds.get(0).getValue().split("-");
            element = "<year>" + tmp[0] + "</year>";
        }
        fieldCache.put("virtual.crossrefissued", element);
        // Return the value of the virtual field (if any)
        if (fieldCache.containsKey(fieldName)) {
            return new String[] { fieldCache.get(fieldName) };
        }
        return null;
    }
}
