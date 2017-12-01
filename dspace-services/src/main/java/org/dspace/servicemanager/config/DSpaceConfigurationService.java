/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;

import org.dspace.constants.Constants;
import org.dspace.servicemanager.ServiceConfig;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The central DSpace configuration service.
 * This is effectively immutable once the config has loaded.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (mdiggory at atmire dot com)
 */
public final class DSpaceConfigurationService implements ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(DSpaceConfigurationService.class);

    public static final String DSPACE_WEB_CONTEXT_PARAM = "dspace-config";
    public static final String DSPACE = "dspace";
    public static final String EXT_CONFIG = "cfg";
    public static final String DOT_CONFIG = "." + EXT_CONFIG;

    public static final String DSPACE_PREFIX = "dspace.";
    public static final String DSPACE_HOME = DSPACE + ".dir";
    public static final String DEFAULT_CONFIGURATION_FILE_NAME = "dspace-defaults" + DOT_CONFIG;
    public static final String DEFAULT_DSPACE_CONFIG_PATH = "config/" + DEFAULT_CONFIGURATION_FILE_NAME;

    public static final String DSPACE_CONFIG_PATH = "config/" + DSPACE + DOT_CONFIG;

    public static final String DSPACE_MODULES_CONFIG_PATH = "config" + File.separator + "modules";

    protected transient Map<String, Map<String, ServiceConfig>> serviceNameConfigs;
    public static final String DSPACE_CONFIG_ADDON = "dspace/config-*";

    public DSpaceConfigurationService() {
        // init and load up current config settings
        loadInitialConfig(null);
    }

    public DSpaceConfigurationService(String providedHome) {
		loadInitialConfig(providedHome);
	}

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getAllProperties()
     */
    @Override
    public Map<String, String> getAllProperties() {
        Map<String, String> props = new LinkedHashMap<String, String>();
//        for (Entry<String, DSpaceConfig> config : configuration.entrySet()) {
//            props.put(config.getKey(), config.getValue().getValue());
//        }

        for (DSpaceConfig config : configuration.values()) {
            props.put(config.getKey(), config.getValue());
        }
        return props;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getProperties()
     */
    @Override
    public Properties getProperties() {
        Properties props = new Properties();
        for (DSpaceConfig config : configuration.values()) {
            props.put(config.getKey(), config.getValue());
        }
        return props;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getProperty(java.lang.String)
     */
    @Override
    public String getProperty(String name) {
        DSpaceConfig config = configuration.get(name);
        String value = null;
        if (config != null) {
            value = config.getValue();
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#getPropertyAsType(java.lang.String, java.lang.Class)
     */
    @Override
    public <T> T getPropertyAsType(String name, Class<T> type) {
        String value = getProperty(name);
        return convert(value, type);
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
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPropertyAsType(String name, T defaultValue, boolean setDefaultIfNotFound) {
        String value = getProperty(name);
        T property = null;
        if (defaultValue == null) {
            property = null; // just return null when default value is null
        } else if (value == null) {
            property = defaultValue; // just return the default value if nothing is currently set
            // also set the default value as the current stored value
            if (setDefaultIfNotFound) {
                setProperty(name, defaultValue);
            }
        } else {
            // something is already set so we convert the stored value to match the type
            property = (T)convert(value, defaultValue.getClass());
        }
        return property;
    }

    // config loading methods

    /* (non-Javadoc)
     * @see org.dspace.services.ConfigurationService#setProperty(java.lang.String, java.lang.Object)
     */
    @Override
    public boolean setProperty(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null for setting configuration");
        }
        boolean changed = false;
        if (value == null) {
            changed = this.configuration.remove(name) != null;
            log.info("Cleared the configuration setting for name ("+name+")");
        } else {
            SimpleTypeConverter converter = new SimpleTypeConverter();
            String sVal = (String)converter.convertIfNecessary(value, String.class);
            changed = loadConfig(name, sVal);
        }
        return changed;
    }

    // INTERNAL loading methods
    public List<DSpaceConfig> getConfiguration() {
        return new ArrayList<DSpaceConfig>( configuration.values() );
    }

    /**
     * Get all configs that start with the given value.
     * @param prefix a string which the configs to return must start with
     * @return the list of all configs that start with the given string
     */
    public List<DSpaceConfig> getConfigsByPrefix(String prefix) {
        List<DSpaceConfig> configs = new ArrayList<DSpaceConfig>();
        if (prefix != null && prefix.length() > 0) {
            for (DSpaceConfig config : configuration.values()) {
                if (config.getKey().startsWith(prefix)) {
                    configs.add(config);
                }
            }
        }
        return configs;
    }

    protected Map<String, DSpaceConfig> configuration = Collections.synchronizedMap(new LinkedHashMap<String, DSpaceConfig>());

    /**
     * @return a map of the service name configurations that are known for fast resolution
     */
    public Map<String, Map<String, ServiceConfig>> getServiceNameConfigs() {
        return serviceNameConfigs;
    }

    public void setConfiguration(Map<String, DSpaceConfig> configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        this.configuration = configuration;
        replaceVariables(this.configuration);
        // refresh the configs
        serviceNameConfigs = makeServiceNameConfigs();
    }

    /**
     * Load a series of properties into the configuration.
     * Checks to see if the settings exist or are changed and only loads
     * changes.  Clears out existing ones depending on the setting.
     *
     * @param properties a map of key -> value strings
     * @param clear if true then clears the existing configuration settings first
     * @return the list of changed configuration names
     */
    public String[] loadConfiguration(Map<String, String> properties, boolean clear) {
        if (properties == null) {
            throw new IllegalArgumentException("properties cannot be null");
        }
        // transform to configs and call load
        ArrayList<DSpaceConfig> dspaceConfigs = new ArrayList<DSpaceConfig>();
        for (Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key != null &&  ! "".equals(key)) {
                String val = entry.getValue();
                if (val != null &&  ! "".equals(val)) {
                    dspaceConfigs.add( new DSpaceConfig(entry.getKey(), entry.getValue()) );
                }
            }
        }
        return loadConfiguration(dspaceConfigs, clear);
    }

    /**
     * Load up a bunch of {@link DSpaceConfig}s into the configuration.
     * Checks to see if the settings exist or are changed and only
     * loads changes.  Clears out existing ones depending on the setting.
     *
     * @param dspaceConfigs a list of {@link DSpaceConfig} objects
     * @param clear if true then clears the existing configuration settings first
     * @return the list of changed configuration names
     */
    public String[] loadConfiguration(List<DSpaceConfig> dspaceConfigs, boolean clear) {
        ArrayList<String> changed = new ArrayList<String>();
        if (clear) {
            this.configuration.clear();
        }
        for (DSpaceConfig config : dspaceConfigs) {
            String key = config.getKey();
            boolean same = true;
            if (clear) {
                // all are new
                same = false;
            } else {
                if (this.configuration.containsKey(key)) {
                    if (this.configuration.get(key).equals(config)) {
                        // this one has changed
                        same = false;
                    }
                } else {
                    // this one is new
                    same = false;
                }
            }
            if (!same) {
                changed.add(key);
                this.configuration.put(key, config);
            }
        }
        if (changed.size() > 0) {
            replaceVariables(this.configuration);
            // refresh the configs
            serviceNameConfigs = makeServiceNameConfigs();
        }
        return changed.toArray(new String[changed.size()]);
    }

    /**
     * Loads an additional config setting into the system.
     * @param key
     * @param value
     * @return true if the config is new or changed
     */
    public boolean loadConfig(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        // update replacements and add
        boolean changed = replaceAndAddConfig( new DSpaceConfig(key, value) );
        if (changed) {
            // refresh the configs
            serviceNameConfigs = makeServiceNameConfigs();
        }
        return changed;
    }

    /**
     * Clears the configuration settings.
     */
    public void clear() {
        this.configuration.clear();
        this.serviceNameConfigs.clear();
        log.info("Cleared all configuration settings");
    }

    // loading from files code

    /**
     * Loads up the default initial configuration from the DSpace configuration
     * files in the file home and on the classpath.  Order:
     * <ol>
     *  <li>Create {@code serverId} from local host name if available.</li>
     *  <li>Create {@code dspace.testing = false}.
     *  <li>Determine the value of {@code dspace.dir} and add to configuration.</li>
     *  <li>Load {@code classpath:config/dspace_defaults.cfg}.</li>
     *  <li>Copy system properties with names beginning "dspace." <em>except</em>
     *      {@code dspace.dir}, removing the "dspace." prefix from the name.</li>
     *  <li>Load all {@code classpath:dspace/config-*.cfg} using whatever
     *      matched "*" as module prefix.</li>
     *  <li>Load all {@code ${dspace.dir}/config/modules/*.cfg} using whatever
     *      matched "*" as module prefix.</li>
     *  <li>Load {@code classpath:dspace.cfg}.</li>
     *  <li>Load from the path in the system property {@code dspace.configuration}
     *      if defined, or {@code ${dspace.dir}/config/dspace.cfg}.</li>
     *  <li>Perform variable substitutions throughout the assembled configuration.</li>
     * </ol>
     *
     * <p>The initial value of {@code dspace.dir} will be:</p>
     * <ol>
     *  <li>the value of the system property {@code dspace.dir} if defined;</li>
     *  <li>else the value of {@code providedHome} if not null;</li>
     *  <li>else the servlet container's home + "/dspace/" if defined (see {@link getCatalina()});</li>
     *  <li>else the user's home directory if defined;</li>
     *  <li>else "/".
     * </ol>
     *
     * @param providedHome DSpace home directory, or null.
     */
    public void loadInitialConfig(String providedHome) {
        Map<String, String> configMap = new LinkedHashMap<String, String>();
        // load default settings
        try {
            String defaultServerId = InetAddress.getLocalHost().getHostName();
            configMap.put("serverId", defaultServerId);
        } catch (UnknownHostException e) {
            // oh well
        }
        // default is testing mode off
        configMap.put(Constants.DSPACE_TESTING_MODE, "false");

        // now we load the settings from properties files
        String homePath = System.getProperty(DSPACE_HOME);

        // now we load from the provided parameter if its not null
        if (providedHome != null && homePath == null) {
            homePath = providedHome;
        }

        if (homePath == null) {
            String catalina = getCatalina();
            if (catalina != null) {
                homePath = catalina + File.separatorChar + DSPACE + File.separatorChar;
            }
        }
        if (homePath == null) {
            homePath = System.getProperty("user.home");
        }
        if (homePath == null) {
            homePath = "/";
        }

        // make sure it's set properly
        //System.setProperty(DSPACE_HOME, homePath);
        configMap.put(DSPACE_HOME, homePath);

        // LOAD the internal defaults
        Properties defaultProps = readPropertyResource(DEFAULT_DSPACE_CONFIG_PATH);
        if (defaultProps.size() <= 0) {
            // failed to load defaults!
            throw new RuntimeException("Failed to load default dspace config properties: " + DEFAULT_DSPACE_CONFIG_PATH);
        }
        pushPropsToMap(configMap, defaultProps);

        // load all properties from the system which begin with the prefix
        Properties systemProps = System.getProperties();
        for (Object o : systemProps.keySet()) {
            String key = (String) o;
            if (key != null
                    && ! key.equals(DSPACE_HOME)) {
                try {
                    if (key.startsWith(DSPACE_PREFIX)) {
                        String propName = key.substring(DSPACE_PREFIX.length());
                        String propVal = systemProps.getProperty(key);
                        log.info("Loading system property as config: "+propName+"=>"+propVal);
                        configMap.put(propName, propVal);
                    }
                } catch (RuntimeException e) {
                    log.error("Failed to properly get config value from system property: " + o, e);
                }
            }
        }

        // Collect values from all the properties files: the later ones loaded override settings from prior.


        //Find any addon config files found in the config dir in our jars
        try {
            PathMatchingResourcePatternResolver patchMatcher = new PathMatchingResourcePatternResolver();
            Resource[] resources = patchMatcher.getResources("classpath*:" + DSPACE_CONFIG_ADDON + DOT_CONFIG);
            for (Resource resource : resources) {
                String prefix = resource.getFilename().substring(0, resource.getFilename().lastIndexOf(".")).replaceFirst("config-", "");
                pushPropsToMap(configMap, prefix, readPropertyStream(resource.getInputStream()));
            }
        }catch (Exception e){
            log.error("Failed to retrieve properties from classpath: " + e.getMessage(), e);

        }
        //Attempt to load up all the config files in the modules directory
        try{
            File modulesDirectory = new File(homePath + File.separator + DSPACE_MODULES_CONFIG_PATH + File.separator);
            if(modulesDirectory.exists()){
                try{
                    Resource[] resources = new PathMatchingResourcePatternResolver().getResources(modulesDirectory.toURI().toURL().toString() + "*" + DOT_CONFIG);
                    if(resources != null){
                        for(Resource resource : resources){
                            String prefix = resource.getFilename().substring(0, resource.getFilename().lastIndexOf("."));
                            pushPropsToMap(configMap, prefix, readPropertyStream(resource.getInputStream()));
                        }
                    }
                }catch (IOException e){
                    log.error("Error while loading the modules properties from:" + modulesDirectory.getAbsolutePath());
                }
            }else{
                log.info("Failed to load the modules properties since (" + homePath + File.separator + DSPACE_MODULES_CONFIG_PATH + "): Does not exist");
            }

        }catch (IllegalArgumentException e){
            //This happens if we don't have a modules directory
            log.error("Error while loading the module properties since (" +  homePath + File.separator + DSPACE_MODULES_CONFIG_PATH + "): is not a valid directory", e);
        }

        // attempt to load from the current classloader also (works for commandline config sitting on classpath
        pushPropsToMap(configMap, readPropertyResource(DSPACE + DOT_CONFIG));

        // read all the known files from the home path that are properties files
        String configPath = System.getProperty("dspace.configuration");
        if (null == configPath)
        {
            configPath = homePath + File.separatorChar + DSPACE_CONFIG_PATH;
        }
        pushPropsToMap(configMap, readPropertyFile(configPath));

//      TODO: still use this local file loading?
//        pushPropsToMap(configMap, readPropertyFile(homePath + File.separatorChar + "local" + DOT_PROPERTIES));


//        pushPropsToMap(configMap, readPropertyResource(DSPACE + DOT_PROPERTIES));
//        pushPropsToMap(configMap, readPropertyResource("local" + DOT_PROPERTIES));
//        pushPropsToMap(configMap, readPropertyResource("webapp" + DOT_PROPERTIES));




        // now push all of these into the config service store
        loadConfiguration(configMap, true);
        log.info("Started up configuration service and loaded "+configMap.size()+" settings");
    }


    /**
     * Adds in this DSConfig and then updates the config by checking for
     * replacements everywhere else.
     * @param dsConfig a DSConfig to update the value of and then add in to the main config
     * @return true if the config changed or is new
     */
    protected boolean replaceAndAddConfig(DSpaceConfig dsConfig) {
        DSpaceConfig newConfig = null;
        String key = dsConfig.getKey();
        if (dsConfig.getValue().contains("${")) {
            String value = dsConfig.getValue();
            int start = -1;
            while ((start = value.indexOf("${")) > -1) {
                int end = value.indexOf('}', start);
                if (end > -1) {
                    String newKey = value.substring(start+2, end);
                    if (newKey.equals(key)) {
                        log.warn("Found circular reference for key ("+newKey+") in config value: " + value);
                        break;
                    }
                    DSpaceConfig dsc = this.configuration.get(newKey);
                    if (dsc == null) {
                        log.warn("Could not find key ("+newKey+") for replacement in value: " + value);
                        break;
                    }
                    String newVal = dsc.getValue();
                    value = value.replace("${"+newKey+"}", newVal);
                    newConfig = new DSpaceConfig(key, value);
                } else {
                    log.warn("Found '${' but could not find a closing '}' in the value: " + value);
                    break;
                }
            }
        }

        // add the config
        if (this.configuration.containsKey(key) && this.configuration.get(key).equals(dsConfig)) {
            return false; // SHORT CIRCUIT
        }

        // config changed or new
        this.configuration.put(key, newConfig != null ? newConfig : dsConfig);
        // update replacements
        replaceVariables(this.configuration);
        return true;
    }

    /**
     * This will replace the ${key} with the value from the matching key
     * if it exists.  Logs a warning if the key does not exist.
     * Goes through and updates the replacements for the the entire
     * configuration and updates any replaced values.
     */
    protected void replaceVariables(Map<String, DSpaceConfig> dsConfiguration) {
        for (Entry<String, DSpaceConfig> entry : dsConfiguration.entrySet()) {
            if (entry.getValue().getValue().contains("${")) {
                String value = entry.getValue().getValue();
                int start = -1;
                while ((start = value.indexOf("${")) > -1) {
                    int end = value.indexOf('}', start);
                    if (end > -1) {
                        String newKey = value.substring(start+2, end);
                        DSpaceConfig dsc = dsConfiguration.get(newKey);
                        if (dsc == null) {
                            log.warn("Could not find key ("+newKey+") for replacement in value: " + value);
                            break;
                        }
                        String newVal = dsc.getValue();
                        String oldValue = value;
                        value = value.replace("${"+newKey+"}", newVal);
                        if (value.equals(oldValue)) {
                            log.warn("No change after variable replacement -- is "
                                    + newKey + " = " + newVal +
                                    " a circular reference?");
                            break;
                        }
                        entry.setValue( new DSpaceConfig(entry.getValue().getKey(), value) );
                    } else {
                        log.warn("Found '${' but could not find a closing '}' in the value: " + value);
                        break;
                    }
                }
            }
        }
    }

    protected Properties readPropertyFile(String filePathName) {
        Properties props = new Properties();
        InputStream is = null;
        try {
            File f = new File(filePathName);
            if (f.exists()) {
                is = new FileInputStream(f);
                props.load(is);
                log.info("Loaded "+props.size()+" config properties from file: " + f);
            }
            else
            {
            	log.info("Failed to load config properties from file ("+filePathName+"): Does not exist");

            }
        } catch (Exception e) {
            log.warn("Failed to load config properties from file ("+filePathName+"): " + e.getMessage(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    // Ignore exception on close
                }
            }
        }
        return props;
    }

    protected Properties readPropertyResource(String resourcePathName) {
        Properties props = new Properties();
        try {
            ClassPathResource resource = new ClassPathResource(resourcePathName);
            if (resource.exists()) {
                props.load(resource.getInputStream());
                log.info("Loaded "+props.size()+" config properties from resource: " + resource);
            }
        } catch (Exception e) {
            log.warn("Failed to load config properties from resource ("+resourcePathName+"): " + e.getMessage(), e);
        }
        return props;
    }

    protected Properties readPropertyFile(File propertyFile) {
        Properties props = new Properties();
        try{
            if(propertyFile.exists()){
                props.load(new FileInputStream(propertyFile));
                log.info("Loaded"+props.size() + " config properties from file: " + propertyFile.getName());
            }

        } catch (Exception e){
            log.warn("Failed to load config properties from file (" + propertyFile.getName() + ": "+ e.getMessage(), e);
        }
        return props;
    }

    protected Properties readPropertyStream(InputStream propertyStream){
        Properties props = new Properties();
        try{
            props.load(propertyStream);
            log.info("Loaded"+props.size() + " config properties from stream");
        } catch (Exception e){
            log.warn("Failed to load config properties from stream: " + e.getMessage(), e);
        }
        return props;
    }

    protected void pushPropsToMap(Map<String, String> map, Properties props) {
        pushPropsToMap(map, null, props);

    }
    protected void pushPropsToMap(Map<String, String> map, String prefix, Properties props) {
        for (Entry<Object, Object> entry : props.entrySet()) {
            String key = entry.getKey().toString();
            if(prefix != null){
                key = prefix + "." + key;
            }
            map.put(key, entry.getValue() == null ? "" : entry.getValue().toString());
        }
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

    @Override
    public String toString() {
        return "Config:" + DSPACE_HOME + ":size=" + configuration.size();
    }


    /**
     * Constructs service name configs map for fast lookup of service
     * configurations.
     * @return the map of config service settings
     */
    public Map<String, Map<String, ServiceConfig>> makeServiceNameConfigs() {
        Map<String, Map<String, ServiceConfig>> serviceNameConfigs = new HashMap<String, Map<String,ServiceConfig>>();
        for (DSpaceConfig dsConfig : getConfiguration()) {
            String beanName = dsConfig.getBeanName();
            if (beanName != null) {
                Map<String, ServiceConfig> map = null;
                if (serviceNameConfigs.containsKey(beanName)) {
                    map = serviceNameConfigs.get(beanName);
                } else {
                    map = new HashMap<String, ServiceConfig>();
                    serviceNameConfigs.put(beanName, map);
                }
                map.put(beanName, new ServiceConfig(dsConfig));
            }
        }
        return serviceNameConfigs;
    }

    private <T> T convert(String value, Class<T> type) {
        SimpleTypeConverter converter = new SimpleTypeConverter();

        if (value != null) {
            if (type.isArray()) {
                String[] values = value.split(",");
                return (T)converter.convertIfNecessary(values, type);
            }

            if (type.isAssignableFrom(String.class)) {
                return (T)value;
            }
        } else {
            if (boolean.class.equals(type)) {
                return (T)Boolean.FALSE;
            } else if (int.class.equals(type) || long.class.equals(type)) {
                return (T)converter.convertIfNecessary(0, type);
            }
        }

        return (T)converter.convertIfNecessary(value, type);
    }
}
