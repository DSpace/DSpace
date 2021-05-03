/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for Orcid factory classes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class OrcidFactoryUtils {

    private OrcidFactoryUtils() {

    }

    /**
     * Parse the given configurations value and returns a map with metadata fields
     * as keys and types/sources as values.
     *
     * @param  configurations the configurations to parse
     * @return                the configurations parsing result as map
     */
    public static Map<String, String> parseConfigurations(String configurations) {
        Map<String, String> configurationMap = new HashMap<String, String>();
        if (StringUtils.isBlank(configurations)) {
            return configurationMap;
        }

        for (String configuration : configurations.split(",")) {
            String[] configurationSections = parseConfiguration(configuration);
            configurationMap.put(configurationSections[0], configurationSections[1]);
        }

        return configurationMap;
    }

    /**
     * Parse the given configuration value and returns it's section. The expected
     * configuration syntax is field::type.
     *
     * @param  configuration         the configuration to parse
     * @return                       the configuration sections
     * @throws IllegalStateException if the given configuration is not valid
     */
    private static String[] parseConfiguration(String configuration) {
        String[] configurations = configuration.split("::");
        if (configurations.length != 2) {
            throw new IllegalStateException(
                "The configuration '" + configuration + "' is not valid. Expected field::type");
        }
        return configurations;
    }

}
