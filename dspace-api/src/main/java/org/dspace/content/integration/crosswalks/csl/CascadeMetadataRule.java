/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.csl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

public class CascadeMetadataRule {
    @Autowired
    private ItemService itemService;
    private List<String> fields;

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getValue(Item item) {
        for (String field : fields) {
            String value = getMetadata(item, field);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }


    private String getMetadata(Item item, String field) {
        String value = itemService.getMetadata(item, field);
        return value;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }
}
