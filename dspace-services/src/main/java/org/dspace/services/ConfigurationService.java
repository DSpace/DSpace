/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import java.util.Map;
import java.util.Properties;


/**
 * This service handles retrieval of the configuration data for a DSpace 
 * instance.
 * <p>
 * The configuration files are properties files which look like this for
 * simple values:
 * <xmp>
 * thing.name = aaronz
 * thing.number = 1234
 * thing.on = true
 * thing.value = abc,def,ghi
 * </xmp>
 * For these simple cases the service will automatically translate the 
 * settings into strings, booleans, numbers and arrays as requested
 * in the various {@link #getPropertyAsType(String, Class)} methods.
 * <p>
 * There are special case configuration parameters allowed as well.
 * <p>
 * The first allows setting of a parameter on any DSpace service by the 
 * given name:
 * <xmp>
 * emailEnabled@org.dspace.Service = true
 * adminUser@org.dspace.impl.MyService = aaronz
 * </xmp>
 * This should be used sparingly and really only by system admins (not 
 * developers).  Developers should be using simple config values to 
 * expose service configurations.
 * <p>
 * The second allows controlling the implementation used for a service 
 * interface or provider:
 * <xmp>
 * $org.dspace.Service = org.dspace.impl.MyService
 * </xmp>
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface ConfigurationService {

    /**
     * Get a configuration property (setting) from the system as a 
     * specified type.
     * 
     * @param <T>
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
     * @param <T>
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
     * @param <T>
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
     * Get all currently known configuration settings
     * 
     * @return all the configuration properties as a map of name -> value
     */
    public Map<String, String> getAllProperties();

    /**
     * Convenience method - get a configuration property (setting) from 
     * the system.
     * 
     * @param name the property name
     * @return the property value OR null if none is found
     */
    public String getProperty(String name);

    /**
     * Convenience method - get all configuration properties (settings)
     * from the system.
     * 
     * @return all the configuration properties in a properties object (name -> value)
     */
    public Properties getProperties();

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

}
