/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.Properties;
import mockit.Mock;
import mockit.MockUp;

/**
 * Like {@link ConfigurationManager} except that we can set properties
 * programmatically.  This class does not load any properties from files.
 *
 * @author mwood
 */
public class MockConfigurationManager
        extends MockUp<ConfigurationManager>
{
    private static final Properties props = new Properties();

    @Mock
    public static String getProperty(String key)
    {
        return props.getProperty(key);
    }

    public static void setProperty(String key, String value)
    {
        props.put(key, value);
    }
}
