/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;

/**
 * Implements virtual field processing for generate an unique identifier of the
 * citation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4sciecnce.it)
 */
public class VirtualFieldUniqueIdentifier implements VirtualField {

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        // Check to see if the virtual field is already in the cache
        // - processing is quite intensive, so we generate all the values on first
        // request
        if (fieldCache.containsKey(fieldName)) {
            return new String[] { fieldCache.get(fieldName) };
        }
        String handle = item.getHandle();
        if (StringUtils.isNotBlank(handle)) {
            fieldCache.put("virtual.uniqueidentifier", handle.replace("/", "_"));
        } else {
            fieldCache.put("virtual.uniqueidentifier", "prev-" + item.getID());
        }
        // Return the value of the virtual field (if any)
        if (fieldCache.containsKey(fieldName)) {
            return new String[] { fieldCache.get(fieldName) };
        }
        return null;
    }
}