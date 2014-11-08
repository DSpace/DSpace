/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.dspace.services.ConfigurationService;

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

	protected String mappingFile; //The properties absolute filename
	
    protected String converterNameFile; //The properties filename

    protected ConfigurationService configurationService;
	
    protected Map<String, String> mapping;

    protected String defaultValue = "";

    protected List<String> fieldKeys;

    protected Map<String, String> regexConfig = new HashMap<String, String>();

    public final String REGEX_PREFIX = "regex.";

    public void init() {
        this.mappingFile = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator + "crosswalks" + File.separator + converterNameFile;
        
        this.mapping = new HashMap<String, String>();
        
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(new File(mappingFile));
            Properties mapConfig = new Properties();
            mapConfig.load(fis);
            fis.close();
            for (Object key : mapConfig.keySet())
            {
                String keyS = (String)key;
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
                    regexConfig.put(regex,regReplace);
                }
                if (mapConfig.getProperty(keyS) != null)
                    mapping.put(keyS, mapConfig.getProperty(keyS));
                else 
                    mapping.put(keyS, "");
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("", e);
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                }
                catch (IOException ioe)
                {
                    // ...
                }
            }
        }
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
                regexConfig.put(regex,regReplace);
            }
        }
    }
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


    public void setFieldKeys(List<String> fieldKeys)
    {
        this.fieldKeys = fieldKeys;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }
    
    public void setConverterNameFile(String converterNameFile)
    {
        this.converterNameFile = converterNameFile;
    }
    public void setConfigurationService(ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }
}
