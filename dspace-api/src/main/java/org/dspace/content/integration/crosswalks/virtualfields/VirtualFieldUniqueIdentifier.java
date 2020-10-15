/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Implements virtual field processing for generate an unique identifier of the
 * citation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4sciecnce.it)
 */
public class VirtualFieldUniqueIdentifier implements VirtualField {

    public String[] getMetadata(Context context, Item item, String fieldName) {
        String handle = item.getHandle();
        if (StringUtils.isNotBlank(handle)) {
            return new String[] { handle.replace("/", "_") };
        } else {
            return new String[] { "prev-" + item.getID() };
        }
    }
}