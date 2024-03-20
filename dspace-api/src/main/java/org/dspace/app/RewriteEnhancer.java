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
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public abstract class RewriteEnhancer extends AbstractItemEnhancer {
    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataFieldService metadatafieldService;

    /**
     * Update item, replacing metadata field values with those in newValues. A field is replaced if its field name is
     * contained as a key in newValues, even if the corresponding list is empty.
     *
     * @param context   DSpace Context
     * @param item      Item
     * @param newValues keys: field names; values: List of new values of this field.
     */
    protected void updateItem(Context context, Item item, HashMap<String, List<String>> newValues) {
        HashMap<String, HashSet<String>> oldValues = new HashMap<>();
        try {
            for (String tf : newValues.keySet()) {
                List<MetadataValue> fields = itemService.getMetadataByMetadataString(item, tf);

                if (!fields.isEmpty()) {
                    oldValues.put(tf, new HashSet<>());
                    oldValues.get(tf).addAll(fields.stream().map(MetadataValue::getValue).collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            return;
        }

        reloop:
        while (true) {
            for (String field : oldValues.keySet()) {
                if (newValues.containsKey(field)) {
                    for (String oldValue : oldValues.get(field)) {
                        if (newValues.get(field).contains(oldValue)) {
                            newValues.get(field).remove(oldValue);
                            oldValues.get(field).remove(oldValue);
                            continue reloop;
                        }
                    }
                }
            }
            break;
        }

        List<MetadataValue> mvs = item.getMetadata();
        List<MetadataValue> mvsToDelete = new ArrayList<>();
        mv:
        for (MetadataValue mv : mvs) {
            String f = mv.getMetadataField().toString('.');
            if (oldValues.containsKey(f)) {
                for (String oldValue : oldValues.get(f)) {
                    if (oldValue.equals(mv.getValue())) {
                        mvsToDelete.add(mv);
                        continue mv;
                    }
                }
            }
        }

        try {
            for (String f : newValues.keySet()) {
                for (String value : newValues.get(f)) {
                    itemService.addMetadata(context, item, metadatafieldService.findByString(context, f, '.'),
                            null, value);
                }
            }
            itemService.removeMetadataValues(context, item, mvsToDelete);
        } catch (SQLException e) {
            // deliberately left empty. Order ensures we do as little harm as possible.
        }
    }
}
