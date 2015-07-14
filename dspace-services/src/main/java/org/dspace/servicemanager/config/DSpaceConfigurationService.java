/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;

/**
 * The central DSpace configuration service. Uses Apache Commons Configuration
 * to provide the ability to reload Property files.
 *
 * @author Tim Donohue (rewrote to use Apache Commons Config
 * @author Aaron Zeckoski
 * @author Kevin Van de Velde
 * @author Mark Diggory
 */
public final class DSpaceConfigurationService implements ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(DSpaceConfigurationService.class);

    public static final String DSPACE_WEB_CONTEXT_PARAM = "dspace-config";
    public static final String DSPACE = "dspace";
    public static final String EXT_CONFIG = "cfg";
    public static final String DOT_CONFIG = "." + EXT_CONFIG;

    public static final String DSPACE_HOME = DSPACE + ".dir";
    public static final String DEFAULT_CONFIG_DIR = "config";
    public static final String DEFAULT_CONFIG_DEFINITION_FILE = "config-definition.xml";
    public static final String DSPACE_CONFIG_DEFINITION_PATH = DEFAULT_CONFIG_DIR + File.separatorChar + DEFAULT_CONFIG_DEFINITION_FILE;

    public static final String DSPACE_CONFIG_PATH = DEFAULT_CONFIG_DIR + File.separatorChar + DSPACE + DOT_CONFIG;

    // Current ConfigurationBuilder
    DefaultConfigurationBuilder configurationBuilder = null;

    // Current Configuration
    protected Configuration configuration = null;

    public DSpaceConfigurationService() {
        // init and load up current config settings
        loadInitialConfig(null);
    }

    public DSpaceConfigurationService(String providedHome) {
		loadInitialConfig(providedHome);
	}

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getProperties()
     */
    @Override
    public Properties getProperties() {
        // Return our configuration as a set of Properties
        return ConfigurationConverter.getProperties(configuration);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getPropertyKeys()
     */
    @Override
    public List<String> getPropertyKeys() {

        Iterator<String> keys = configuration.getKeys();

        List<String> keyList = new ArrayList<>();
        while(keys.hasNext())
        {
            keyList.add(keys.next());
        }
        return keyList;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getPropertyKeys(java.lang.String)
     */
    @Override
    public List<String> getPropertyKeys(String prefix) {

        Iterator<String> keys = configuration.getKeys(prefix);

        List<String> keyList = new ArrayList<>();
        while(keys.hasNext())
        {
            keyList.add(keys.next());
        }
        return keyList;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getConfiguration()
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getPropertyValue(java.lang.Object)
     */
    @Override
    public Object getPropertyValue(String name) {
        return configuration.getProperty(name);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getProperty(java.lang.String)
     */
    @Override
    public String getProperty(String name) {
        return configuration.getString(name);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getPropertyAsType(java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T getPropertyAsType(String name, Class<T> type) {
        return convert(name, type);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getPropertyAsType(java.lang.String, java.lang.Object)
     */
    @Override
    public <T> T getPropertyAsType(String name, T defaultValue) {
        return getPropertyAsType(name, defaultValue, false);
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getPropertyAsType(java.lang.String, java.lang.Object, boolean)
     */
    @Override
    public <T> T getPropertyAsType(String name, T defaultValue, boolean setDefaultIfNotFound) {

        // If this key doesn't exist, immediately return a value
        if(!configuration.containsKey(name))
        {
            // if flag is set, save the default value as the new value for this property
            if(setDefaultIfNotFound)
            {
                setProperty(name, defaultValue);
            }
            
            // Either way, return our default value as if it was the setting
            return defaultValue;
        }

        // Get the class associated with our default value
        Class type = defaultValue.getClass();

        return (T) convert(name, type);
    }

    // config loading methods

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#setProperty(java.lang.String, java.lang.Object)
     */
    @Override
    public synchronized boolean setProperty(String name, Object value)
    {
        boolean changed = false;
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null for setting configuration");
        }
        else
        {
            Object oldValue = configuration.getProperty(name);

            if (value == null && oldValue!=null)
            {
                changed = true;
                configuration.clearProperty(name);
                log.info("Cleared the configuration setting for name ("+name+")");
            }
            else if(!value.equals(oldValue))
            {
               changed = true;
               configuration.setProperty(name, value);
            }
        }
        return changed;
    }

    /**
     * Load a series of properties into the configuration.
     * Checks to see if the settings exist or are changed and only loads
     * changes.
     * <P>
     * This only adds/updates configurations, if you wish to first clear all
     * existing configurations, see clear() method.
     *
     * @param properties a map of key -> value settings
     * @return the list of changed configuration keys
     */
    public String[] loadConfiguration(Map<String, Object> properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties cannot be null");
        }

        ArrayList<String> changed = new ArrayList<String>();

        // loop through each new property entry
        for (Entry<String, Object> entry : properties.entrySet())
        {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Load this new individual key
            boolean updated = loadConfig(key, value);

            // If it was updated, add to our list of changed settings
            if(updated)
            {
                changed.add(key);
            }
        }

        // Return an array of updated keys
        return changed.toArray(new String[changed.size()]);
    }

    /**
     * Loads a single additional config setting into the system.
     * @param key
     * @param value
     * @return true if the config is new or changed
     */
    public boolean loadConfig(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        // Check if the value has changed
        if (this.configuration.containsKey(key) &&
            this.configuration.getProperty(key).equals(value))
        {
            // no change to the value
            return false;
        }
        else
        {
            // Either this config doesn't exist, or it is not the same value,
            // so we'll update it.
            this.configuration.setProperty(key, value);
            return true;
        }
    }

    /**
     * Clears all the configuration settings.
     */
    public void clear() {
        this.configuration.clear();
        log.info("Cleared all configuration settings");
    }

    /**
     * Clears a single configuration
     * @param key key of the configuration
     */
    public void clearConfig(String key) {
        this.configuration.clearProperty(key);
    }

    // loading from files code

    /**
     * Loads up the configuration from the DSpace configuration files.
     * <P>
     * Determines the home directory of DSpace, and then loads the configurations
     * based on the configuration definition file in that location
     * (using Apache Commons Configuration).
     * @param providedHome DSpace home directory, or null.
     */
    public void loadInitialConfig(String providedHome)
    {
        // See if homePath is specified as a System Property
        String homePath = System.getProperty(DSPACE_HOME);

        // If a provided home was passed in & no system property, use provided home
        if (providedHome != null && homePath == null) {
            homePath = providedHome;
        }
        // If still null, check Catalina
        if (homePath == null) {
            String catalina = getCatalina();
            if (catalina != null) {
                homePath = catalina + File.separatorChar + DSPACE + File.separatorChar;
            }
        }
        // If still null, check "user.home" system property
        if (homePath == null) {
            homePath = System.getProperty("user.home");
        }
        // Finally, no other option but to assume root path ("/")
        if (homePath == null) {
            homePath = "/";
        }

        // Based on homePath get full path to the configuration definition
        String configDefinition = homePath + File.separatorChar + DSPACE_CONFIG_DEFINITION_PATH;

        try
        {
            // Load our configuration definition, which in turn loads all our config files/settings
            // See: http://commons.apache.org/proper/commons-configuration/userguide_v1.10/howto_configurationbuilder.html
            configurationBuilder = new DefaultConfigurationBuilder(configDefinition);

            // Actually parser our configuration definition & return the resulting Configuration
            configuration = configurationBuilder.getConfiguration();
        }
        catch(ConfigurationException ce)
        {
            log.error("Unable to load configurations based on definition at " + configDefinition);
            System.err.println("Unable to load configurations based on definition at " + configDefinition);
            throw new RuntimeException(ce);
        }

        log.info("Started up configuration service and loaded settings: " + toString());
    }

    /**
     * Reload the configuration from the DSpace configuration files.
     * <P>
     * Uses the initialized ConfigurationBuilder to reload all configurations.
     */
    @Override
    public void reloadConfig()
    {
        try
        {
            configurationBuilder.reload();
            this.configuration = configurationBuilder.getConfiguration();
        }
        catch(ConfigurationException ce)
        {
            log.error("Unable to reload configurations based on definition at " + configurationBuilder.getFile().getAbsolutePath(), ce);
        }
        log.info("Reloaded configuration service: " + toString());
    }

    @Override
    public String toString() {
        // Get the size of the generated Properties
        Properties props = getProperties();
        int size = props!=null ? props.size() : 0;

        // Return the configuration directory and number of configs loaded
        return "ConfigDir=" + configuration.getString(DSPACE_HOME) + File.separatorChar + DEFAULT_CONFIG_DIR + ", Size=" + size;
    }

    /**
     * This simply attempts to find the servlet container home for tomcat.
     * @return the path to the servlet container home OR null if it cannot be found
     */
    protected String getCatalina() {
        String catalina = System.getProperty("catalina.base");
        if (catalina == null) {
            catalina = System.getProperty("catalina.home");
        }
        return catalina;
    }

    /**
     * Convert the value of a given property to a specific object type.
     * <P>
     * Note: in most cases we can just use Configuration get*() methods.
     *
     * @param name Key of the property to convert
     * @param <T> object type
     * @return converted value
     */
    private <T> T convert(String name, Class<T> type) {

        // If this key doesn't exist, just return null
        if(!configuration.containsKey(name))
        {
            // Special case. For booleans, return false if key doesn't exist
            if(Boolean.class.equals(type) || boolean.class.equals(type))
                return (T) Boolean.FALSE;
            else
                return null;
        }

        // Based on the type of class, call the appropriate
        // method of the Configuration object
        if(type.isArray())
            return (T) configuration.getStringArray(name);
        else if(String.class.equals(type) || type.isAssignableFrom(String.class))
            return (T) configuration.getString(name);
        else if(BigDecimal.class.equals(type))
            return (T) configuration.getBigDecimal(name);
        else if(BigInteger.class.equals(type))
            return (T) configuration.getBigInteger(name);
        else if(Boolean.class.equals(type) || boolean.class.equals(type))
            return (T) Boolean.valueOf(configuration.getBoolean(name));
        else if(Byte.class.equals(type) || byte.class.equals(type))
            return (T) Byte.valueOf(configuration.getByte(name));
        else if(Double.class.equals(type) || double.class.equals(type))
            return (T) Double.valueOf(configuration.getDouble(name));
        else if(Float.class.equals(type) || float.class.equals(type))
            return (T) Float.valueOf(configuration.getFloat(name));
        else if(Integer.class.equals(type) || int.class.equals(type))
            return (T) Integer.valueOf(configuration.getInt(name));
        else if(List.class.equals(type))
            return (T) configuration.getList(name);
        else if(Long.class.equals(type) || long.class.equals(type))
            return (T) Long.valueOf(configuration.getLong(name));
        else if(Short.class.equals(type) || short.class.equals(type))
            return (T) Short.valueOf(configuration.getShort(name));
        else
        {
            // If none of the above works, try to convert the value to the required type
            SimpleTypeConverter converter = new SimpleTypeConverter();
            return (T) converter.convertIfNecessary(configuration.getProperty(name), type);
        }
    }
}
