/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.SelfNamedPlugin;

public class MapConverter extends SelfNamedPlugin implements IConverter
{
    /** Location of config file */
    private final String configFilePath = ConfigurationManager
            .getProperty("dspace.dir")
            + File.separator
            + "config"
            + File.separator
            + "crosswalks"
            + File.separator;

    public final String REGEX_PREFIX = "regex.";
    
    private Properties mapConfig;
    private Map<String, String> regexConfig = new HashMap<String, String>();

    private synchronized void init()
    {
        if (mapConfig != null)
            return;
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(configFilePath + "mapConverter-"
                    + getPluginInstanceName() + ".properties");
            mapConfig = new Properties();
            mapConfig.load(fis);
            fis.close();
            for (Object key : mapConfig.keySet())
            {
                String keyS = (String)key;
                if (keyS.startsWith(REGEX_PREFIX))
                {
                    String regex = keyS.substring(REGEX_PREFIX.length());
                    String regReplace = mapConfig.getProperty(keyS);
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
        catch (Exception e)
        {
            throw new IllegalArgumentException(
                    "Impossibile leggere la configurazione per il converter "
                            + getPluginInstanceName(), e);
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

    }

    public String makeConversion(String value)
    {
        if (value == null) return null;
        init();
        String tmp = "";
        if (mapConfig.containsKey(value))
        {
            tmp = mapConfig.getProperty(value, mapConfig
                    .getProperty("mapConverter.default"));    
        }
        else
        {
        	tmp = mapConfig.getProperty("mapConverter.default");
            for (String regex : regexConfig.keySet())
            {
                if (value != null && value.matches(regex))
                {
                    tmp = value.replaceAll(regex, regexConfig.get(regex));
                }
            }
        }
        
        if ("@@ident@@".equals(tmp))
        {
            return value;
        }
        else if (StringUtils.isNotBlank(tmp))
        {
            return tmp;
        }
        else
            return null;
    }

}
