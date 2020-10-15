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
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements virtual field processing to retrieve issued date.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldDate implements VirtualField {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldDate.class);

    private final ItemService itemService;

    @Autowired
    public VirtualFieldDate(ItemService itemService) {
        this.itemService = itemService;
    }

    public String[] getMetadata(Context context, Item item, String fieldName) {

        String dateIssued = itemService.getMetadataFirstValue(item, "dc", "date", "issued", Item.ANY);
        if (StringUtils.isBlank(dateIssued)) {
            return new String[] {};
        }

        if (dateIssued.length() < 4) {
            LOGGER.warn("Invalid dc.date.issued found for item " + item.getID() + ": " + dateIssued);
            return new String[] {};
        }

        String[] virtualFieldName = fieldName.split("\\.");
        String qualifier = virtualFieldName.length == 3 ? virtualFieldName[2] : "year";

        switch (qualifier) {
            case "month":
                return dateIssued.length() > 7 ? new String[] { dateIssued.substring(5, 7) } : new String[] {};
            case "year":
            default:
                return new String[] { dateIssued.substring(0, 4) };

        }
    }
}