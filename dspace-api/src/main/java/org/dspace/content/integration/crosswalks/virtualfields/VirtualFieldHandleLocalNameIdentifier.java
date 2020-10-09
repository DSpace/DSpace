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
 * Implements virtual field processing to build custom identifier
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldHandleLocalNameIdentifier implements VirtualField {

    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        if (fieldCache.containsKey(fieldName)) {
            return (new String[] { (String) fieldCache.get(fieldName) });
        }

        String handle = item.getHandle();
        int position = handle.indexOf("/");

        if (StringUtils.isNotBlank(handle)) {
            fieldCache.put("virtual.handlelocalname", handle.substring(position + 1));
        } else {
            fieldCache.put("virtual.handlelocalname", "ATT-" + item.getID());
        }
        if (fieldCache.containsKey(fieldName)) {
            return (new String[] { (String) fieldCache.get(fieldName) });
        } else {
            return null;
        }
    }
}