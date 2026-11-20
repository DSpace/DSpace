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
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;

public class ComplexUrlRule {
    @Autowired
    private ItemService itemService;
    @Autowired
    private HandleService handleService;
    private List<String> fields;

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getUrl(Item item) {
        String value;
        for (String field : fields) {
            switch (field) {
                case "[[handle]]":
                    value = getHandleUrl(item);
                    break;
                default:
                    value = getMetadataUrl(item, field);
            }
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String getHandleUrl(Item item) {
        String handle = handleService.getCanonicalForm(item.getHandle());
        if (StringUtils.isNotBlank(handle)) {
            return handle;
        }
        throw new RuntimeException("Item does not have a handle: " + item.getID());
    }

    private String getMetadataUrl(Item item, String field) {
        String value = itemService.getMetadata(item, field);
        if (StringUtils.isNotBlank(value)) {
            if (!value.startsWith("http") && field.equals("dc.identifier.doi")) {
                return "https://doi.org/" + value;
            }
            return value;
        }
        return null;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public void setHandleService(HandleService handleService) {
        this.handleService = handleService;
    }
}
