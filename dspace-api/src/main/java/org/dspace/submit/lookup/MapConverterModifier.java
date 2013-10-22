/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class MapConverterModifier extends AbstractModifier
{

    String filename; // The properties filename

    Map<String, String> mapping;

    String defaultValue = "";

    List<String> fieldKeys;

    private Map<String, String> regexConfig = new HashMap<String, String>();

    public final String REGEX_PREFIX = "regex.";

    /**
     * @param name
     */
    public MapConverterModifier(String name)
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
        if (mapping != null && fieldKeys != null)
        {
            for (String key : fieldKeys)
            {
                List<Value> values = record.getValues(key);

                if (values == null)
                    continue;

                List<Value> newValues = new ArrayList<Value>();

                for (Value value : values)
                {
                    String stringValue = value.getAsString();

                    String tmp = "";
                    if (mapping.containsKey(stringValue))
                    {
                        tmp = mapping.get(stringValue);
                    }
                    else
                    {
                        tmp = defaultValue;
                        for (String regex : regexConfig.keySet())
                        {
                            if (stringValue != null
                                    && stringValue.matches(regex))
                            {
                                tmp = stringValue.replaceAll(regex,
                                        regexConfig.get(regex));
                            }
                        }
                    }

                    if ("@@ident@@".equals(tmp))
                    {
                        newValues.add(new StringValue(stringValue));
                    }
                    else if (StringUtils.isNotBlank(tmp))
                    {
                        newValues.add(new StringValue(tmp));
                    }
                    else
                        newValues.add(new StringValue(stringValue));
                }

                record.updateField(key, newValues);
            }
        }

        return record;
    }

    public void setMapping(Map<String, String> mapping)
    {
        this.mapping = mapping;

        for (String keyS : mapping.keySet())
        {
            if (keyS.startsWith(REGEX_PREFIX))
            {
                String regex = keyS.substring(REGEX_PREFIX.length());
                String regReplace = mapping.get(keyS);
                if (regReplace == null)
                {
                    regReplace = "";
                }
                else if (regReplace.equalsIgnoreCase("@ident@"))
                {
                    regReplace = "$0";
                }
                regexConfig.put(regex, regReplace);
            }
        }
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public void setFieldKeys(List<String> fieldKeys)
    {
        this.fieldKeys = fieldKeys;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }
}
