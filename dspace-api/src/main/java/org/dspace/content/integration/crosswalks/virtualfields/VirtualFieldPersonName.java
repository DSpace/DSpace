/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that returns the first or last name
 * from the dc.title metadata.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldPersonName implements VirtualField {

    private static Logger log = LogManager.getLogger(VirtualFieldPersonName.class);

    public static final String LAST_NAME = "lastName";
    public static final String FIRST_NAME = "firstName";

    private final ItemService itemService;

    @Autowired
    public VirtualFieldPersonName(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String title = itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);

        if (StringUtils.isBlank(title)) {
            return new String[] {};
        }

        if (!title.contains(",")) {
            return new String[] { title };
        }

        String[] virtualFieldName = fieldName.split("\\.");
        String qualifier = virtualFieldName[2];

        try {
            switch (qualifier) {
                case FIRST_NAME:
                    return new String[] { title.split(",")[1].trim() };
                case LAST_NAME:
                    return new String[] { title.split(",")[0].trim() };
                default:
                    throw new IllegalArgumentException("Invalid qualifier for personName virtual field: " + qualifier);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn("Something went wrong for metadata: " + title, e);
            return new String[] { title.replace(",", "") };
        }

    }

}
