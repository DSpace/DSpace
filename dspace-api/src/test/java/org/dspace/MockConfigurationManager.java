/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace;

import java.util.Properties;
import mockit.Mock;
import mockit.MockUp;
import org.dspace.core.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy ConfigurationManager with a setter instead of external storage for
 * values.  Call {@link setProperty} to create configuration.
 *
 * <p>Please note that this implementation is incomplete!</p>
 *
 * @author mwood
 */
public class MockConfigurationManager
        extends MockUp<ConfigurationManager>
{
    private static final Properties properties = new Properties();
    private static final Logger log = LoggerFactory.getLogger(MockConfigurationManager.class);

    /**
     * Set a value in the configuration map.
     *
     * @param key name of the configuration datum.
     * @param value value to be assigned to the name.
     */
    public static void setProperty(String key, String value)
    {
        log.info("setProperty({}, {});", key, value);
        properties.setProperty(key, value);
    }

    /**
     * Fetch a value from the map.
     *
     * @param key name of the configuration property desired.
     * @return value bound to that name, or null if not set.
     */
    @Mock
    public static String getProperty(String key)
    {
        log.info("getProperty({});", key);
        return properties.getProperty(key);
    }
}
