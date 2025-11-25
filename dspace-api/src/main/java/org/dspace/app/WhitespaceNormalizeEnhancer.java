/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class WhitespaceNormalizeEnhancer extends RewriteEnhancer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(WhitespaceNormalizeEnhancer.class);
    private List<String> metaDataFields;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected MetadataFieldService metadatafieldService;

    protected String sourceEntityType;

    public void setSourceEntityType(String sourceEntityType) {
        this.sourceEntityType = sourceEntityType;
    }

    @Override
    public boolean canEnhance(Context context, Item item) {
        try {
            return sourceEntityType == null || sourceEntityType.equals(itemService.getEntityType(context, item).toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enhance(Context context, Item item) {
        HashMap<String, List<String>> newValues = new HashMap<>();
        boolean modified = false;
        for (String metaDataField : metaDataFields) {
            List<MetadataValue> values = itemService.getMetadataByMetadataString(item, metaDataField);
            if (values == null) {
                continue;
            }
            for (MetadataValue mv : values) {
                String text = mv.getValue();
                String textCopy = text;
                text = replaceSpecialSpaces(text);
                text = replaceSpecialNewline(text);
                text = replaceNewlineToReturnNewline(text);
                text = removeWhitespacesBeforeReturnNewline(text);
                text = removeZeroWidthSpaces(text);
                text = normalizeToNFC(text);
                if (!newValues.containsKey(metaDataField)) {
                    newValues.put(metaDataField, new ArrayList<>());
                }
                if (text.equals(textCopy)) {
                    newValues.get(metaDataField).add(textCopy);
                } else {
                    newValues.get(metaDataField).add(text);
                    modified = true;
                }
            }
        }
        if (modified) {
            updateItem(context, item, newValues);
        }
    }

    private static String doWhileReplaceAll(String original, String regex, String replacement) {
        String temp;
        do {
            temp = original;
            original = temp.replaceAll(regex, replacement);
        }
        while (!temp.equals(original));
        return original;
    }

    private static String replaceSpecialSpaces(String original) {
        String regex = "(\\u0009|\\u00A0|\\u1680|\\u2000|\\u2001|\\u2002|\\u2003|\\u2004|\\u2005|" +
            "\\u2006|\\u2007|\\u2008|\\u2009|\\u200A|\\u202F|\\u205F|\\u3000)+|(\\u0020){2,}";
        return doWhileReplaceAll(original, regex, " ");
    }

    private static String replaceSpecialNewline(String original) {
        String regex = "(\\u000B|\\u000C|\\u0085|\\u2028|\\u2029)";
        return doWhileReplaceAll(original, regex, "\n");
    }

    private static String replaceNewlineToReturnNewline(String original) {
        String regex = "(?<!\\u000D)\\u000A";
        return doWhileReplaceAll(original, regex, "\r\n");
    }

    private static String removeWhitespacesBeforeReturnNewline(String original) {
        String regex = "(\\u0020)+(?=\\u000D\\u000A)";
        return doWhileReplaceAll(original, regex, "");
    }

    private static String removeZeroWidthSpaces(String original) {
        String regex = "(\\u180E|\\u200B|\\u200C|\\u200D|\\u2060|\\uFEFF)+";
        return doWhileReplaceAll(original, regex, "");
    }

    private static String normalizeToNFC(String original) {
        return Normalizer.normalize(original, Normalizer.Form.NFC);
    }

    public void setMetaDataFields(List<String> metaDataFields) {
        this.metaDataFields = metaDataFields;
    }
}
