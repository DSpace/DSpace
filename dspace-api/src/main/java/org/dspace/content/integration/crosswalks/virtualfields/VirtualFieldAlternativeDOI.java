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
import org.springframework.beans.factory.annotation.Autowired;


public class VirtualFieldAlternativeDOI implements VirtualField {

    @Autowired
    private ItemDOIService itemDOIService;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        String[] qualifiers = StringUtils.split(fieldName, ".");
        if (qualifiers.length != 3) {
            throw new IllegalArgumentException("Invalid field name " + fieldName);
        }

        return itemDOIService.getAlternativeDOIFromItem(item);
    }
}
