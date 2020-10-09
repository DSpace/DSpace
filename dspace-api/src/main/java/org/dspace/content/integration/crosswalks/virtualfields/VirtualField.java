/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Map;

import org.dspace.content.Item;

public interface VirtualField extends VirtualFieldDisseminator, VirtualFieldIngester {

    public default boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value) {
        // NOOP - we won't add any metadata yet, we'll pick it up when we finalize the item
        return true;
    }

    public default boolean finalizeItem(Item item, Map<String, String> fieldCache) {
        return false;
    }
}
