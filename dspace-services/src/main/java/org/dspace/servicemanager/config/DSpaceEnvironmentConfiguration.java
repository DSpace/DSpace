/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bash does not allow environment variables that contain dots in their name.
 * This Configuration loads environment variables that contains two underlines
 * and replaces "__P__" -> "." and "__D__" -> "-"
 * E.g.: dspace__P__dir will be read as dspace.dir.
 * E.g.: my__D__dspace__P__prop will be read as my-dspace.prop.
 *
 * Most of this file was copied from org.apache.commons.configuration2.EnvironmentConfiguration.
 *
 * @author Pascal-Nicolas Becker -- dspace at pascal dash becker dot de
 */
public class DSpaceEnvironmentConfiguration extends MapConfiguration {

    private static Logger log = LoggerFactory.getLogger(DSpaceEnvironmentConfiguration.class);

    /**
     * Create a Configuration based on the environment variables.
     *
     * @see System#getenv()
     */
    public DSpaceEnvironmentConfiguration() {
        super(getModifiedEnvMap());
    }

    public static Map<String, Object> getModifiedEnvMap() {
        HashMap<String, Object> env = new HashMap<>(System.getenv().size());
        for (String key : System.getenv().keySet()) {
            // ignore all properties that do not contain __ as those will be loaded
            // by apache commons config environment lookup.
            if (!StringUtils.contains(key, "__")) {
                continue;
            }

            // replace "__P__" with a single dot.
            // replace "__D__" with a single dash.
            String lookup = StringUtils.replace(key, "__P__", ".");
            lookup = StringUtils.replace(lookup, "__D__", "-");
            if (System.getenv(key) != null) {
                // store the new key with the old value in our new properties map.
                env.put(lookup, System.getenv(key));
                log.debug("Found env " + lookup + " = " + System.getenv(key) + ".");
            } else {
                log.debug("Didn't found env " + lookup + ".");
            }
        }
        return env;
    }

    /**
     * Adds a property to this configuration. Because this configuration is
     * read-only, this operation is not allowed and will cause an exception.
     *
     * @param key   the key of the property to be added
     * @param value the property value
     */
    @Override
    protected void addPropertyDirect(String key, Object value) {
        throw new UnsupportedOperationException("EnvironmentConfiguration is read-only!");
    }

    /**
     * Removes a property from this configuration. Because this configuration is
     * read-only, this operation is not allowed and will cause an exception.
     *
     * @param key the key of the property to be removed
     */
    @Override
    protected void clearPropertyDirect(String key) {
        throw new UnsupportedOperationException("EnvironmentConfiguration is read-only!");
    }

    /**
     * Removes all properties from this configuration. Because this
     * configuration is read-only, this operation is not allowed and will cause
     * an exception.
     */
    @Override
    protected void clearInternal() {
        throw new UnsupportedOperationException("EnvironmentConfiguration is read-only!");
    }
}
