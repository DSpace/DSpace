/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that returns all metadata value
 * authorities of the given metadata field for the specific item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldAuthority implements VirtualField {

    @Autowired
    private ItemService itemService;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String[] qualifiers = StringUtils.split(fieldName, ".");
        if (qualifiers.length != 3) {
            throw new IllegalArgumentException("Invalid field name " + fieldName);
        }

        String metadataField = StringUtils.replaceAll(qualifiers[2], "-", ".");

        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .map(MetadataValue::getAuthority)
            .map(authority -> isGenerateAuthority(authority) || isReferenceAuthority(authority) ? null : authority)
            .toArray(String[]::new);
    }

    private boolean isGenerateAuthority(String authority) {
        return StringUtils.startsWith(authority, AuthorityValueService.GENERATE);
    }

    private boolean isReferenceAuthority(String authority) {
        return StringUtils.startsWith(authority, AuthorityValueService.REFERENCE);
    }

}
