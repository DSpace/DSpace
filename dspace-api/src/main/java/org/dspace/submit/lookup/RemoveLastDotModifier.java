/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import gr.ekt.bte.core.AbstractModifier;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class RemoveLastDotModifier extends AbstractModifier
{

    List<String> fieldKeys;

    /**
     * @param name
     *     modifier name
     */
    public RemoveLastDotModifier(String name)
    {
        super(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gr.ekt.bte.core.AbstractModifier#modify(gr.ekt.bte.core.MutableRecord)
     */
    @Override
    public Record modify(MutableRecord record)
    {
        if (fieldKeys != null)
        {
            for (String key : fieldKeys)
            {
                List<Value> values = record.getValues(key);

                List<Value> newValues = new ArrayList<Value>();

                if (values != null)
                {
                    for (Value value : values)
                    {
                        String valueString = value.getAsString();
                        if (StringUtils.isNotBlank(valueString)
                                && valueString.endsWith("."))
                        {
                            newValues.add(new StringValue(valueString
                                    .substring(0, valueString.length() - 1)));
                        }
                        else
                        {
                            newValues.add(new StringValue(valueString));
                        }
                    }

                    record.updateField(key, newValues);
                }
            }
        }

        return record;
    }

    public void setFieldKeys(List<String> fieldKeys)
    {
        this.fieldKeys = fieldKeys;
    }
}
