/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.virtual;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class Collected implements VirtualBean {

    @Autowired
    private ItemService itemService;

    /**
     * The fields for which the metadata will be retrieved
     */
    private List<String> fields;

    /**
     * Generic getter for the fields property
     * @return The list of fields to be used in this bean
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Generic setter for the fields property
     * @param fields    the list of fields to which the fields property will be set to
     */
    public void setFields(List<String> fields) {
        this.fields = fields;
    }


    public List<String> getValues(Context context, Item item) {
        List<String> resultValues = new LinkedList<>();
        List<String> value = this.getFields();
        for (String s : value) {
            String[] splittedString = s.split("\\.");

            List<MetadataValue> resultList = itemService.getMetadata(item,
                                                                     splittedString.length > 0 ? splittedString[0] :
                                                                         null,
                                                                     splittedString.length > 1 ? splittedString[1] :
                                                                         null,
                                                                     splittedString.length > 2 ? splittedString[2] :
                                                                         null,
                                                                     Item.ANY);

            for (MetadataValue metadataValue : resultList) {
                if (StringUtils.isNotBlank(metadataValue.getValue())) {
                    resultValues.add(metadataValue.getValue());
                }
            }
        }

        return resultValues;
    }
}
