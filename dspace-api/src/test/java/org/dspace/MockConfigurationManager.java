/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace;

import java.util.Properties;
import mockit.Mock;
import mockit.MockClass;
import org.dspace.core.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mwood
 */
@MockClass(realClass=ConfigurationManager.class)
public class MockConfigurationManager {
    private static final Properties properties = new Properties();
    private static final Logger log = LoggerFactory.getLogger(MockConfigurationManager.class);

    public static void setProperty(String key, String value)
    {
        log.info("setProperty({}, {});", key, value);
        properties.setProperty(key, value);
    }

    @Mock
    public static String getProperty(String key)
    {
        log.info("getProperty({});", key);
        return properties.getProperty(key);
    }
}
