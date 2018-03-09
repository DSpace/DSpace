/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.util;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dspace.submit.lookup.SubmissionLookupDataLoader;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class SubmissionLookupPublication implements MutableRecord, Serializable
{
    private String providerName;

    private Map<String, List<String>> storage = new HashMap<String, List<String>>();

    public SubmissionLookupPublication(String providerName)
    {
        this.providerName = providerName;
    }

    // needed to serialize it with JSON
    public Map<String, List<String>> getStorage()
    {
        return storage;
    }

    @Override
    public Set<String> getFields()
    {
        return storage.keySet();
    }

    public List<String> remove(String md)
    {
        return storage.remove(md);
    }

    public void add(String md, String nValue)
    {
        if (StringUtils.isNotBlank(nValue))
        {
            List<String> tmp = storage.get(md);
            if (tmp == null)
            {
                tmp = new ArrayList<String>();
                storage.put(md, tmp);
            }
            tmp.add(nValue);
        }
    }

    public String getFirstValue(String md)
    {
        List<String> tmp = storage.get(md);
        if (tmp == null || tmp.size() == 0)
        {
            return null;
        }
        return tmp.get(0);
    }

    public String getProviderName()
    {
        return providerName;
    }

    public String getType()
    {
        return getFirstValue(SubmissionLookupDataLoader.TYPE);
    }

    // BTE Record interface methods
    @Override
    public boolean hasField(String md)
    {
        return storage.containsKey(md);
    }

    @Override
    public List<Value> getValues(String md)
    {
        List<String> stringValues = storage.get(md);
        if (stringValues == null)
        {
            return null;
        }
        List<Value> values = new ArrayList<Value>();
        for (String value : stringValues)
        {
            values.add(new StringValue(value));
        }
        return values;
    }

    @Override
    public boolean isMutable()
    {
        return true;
    }

    @Override
    public MutableRecord makeMutable()
    {
        return this;
    }

    @Override
    public boolean addField(String md, List<Value> values)
    {
        if (storage.containsKey(md))
        {
            List<String> stringValues = storage.get(md);
            if (values != null)
            {
                for (Value value : values)
                {
                    stringValues.add(value.getAsString());
                }
            }
        }
        else
        {
            List<String> tmp = new ArrayList<String>();
            if (values != null)
            {
                for (Value value : values)
                {
                    tmp.add(value.getAsString());
                }
            }
            storage.put(md, tmp);
        }

        return true;
    }

    @Override
    public boolean addValue(String md, Value value)
    {
        if (storage.containsKey(md))
        {
            List<String> stringValues = storage.get(md);
            stringValues.add(value.getAsString());
        }
        else
        {
            List<String> tmp = new ArrayList<String>();
            tmp.add(value.getAsString());

            storage.put(md, tmp);
        }

        return true;
    }

    @Override
    public boolean removeField(String md)
    {
        if (storage.containsKey(md))
        {
            storage.remove(md);
        }
        return false;
    }

    @Override
    public boolean removeValue(String md, Value value)
    {
        if (storage.containsKey(md))
        {
            List<String> stringValues = storage.get(md);
            stringValues.remove(value.getAsString());
        }
        return true;
    }

    @Override
    public boolean updateField(String md, List<Value> values)
    {
        List<String> stringValues = new ArrayList<String>();
        for (Value value : values)
        {
            stringValues.add(value.getAsString());
        }
        storage.put(md, stringValues);

        return true;
    }

    @Override
    public boolean updateValue(String md, Value valueOld, Value valueNew)
    {
        if (storage.containsKey(md))
        {
            List<String> stringValues = storage.get(md);
            List<String> newStringValues = storage.get(md);
            for (String s : stringValues)
            {
                if (s.equals(valueOld.getAsString()))
                {
                    newStringValues.add(valueNew.getAsString());
                }
                else
                {
                    newStringValues.add(s);
                }
            }
            storage.put(md, newStringValues);
        }
        return true;
    }
}
