/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DspaceObjectClarinService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.springframework.stereotype.Component;

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
/**
 * Class encapsulating PIDs configuration.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@Component
public class PIDConfiguration {
    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(PIDConfiguration.class);
    private static PIDConfiguration instance;

    private static final String CLARIN_PID_COMMUNITY_CONFIGURATIONS_KEYWORD = "lr.pid.community.configurations";

    private static Map<UUID, PIDCommunityConfiguration> pidCommunityConfigurations;

    private ConfigurationService configurationService = new DSpace().getConfigurationService();

    private DspaceObjectClarinService dspaceObjectClarinService =
            ContentServiceFactory.getInstance().getDspaceObjectClarinService();

    private PIDConfiguration() {
        initialize();
    }

    /**
     * Initializes the singleton
     */
    private void initialize() {
        //All configurations are loaded into one array.
        //New configuration starts after loaded part contains "community".
        String[] pidCommunityConfigurationsArray = configurationService.getArrayProperty
                (CLARIN_PID_COMMUNITY_CONFIGURATIONS_KEYWORD);

        if (ArrayUtils.isEmpty(pidCommunityConfigurationsArray)) {
            return;
        }

        //hashmap for creating PIDCommunityConfiguration
        Map<String, String> map = new HashMap<String, String>();
        pidCommunityConfigurations = new HashMap<UUID, PIDCommunityConfiguration>();
        //exists minimally one configuration, so first community is added to map not in cycle
        String[] keyValue = pidCommunityConfigurationsArray[0].split("=");

        if (keyValue.length < 2) {
            throw new RuntimeException("Cannot initialize PIDConfiguration, because the configuration " +
                    "property has wrong syntax. Property must be in the format: `key=value`");
        }
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();
        map.put(key, value);
        //another parts of configurations
        //start from the first position because the zero position was already added
        for (int i = 1; i < pidCommunityConfigurationsArray.length; i++) {
            keyValue = pidCommunityConfigurationsArray[i].split("=");
            key = keyValue[0].trim();
            value = keyValue[1].trim();
            //finding the end of the configuration
            if (key.equals("community")) {
                //creating PIDCOmmunityConfiguration
                PIDCommunityConfiguration pidCommunityConfiguration = new PIDCommunityConfiguration (map);
                pidCommunityConfigurations.put(
                        pidCommunityConfiguration.getCommunityID(),
                        pidCommunityConfiguration);
                //cleaning map for other configuration
                map.clear();
            }
            map.put(key, value);
        }
        //creating PIDCommunityConfiguration because the last configuration found has not been added
        PIDCommunityConfiguration pidCommunityConfiguration = new PIDCommunityConfiguration (map);
        pidCommunityConfigurations.put(
                pidCommunityConfiguration.getCommunityID(),
                pidCommunityConfiguration);
    }

    /**
     * Returns the only instance of this singleton
     *
     * @return PIDConfiguration
     */
    public static PIDConfiguration getInstance() {
        if (Objects.isNull(instance)) {
            instance = new PIDConfiguration();
        }
        return instance;
    }

    /**
     * Returns PID community configuration by community ID
     *
     * @param communityID
     *            Community ID
     * @return PID community configuration or null
     */
    public static PIDCommunityConfiguration getPIDCommunityConfiguration(
            UUID communityID) {
        instance = getInstance();

        PIDCommunityConfiguration pidCommunityConfiguration = pidCommunityConfigurations
                .get(communityID);
        if (Objects.isNull(pidCommunityConfiguration)) {
            pidCommunityConfiguration = pidCommunityConfigurations.get(null);
        }
        if (Objects.isNull(pidCommunityConfiguration)) {
            throw new IllegalStateException("Missing configuration entry in "
                    + CLARIN_PID_COMMUNITY_CONFIGURATIONS_KEYWORD
                    + " for community with ID " + communityID);
        }
        return pidCommunityConfiguration;
    }

    /**
     * Returns PID community configuration by DSpace object (according to
     * principal community)
     *
     * @param dso
     *            DSpaceObject
     * @return PID community configuration or null
     */
    public PIDCommunityConfiguration getPIDCommunityConfiguration(Context context,
                                                                  DSpaceObject dso) throws SQLException {
        instance = getInstance();
        UUID communityID = null;
        Community community = dspaceObjectClarinService.getPrincipalCommunity(context, dso);
        if (Objects.nonNull(community)) {
            communityID = community.getID();
        }
        return getPIDCommunityConfiguration(communityID);
    }

    /**
     * Returns map of PID community communications
     *
     * @return Map of PID community communications
     */
    public Map<UUID, PIDCommunityConfiguration> getPIDCommunityConfigurations() {
        instance = getInstance();
        return pidCommunityConfigurations;
    }

    /**
     * Returns default PID community configuration
     *
     * @return Default PID community configuration or null
     */
    public PIDCommunityConfiguration getDefaultCommunityConfiguration() {
        instance = getInstance();
        PIDCommunityConfiguration pidCommunityConfiguration = getPIDCommunityConfiguration((UUID)null);
        if (Objects.isNull(pidCommunityConfiguration)) {
            UUID[] keys = pidCommunityConfigurations.keySet().toArray(new UUID[0]);
            if (keys.length > 0) {
                pidCommunityConfiguration = getPIDCommunityConfiguration(keys[0]);
            }
        }
        return pidCommunityConfiguration;
    }

    /**
     * Returns array of distinct alternative prefixes from all community configurations
     *
     * @return Array of distinct alternative prefixes from all community configurations (can be empty)
     */
    public static String[] getAlternativePrefixes(String mainPrefix) {
        instance = getInstance();
        Set<String> alternativePrefixes = new HashSet<String>();
        for (PIDCommunityConfiguration pidCommunityConfiguration : pidCommunityConfigurations.values()) {
            if (Objects.nonNull(mainPrefix) && mainPrefix.equals(pidCommunityConfiguration.getPrefix())) {
                Collections.addAll(alternativePrefixes, pidCommunityConfiguration.getAlternativePrefixes());
            }
        }
        return (String[])alternativePrefixes.toArray(new String[alternativePrefixes.size()]);
    }

    /**
     * Returns prefix from default community configuration
     *
     * @return Prefix from default community configuration
     */
    public String getDefaultPrefix() {
        instance = getInstance();
        String prefix = null;
        PIDCommunityConfiguration pidCommunityConfiguration = getDefaultCommunityConfiguration();
        if (Objects.nonNull(pidCommunityConfiguration)) {
            prefix = pidCommunityConfiguration.getPrefix();
        }
        return prefix;
    }

    /**
     * Returns all possible prefixes for all communities
     *
     * @return All possible prefixes for all communities
     */
    public Set<String> getSupportedPrefixes() {
        instance = getInstance();
        Set<String> prefixes = new HashSet<String>();
        for (PIDCommunityConfiguration pidCommunityConfiguration : pidCommunityConfigurations.values()) {
            prefixes.add(pidCommunityConfiguration.getPrefix());
            Collections.addAll(prefixes, pidCommunityConfiguration.getAlternativePrefixes());
        }
        return prefixes;
    }
}
