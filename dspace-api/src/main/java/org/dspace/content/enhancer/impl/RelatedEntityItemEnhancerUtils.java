/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Utility methods used by {@link RelatedEntityItemEnhancer}
 *
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 *
 */
public class RelatedEntityItemEnhancerUtils {

    @Autowired
    private ItemService itemService;

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedEntityItemEnhancerUtils.class);

    public Map<String, List<MetadataValue>> getCurrentVirtualsMap(Item item, String virtualQualifier) {
        Map<String, List<MetadataValue>> currentVirtualsMap = new HashMap<String, List<MetadataValue>>();
        List<MetadataValue> sources = itemService.getMetadata(item, RelatedEntityItemEnhancer.VIRTUAL_METADATA_SCHEMA,
                RelatedEntityItemEnhancer.VIRTUAL_SOURCE_METADATA_ELEMENT, virtualQualifier, Item.ANY);
        List<MetadataValue> generated = itemService.getMetadata(item,
                RelatedEntityItemEnhancer.VIRTUAL_METADATA_SCHEMA, RelatedEntityItemEnhancer.VIRTUAL_METADATA_ELEMENT,
                virtualQualifier, Item.ANY);

        if (sources.size() != generated.size()) {
            LOGGER.error(
                    "inconsistent virtual metadata for the item {} got {} sources and {} generated virtual metadata",
                    item.getID().toString(), sources.size(), generated.size());
        }

        for (int i = 0; i < Integer.max(sources.size(), generated.size()); i++) {
            String authority;
            if (i < sources.size()) {
                authority = sources.get(i).getValue();
            } else {
                // we have less source than virtual metadata let's generate a random uuid to
                // associate with these extra metadata so that they will be managed as obsolete
                // value
                authority = UUID.randomUUID().toString();
            }
            List<MetadataValue> mvalues = currentVirtualsMap.get(authority);
            if (mvalues == null) {
                mvalues = new ArrayList<MetadataValue>();
            }
            if (i < generated.size()) {
                mvalues.add(generated.get(i));
            }
            currentVirtualsMap.put(authority, mvalues);
        }
        return currentVirtualsMap;
    }

}
