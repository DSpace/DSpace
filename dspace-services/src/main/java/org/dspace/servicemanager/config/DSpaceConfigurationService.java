/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
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
import org.springframework.core.io.ClassPathResource;

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

    public static final String DSPACE = "dspace";
    public static final String EXT_CONFIG = "cfg";
    public static final String DOT_CONFIG = "." + EXT_CONFIG;

    public static final String DSPACE_HOME = DSPACE + ".dir";
    public static final String DEFAULT_CONFIG_DIR = "config";
    public static final String DEFAULT_CONFIG_DEFINITION_FILE = "config-definition.xml";
    public static final String DSPACE_CONFIG_DEFINITION_PATH = DEFAULT_CONFIG_DIR + File.separatorChar + DEFAULT_CONFIG_DEFINITION_FILE;

    public static final String DSPACE_CONFIG_PATH = DEFAULT_CONFIG_DIR + File.separatorChar + DSPACE + DOT_CONFIG;

    // The DSpace Server ID configuration
    public static final String DSPACE_SERVER_ID = "serverId";

    // Current ConfigurationBuilder
    private DefaultConfigurationBuilder configurationBuilder = null;

    // Current Configuration
    private Configuration configuration = null;

    // Current Home directory
    private String homePath = null;

    /**
     * Initializes a ConfigurationService based on default values. The DSpace
     * Home directory is determined based on system properties / searching.
     * <P>
     * See loadInitialConfig() for more details
     */
    public DSpaceConfigurationService() {
        // init and load up current config settings
        loadInitialConfig(null);
    }

    /**
     * Initializes a ConfigurationService based on the provided home directory
     * for DSpace
     * @param providedHome provided home directory
     */
    public DSpaceConfigurationService(String providedHome) {
		loadInitialConfig(providedHome);
	}

    /**
     * Returns all loaded properties as a Properties object.
     *
     * @see org.dspace.services.ConfigurationService#getProperties()
     */
    @Override
    public Properties getProperties() {
        // Return our configuration as a set of Properties
        return ConfigurationConverter.getProperties(configuration);
    }

    /**
     * Returns all Property keys.
     *
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

    /**
     * Returns all Property keys that begin with a given prefix.
     *
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

    /**
     * Returns all loaded properties as a Configuration object.
     *
     * @see org.dspace.services.ConfigurationService#getConfiguration()
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns property value as an Object.
     * If property is not found, null is returned.
     *
     * @see org.dspace.services.ConfigurationService#getPropertyValue(java.lang.String)
     */
    @Override
    public Object getPropertyValue(String name) {
        return configuration.getProperty(name);
    }

    /**
     * Returns property value as a String.
     * If property is not found, null is returned.
     *
     * @see org.dspace.services.ConfigurationService#getProperty(java.lang.String)
     */
    @Override
    public String getProperty(String name) {
        return getProperty(name, null);
    }

    /**
     * Returns property value as a String.
     * If property is not found, default value is returned.
     *
     * @see org.dspace.services.ConfigurationService#getProperty(java.lang.String, java.lang.String)
     */
    @Override
    public String getProperty(String name, String defaultValue) {
        return (String) getPropertyAsType(name, defaultValue);
    }

    /**
     * Returns property value as an array.
     * If property is not found, an empty array is returned.
     *
     * @see org.dspace.services.ConfigurationService#getArrayProperty(java.lang.String)
     */
    @Override
    public String[] getArrayProperty(String name) {
        return getArrayProperty(name, new String[0]);
    }

    /**
     * Returns property value as an array.
     * If property is not found, default value is returned.
     *
     * @see org.dspace.services.ConfigurationService#getArrayProperty(java.lang.String, java.lang.String[])
     */
    @Override
    public String[] getArrayProperty(String name, String[] defaultValue) {
        return getPropertyAsType(name, defaultValue);
    }

    /**
     * Returns property value as a boolean value.
     * If property is not found, false is returned.
     *
     * @see org.dspace.services.ConfigurationService#getBooleanProperty(java.lang.String)
     */
    @Override
    public boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    /**
     * Returns property value as a boolean value.
     * If property is not found, default value is returned.
     *
     * @see org.dspace.services.ConfigurationService#getBooleanProperty(java.lang.String, boolean)
     */
    @Override
    public boolean getBooleanProperty(String name, boolean defaultValue) {
        return getPropertyAsType(name, defaultValue);
    }

    /**
     * Returns property value as an int value.
     * If property is not found, 0 is returned.
     * <P>
     * If you wish to avoid the 0 return value, you can use
     * hasProperty() to first determine whether the property
     * exits. Or, use getIntProperty(name,defaultValue).
     *
     * @see org.dspace.services.ConfigurationService#getIntProperty(java.lang.String)
     */
    @Override
    public int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }

    /**
     * Returns property value as an int value.
     * If property is not found, default value is returned.
     *
     * @see org.dspace.services.ConfigurationService#getIntProperty(java.lang.String, int)
     */
    @Override
    public int getIntProperty(String name, int defaultValue) {
        return getPropertyAsType(name, defaultValue);
    }

    /**
     * Returns property value as a long value.
     * If property is not found, 0 is returned.
     * <P>
     * If you wish to avoid the 0 return value, you can use
     * hasProperty() to first determine whether the property
     * exits. Or, use getLongProperty(name,defaultValue).
     *
     * @see org.dspace.services.ConfigurationService#getLongProperty(java.lang.String)
     */
    @Override
    public long getLongProperty(String name) {
        return getLongProperty(name, 0);
    }

    /**
     * Returns property value as a long value.
     * If property is not found, default value is returned.
     *
     * @see org.dspace.services.ConfigurationService#getLongProperty(java.lang.String,long)
     */
    @Override
    public long getLongProperty(String name, long defaultValue) {
        return getPropertyAsType(name, defaultValue);
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
        if(!hasProperty(name))
        {
            // if flag is set, save the default value as the new value for this property
            if(setDefaultIfNotFound)
            {
                setProperty(name, defaultValue);
            }
            
            // Either way, return our default value as if it was the setting
            return defaultValue;
        }

        // Avoid NPE. If null defaultValue passed in, assume Object class
        Class type = Object.class;
        if(defaultValue!=null)
        {
            // Get the class associated with our default value
            type = defaultValue.getClass();
        }

        return (T) convert(name, type);
    }


    /**
     * Determine if a given property key exists within the currently loaded configuration
     * @see org.dspace.services.ConfigurationService#hasProperty(java.lang.String)
     */
    @Override
    public boolean hasProperty(String name)
    {
        if(configuration.containsKey(name))
            return true;
        else
            return false;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#setProperty(java.lang.String, java.lang.Object)
     */
    @Override
    public synchronized boolean setProperty(String name, Object value)
    {
        // If the value is a type of String, trim any leading/trailing spaces before saving it.
        if(value!=null && String.class.isInstance(value))
        {
            value = ((String) value).trim();
        }

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
            else if(value!=null && !value.equals(oldValue))
            {
               changed = true;
               configuration.setProperty(name, value);
            }
        }
        return changed;
    }

    /**
     * Load (i.e. Add) a series of properties into the configuration.
     * Checks to see if the settings exist or are changed and only loads
     * changes.
     * <P>
     * This only adds/updates configurations, if you wish to first clear all
     * existing configurations, see clear() method.
     *
     * @param properties a map of key to value settings
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
     * Loads (i.e. Adds) a single additional config setting into the system.
     * @param key configuration key to add
     * @param value configuration value to add
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
    private void loadInitialConfig(String providedHome)
    {
        // Determine the DSpace home directory
        homePath = getDSpaceHome(providedHome);

        // Based on homePath get full path to the configuration definition
        String configDefinition = homePath + File.separatorChar + DSPACE_CONFIG_DEFINITION_PATH;

        // Check if our configuration definition exists in the homePath
        File configDefFile = new File(configDefinition);
        if(!configDefFile.exists())
        {
            try
            {
                //If it doesn't exist, check for a configuration definition on Classpath
                // (NOTE: This is mostly for Unit Testing to find the test config-definition.xml)
                ClassPathResource resource = new ClassPathResource(DSPACE_CONFIG_DEFINITION_PATH);
                configDefinition = resource.getFile().getAbsolutePath();
            }
            catch(IOException ioe)
            {
                log.error("Error attempting to load configuration definition from classpath", ioe);
            }
        }

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

        // Finally, set any dynamic, default properties
        setDynamicProperties();

        log.info("Started up configuration service and loaded settings: " + toString());
    }

    /**
     * Reload the configuration from the DSpace configuration files.
     * <P>
     * Uses the initialized ConfigurationBuilder to reload all configurations.
     */
    @Override
    public synchronized void reloadConfig()
    {
        try
        {
            configurationBuilder.reload();
            this.configuration = configurationBuilder.getConfiguration();

            // Finally, (re)set any dynamic, default properties
            setDynamicProperties();
        }
        catch(ConfigurationException ce)
        {
            log.error("Unable to reload configurations based on definition at " + configurationBuilder.getFile().getAbsolutePath(), ce);
        }
        log.info("Reloaded configuration service: " + toString());
    }

     /**
     * Sets properties which are determined dynamically rather than
     * loaded via configuration.
     */
    private void setDynamicProperties()
    {
        // Ensure our DSPACE_HOME property is set to the determined homePath
        setProperty(DSPACE_HOME, homePath);

        try {
            // Attempt to set a default "serverId" property to value of hostname
            String defaultServerId = InetAddress.getLocalHost().getHostName();
            setProperty(DSPACE_SERVER_ID, defaultServerId);
        } catch (UnknownHostException e) {
            // oh well
        }
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
     * This attempts to find the DSpace home directory, based on system properties,
     * and the providedHome (if not null).
     * <p>The initial value of {@code dspace.dir} will be:</p>
     * <ol>
     *  <li>the value of the system property {@code dspace.dir} if defined;</li>
     *  <li>else the value of {@code providedHome} if not null;</li>
     *  <li>else the servlet container's home + "/dspace/" if defined (see {@link DSpaceConfigurationService#getCatalina()});</li>
     *  <li>else the user's home directory if defined;</li>
     *  <li>else "/".</li>
     * </ol>
     * @param providedHome provided home directory (may be null)
     * @return full path to DSpace home
     */
    protected String getDSpaceHome(String providedHome)
    {
        // See if valid home specified as system property (most trusted)
        String sysProperty = System.getProperty(DSPACE_HOME);
        if(isValidDSpaceHome(sysProperty))
        {
            return sysProperty;
        }

        // See if valid home passed in
        if(isValidDSpaceHome(providedHome))
        {
            return providedHome;
        }

        // If still not found, attempt to determine location of our JAR
        String pathRelativeToJar = null;
        try
        {
            // Check location of our running JAR
            URL jarLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
            // Convert to a file & get "grandparent" directory
            // This JAR should be running in [dspace]/lib/, so its parent is [dspace]/lib, and grandparent is [dspace]
            pathRelativeToJar = new File(jarLocation.toURI()).getParentFile().getParentFile().getAbsolutePath();
            // Is the grandparent directory of where the JAR resides a valid DSpace home?
            if(isValidDSpaceHome(pathRelativeToJar))
            {
                return pathRelativeToJar;
            }
        }
        catch(URISyntaxException e)
        { // do nothing
        }

        // If still not valid, check Catalina
        String catalina = getCatalina();
        if(isValidDSpaceHome(catalina))
        {
            return catalina;
        }

        // If still not valid, check "user.home" system property
        String userHome = System.getProperty("user.home");
        if(isValidDSpaceHome(userHome))
        {
            return userHome;
        }

        // Finally, try root path ("/")
        if (isValidDSpaceHome("/")) {
            return "/";
        }

        // If none of the above worked, DSpace Kernel will fail to start.
        throw new RuntimeException("DSpace home directory could not be determined. It MUST include a subpath of " +
                                   "'" + File.separatorChar + DSPACE_CONFIG_DEFINITION_PATH + "'. " +
                                   "Please consider setting the '" + DSPACE_HOME + "' system property or ensure the dspace-api.jar is being run from [dspace]/lib/.");
    }

    /**
     * Returns whether a given path seems to have the required DSpace configurations
     * in order to make it a valid DSpace home directory
     * @param path path to validate
     * @return true if path seems valid, false otherwise
     */
    protected boolean isValidDSpaceHome(String path)
    {
        // If null path, return false immediately
        if(path==null)
            return false;

        // Based on path get full path to the configuration definition
        String configDefinition = path + File.separatorChar + DSPACE_CONFIG_DEFINITION_PATH;
        File configDefFile = new File(configDefinition);

        // Check if the required config exists
        if(configDefFile.exists())
            return true;
        else
            return false;
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
