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

    /**
     * Retrieves and organizes the current virtual metadata fields on an item, grouped by
     * their source entity authority values.
     *
     * <p><strong>Purpose:</strong></p>
     * <p>This method creates a mapping between source entity UUIDs and their corresponding
     * virtual metadata values, enabling the enhancer to determine what virtual metadata
     * currently exists and which source entities they came from.</p>
     *
     * <p><strong>Virtual Metadata Pairing:</strong></p>
     * <p>The method pairs {@code cris.virtualsource.[qualifier]} fields (containing source UUIDs)
     * with {@code cris.virtual.[qualifier]} fields (containing the actual values) based on their
     * position order in the metadata arrays.</p>
     *
     * <p><strong>Inconsistency Handling:</strong></p>
     * <p>When the number of source and virtual metadata fields don't match (data corruption scenario):</p>
     * <ul>
     *   <li><strong>More sources than values:</strong> Extra sources are ignored</li>
     *   <li><strong>More values than sources:</strong> Extra values are associated with random UUIDs
     *       to enable cleanup as obsolete metadata</li>
     * </ul>
     *
     * <p><strong>Return Structure:</strong></p>
     * <p>Returns a Map where:</p>
     * <ul>
     *   <li><strong>Key:</strong> Source entity UUID (from {@code cris.virtualsource.[qualifier]} values)</li>
     *   <li><strong>Value:</strong> List of {@link MetadataValue} objects from {@code cris.virtual.[qualifier]}
     *       that were derived from that source entity</li>
     * </ul>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Item has:
     * - cris.virtualsource.author[0] = "person-uuid-123"
     * - cris.virtualsource.author[1] = "person-uuid-456"
     * - cris.virtual.author[0] = "0000-0000-0000-0000"
     * - cris.virtual.author[1] = "John Doe"
     *
     * Returns:
     * {
     *   "person-uuid-123" → [MetadataValue("0000-0000-0000-0000")],
     *   "person-uuid-456" → [MetadataValue("John Doe")]
     * }
     * </pre>
     *
     * @param item             the item to analyze for current virtual metadata
     * @param virtualQualifier the qualifier used for the virtual metadata fields (e.g., "author", "project")
     * @return a map grouping virtual metadata values by their source entity UUIDs
     * @see RelatedEntityItemEnhancer#enhance(org.dspace.core.Context, Item, boolean)
     * @see RelatedEntityItemEnhancer#VIRTUAL_METADATA_SCHEMA
     * @see RelatedEntityItemEnhancer#VIRTUAL_SOURCE_METADATA_ELEMENT
     */
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
