/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import java.util.List;
import java.util.Properties;
import org.apache.commons.configuration.Configuration;


/**
 * This service handles retrieval of the configuration data for a DSpace 
 * instance.
 * <p>
 * The configuration files are properties files which look like this for
 * simple values:
 * {@code
 * thing.name = aaronz
 * thing.number = 1234
 * thing.on = true
 * thing.value = abc,def,ghi
 * }
 * For these simple cases the service will automatically translate the 
 * settings into strings, booleans, numbers and arrays as requested
 * in the various {@link #getPropertyAsType(String, Class)} methods.
 * <p>
 * There are special case configuration parameters allowed as well.
 * <p>
 * The first allows setting of a parameter on any DSpace service by the 
 * given name:
 * {@code
 * emailEnabled@org.dspace.Service = true
 * adminUser@org.dspace.impl.MyService = aaronz
 * }
 * This should be used sparingly and really only by system admins (not 
 * developers).  Developers should be using simple config values to 
 * expose service configurations.
 * <p>
 * The second allows controlling the implementation used for a service 
 * interface or provider:
 * {@code
 * $org.dspace.Service = org.dspace.impl.MyService
 * }
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ConfigurationService {

    /**
     * Get a configuration property (setting) from the system as a 
     * specified type.
     * 
     * @param <T> class type
     * @param name the property name
     * @param type the type to return the property as
     * @return the property value OR null if none is found
     * @throws UnsupportedOperationException if the type cannot be converted to the requested type
     */
    public <T> T getPropertyAsType(String name, Class<T> type);

    /**
     * Get a configuration property (setting) from the system, or return
     * a default value if none is found.
     * 
     * @param <T> class type
     * @param name the property name
     * @param defaultValue the value to return if this name is not found
     * @return the property value OR null if none is found
     * @throws IllegalArgumentException if the defaultValue type does not match the type of the property by name
     */
    public <T> T getPropertyAsType(String name, T defaultValue);

    /**
     * Get a configuration property (setting) from the system, or return 
     * (and possibly store) a default value if none is found.
     * 
     * @param <T> class type
     * @param name the property name
     * @param defaultValue the value to return if this name is not found
     * @param setDefaultIfNotFound if this is true and the config value 
     * is not found then the default value will be set in the 
     * configuration store assuming it is not null.  Otherwise the
     * default value is just returned but not set.
     * @return the property value OR null if none is found
     * @throws IllegalArgumentException if the defaultValue type does not match the type of the property by name
     */
    public <T> T getPropertyAsType(String name, T defaultValue, boolean setDefaultIfNotFound);

    /**
     * Get keys all currently known configuration settings
     * 
     * @return all the configuration keys as a List
     */
    public List<String> getPropertyKeys();

    /**
     * Get keys all currently known configuration settings, which
     * begin with a given prefix.
     * <P>
     * For example, passing in "db" would return the keys "db.url", "db.username", etc.
     *
     * @param prefix prefix of key
     * @return all the configuration keys as a List
     */
    public List<String> getPropertyKeys(String prefix);

    /**
     * Convenience method - get a configuration property (setting) from 
     * the system as a String.
     * 
     * @param name the property name
     * @return the property value OR null if none is found
     */
    public String getProperty(String name);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as a String.
     *
     * @param name the property name
     * @param defaultValue default value if property not found
     * @return the property value OR default value if not found
     */
    public String getProperty(String name, String defaultValue);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as a String Array.
     *
     * @param name the property name
     * @return the String Array value
     */
    public String[] getArrayProperty(String name);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as a String Array.
     *
     * @param name the property name
     * @param defaultValue the default value if property not found
     * @return the String Array value or default value if not found
     */
    public String[] getArrayProperty(String name, String[] defaultValue);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as a boolean value.
     *
     * @param name the property name
     * @return the boolean property value (true/false)
     */
    public boolean getBooleanProperty(String name);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as a boolean value.
     *
     * @param name the property name
     * @param defaultValue the default value if property not found
     * @return the boolean property value (true/false) or default value if not found
     */
    public boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as an int value.
     *
     * @param name the property name
     * @return the integer property value
     */
    public int getIntProperty(String name);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as an int value.
     *
     * @param name the property name
     * @param defaultValue the default value if property not found
     * @return the integer property value or default value if not found
     */
    public int getIntProperty(String name, int defaultValue);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as a long value.
     *
     * @param name the property name
     * @return the long property value
     */
    public long getLongProperty(String name);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as a long value.
     *
     * @param name the property name
     * @param defaultValue the default value if property not found
     * @return the long property value or default value if not found
     */
    public long getLongProperty(String name, long defaultValue);

    /**
     * Convenience method - get a configuration property (setting) from
     * the system as its stored object
     * 
     * @param name the property name
     * @return the property value OR null if none is found
     */
    public Object getPropertyValue(String name);

    /**
     * Convenience method - get all configuration properties (settings)
     * from the system.
     *
     * @return all the configuration properties in a properties object (name to value)
     */
    public Properties getProperties();

    /**
     * Convenience method - get entire configuration (settings)
     * from the system.
     * 
     * @return Configuration object representing the system configuration
     */
    public Configuration getConfiguration();

    /**
     * Return whether a property exists within the configuration
     *
     * @param name the property name
     * @return true if property exists, false if not
     */
    public boolean hasProperty(String name);

    /**
     * Set a configuration property (setting) in the system.
     * Type is not important here since conversion happens automatically
     * when properties are requested.
     * 
     * @param name the property name 
     * @param value the property value (set this to null to clear out the property)
     * @return true if the property is new or changed from the existing value, false if it is the same
     * @throws IllegalArgumentException if the name is null
     * @throws UnsupportedOperationException if the type cannot be converted to something that is understandable by the system as a configuration property value
     */
    public boolean setProperty(String name, Object value);

    /**
     * Reload the configuration from the DSpace configuration files.
     * <P>
     * Uses the initialized ConfigurationService to reload all configurations.
     */
    public void reloadConfig();

}
