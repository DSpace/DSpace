/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;

/**
 * Implements virtual field processing for generate an xml element to insert on
 * crosserf xml deposit file.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldCrossrefIssued implements VirtualFieldDisseminator, VirtualFieldIngester {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

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

    public boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value) {
        // NOOP - we won't add any metadata yet, we'll pick it up when we
        // finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map<String, String> fieldCache) {
        return false;
    }
}
