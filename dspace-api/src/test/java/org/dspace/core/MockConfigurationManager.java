/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.Properties;
import mockit.Deencapsulation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Like {@link ConfigurationManager} except that we can set properties
 * programmatically.
 * <P>
 * Based on the boolean value passed to the constructor, this Mock class
 * can either initialize itself based on dspace.cfg, or initialize to
 * an empty property set.
 * <P>
 * In the situation where you initialize this Mock class via dspace.cfg,
 * you can still overwrite any default values by simply using the
 * "setProperty()" method.
 *
 * @author mwood
 * @author tdonohue
 */
public class MockConfigurationManager
        extends MockUp<ConfigurationManager>
{
    private static Properties props = new Properties();
    
    /**
     * Initialize Mock object by either loading all properties or starting
     * with an empty property set.
     * @param loadProps whether or not to initialize by loading dspace.cfg properties
     */
    public MockConfigurationManager(boolean loadProps)
    {
        if(loadProps)
        {
            // Call ConfigurationManager.getProperties() to initialize by 
            // reading all properties from configured dspace.cfg file
            props = Deencapsulation.invoke(ConfigurationManager.class, "getProperties");
        }
        else
        {
            props = new Properties();
        }
    }
    
    
    @Mock
    public static String getProperty(String key)
    {
        String value = props.getProperty(key);
        return (value != null) ? value.trim() : null;
    }

    public static void setProperty(String key, String value)
    {
        props.put(key, value);
    }
}
