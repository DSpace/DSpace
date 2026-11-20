/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements virtual field processing for split pagenumber range information.
 *
 * @author bollini
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldPageNumber implements VirtualField {

    private final ItemService itemService;

    @Autowired
    public VirtualFieldPageNumber(ItemService itemService) {
        this.itemService = itemService;
    }

    public String[] getMetadata(Context context, Item item, String fieldName) {
        List<MetadataValue> dcvs = itemService.getMetadataByMetadataString(item, "oaire.citation.startPage");
        List<MetadataValue> dcvs2 = itemService.getMetadataByMetadataString(item, "oaire.citation.endPage");

        if (CollectionUtils.isEmpty(dcvs) || CollectionUtils.isEmpty(dcvs2)) {
            return new String[] {};
        }

        return new String[] { dcvs.get(0).getValue() + " - " + dcvs2.get(0).getValue() };
    }
}