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

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class Concatenate implements VirtualBean {

    @Autowired
    private ItemService itemService;

    private List<String> fields;
    private String separator;

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getValue(Context context, Item item) {

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

            String resultString = "";
            for (int i = 0; i < resultList.size(); i++) {
                String metadataValueString = resultList.get(i).getValue();
                if (StringUtils.isNotBlank(metadataValueString)) {
                    if (StringUtils.isNotBlank(resultString)) {
                        resultString += this.getSeparator();
                    }
                    resultString += metadataValueString;
                }
            }
            if (StringUtils.isNotBlank(resultString)) {
                resultValues.add(resultString);
            }
        }

        String result = StringUtils.join(resultValues, this.getSeparator());

        return result;
    }
}
