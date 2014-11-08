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

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class FieldMergeModifier extends AbstractModifier
{
    protected Map<String, List<String>> mergeFieldMap;

    public FieldMergeModifier()
    {
        super("FieldMergeModifier");
    }

    @Override
    public Record modify(MutableRecord rec)
    {
        if (mergeFieldMap != null)
        {
            for (String target_field : mergeFieldMap.keySet())
            {
                List<String> source_fields = mergeFieldMap.get(target_field);
                for (String source_field : source_fields)
                {
                    List<Value> values = rec.getValues(source_field);
                    if (values != null && values.size() > 0)
                    {
                        for (Value value : values)
                        {
                            rec.addValue(target_field, value);
                        }
                    }
                    // rec.removeField(source_field);
                }
            }
        }
        return rec;
    }

    /**
     * @return the merge_field_map
     */
    public Map<String, List<String>> getMergeFieldMap()
    {
        return mergeFieldMap;
    }

    /**
     * @param merge_field_map
     *            the merge_field_map to set
     */
    public void setMergeFieldMap(Map<String, List<String>> merge_field_map)
    {
        this.mergeFieldMap = merge_field_map;
    }
}
