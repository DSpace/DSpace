/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import org.dspace.content.Item;

/**
 * Interface for virtual field that allow to get metadata from item and add metadata to an item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface VirtualField extends VirtualFieldDisseminator, VirtualFieldIngester {

    public default boolean addMetadata(Item item, String fieldName, String value) {
        // NOOP - we won't add any metadata yet, we'll pick it up when we finalize the item
        return true;
    }

    public default boolean finalizeItem(Item item) {
        return false;
    }
}
