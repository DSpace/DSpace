/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.lookup;

import gr.ekt.bte.core.AbstractModifier;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.Value;

import java.util.List;
import java.util.Map;

public class FieldMergeModifier extends AbstractModifier {
    private Map<String, List<String>> merge_field_map;
    public FieldMergeModifier() {
        super("FieldMergeModifier");
    }

    @Override
    public Record modify(MutableRecord rec) {
        for (String target_field : merge_field_map.keySet()) {
            List<String> source_fields = merge_field_map.get(target_field);
            for (String source_field : source_fields) {
                List<Value> values = rec.getValues(source_field);
                for (Value value : values) {
                    rec.addValue(target_field, value);
                }
                rec.removeField(source_field);
            }
        }
        return rec;
    }

    /**
     * @return the merge_field_map
     */
    public Map<String, List<String>> getMergeFieldMap() {
        return merge_field_map;
    }

    /**
     * @param merge_field_map the merge_field_map to set
     */
    public void setMergeFieldMap(Map<String, List<String>> merge_field_map) {
        this.merge_field_map = merge_field_map;
    }
}

