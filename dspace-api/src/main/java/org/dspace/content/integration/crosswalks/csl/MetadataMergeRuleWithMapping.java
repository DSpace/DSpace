/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.csl;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.itemupdate.MetadataUtilities;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataMergeRuleWithMapping {
    @Autowired
    private ItemService itemService;
    private Map<String,List<String>> mapping;
    private String separator;
    private String replacementRegex;
    private String replacementValue;
    private boolean onlyFirstValues;

    public Map<String, List<String>> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, List<String>> mapping) {
        this.mapping = mapping;
    }

    public String getValue(Item item, String cslType) {
        List<String> fields = mapping.containsKey(cslType) ? mapping.get(cslType) : mapping.get("default");
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        List<String> values = fields.stream()
                .flatMap(field -> {
                    try {
                        if (onlyFirstValues) {
                            return Stream.ofNullable(itemService.getMetadata(item, field));
                        } else {
                            String[] metadataSplitted = MetadataUtilities.parseCompoundForm(field);
                            String schema = metadataSplitted[0];
                            String element = metadataSplitted[1];
                            String qualifier = metadataSplitted.length == 3 ? metadataSplitted[2] : null;

                            return itemService.getMetadata(item, schema, element, qualifier, Item.ANY)
                                    .stream()
                                    .map(MetadataValue::getValue);
                        }
                    } catch (ParseException e) {
                        throw new RuntimeException("Failed to parse metadata field: " + field, e);
                    }
                })
                .filter(StringUtils::isNotBlank)
                .map(value -> value.replaceAll(replacementRegex, replacementValue))
                .collect(Collectors.toList());
        return values.isEmpty() ? null : StringUtils.join(values, separator);
    }


    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getReplacementRegex() {
        return replacementRegex;
    }

    public void setReplacementRegex(String replacementRegex) {
        this.replacementRegex = replacementRegex;
    }

    public String getReplacementValue() {
        return replacementValue;
    }

    public void setReplacementValue(String replacementValue) {
        this.replacementValue = replacementValue;
    }

    public boolean isOnlyFirstValues() {
        return onlyFirstValues;
    }

    public void setOnlyFirstValues(boolean onlyFirstValues) {
        this.onlyFirstValues = onlyFirstValues;
    }
}
