/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
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
        // All configurations are loaded into one array.
        // New configuration starts after loaded part contains "community".
        String[] pidCommunityConfigurationsArray = configurationService.getArrayProperty
                (CLARIN_PID_COMMUNITY_CONFIGURATIONS_KEYWORD);

        if (ArrayUtils.isEmpty(pidCommunityConfigurationsArray)) {
            return;
        }

        String convertedProperties = convertPropertyToValidString(pidCommunityConfigurationsArray);
        if (StringUtils.isEmpty(convertedProperties)) {
            log.error("Cannot convert community array property into valid string.");
            return;
        }

        pidCommunityConfigurations = new HashMap<UUID, PIDCommunityConfiguration>();
        for (String pidCommunityConfigurationString : convertedProperties.split(";")) {
            PIDCommunityConfiguration pidCommunityConfiguration = PIDCommunityConfiguration
                    .fromString(pidCommunityConfigurationString);
            pidCommunityConfigurations.put(
                    pidCommunityConfiguration.getCommunityID(),
                    pidCommunityConfiguration);
        }
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

        if (MapUtils.isEmpty(pidCommunityConfigurations)) {
            log.info("The configuration property " + CLARIN_PID_COMMUNITY_CONFIGURATIONS_KEYWORD + " is not defined." +
                    " Using default configuration of the `handle.prefix`.");
            return null;
        }

        PIDCommunityConfiguration pidCommunityConfiguration = pidCommunityConfigurations
                .get(communityID);

        if (Objects.isNull(pidCommunityConfiguration)) {
            // Yes, there is a configuration for the community with ID `null`.
            pidCommunityConfiguration = pidCommunityConfigurations.get(null);
        }
        if (Objects.isNull(pidCommunityConfiguration)) {
            log.info("Missing configuration entry in " + CLARIN_PID_COMMUNITY_CONFIGURATIONS_KEYWORD +
                    " for community with ID {}. Using default configuration of the `handle.prefix`.", communityID);
            return null;
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

    /**
     * Convert array property into single string property divided by `;` and not by `,`.
     * @param pidCommunityConfigurationsArray
     * @return
     */
    public String convertPropertyToValidString(String[] pidCommunityConfigurationsArray) {
        String wholePccString = String.join(",", pidCommunityConfigurationsArray);
        String[] splittedByCommunity = wholePccString.split("community=");
        Collection<String> pccWithoutCommunity = Arrays.asList(splittedByCommunity);

        // pcc = pidCommunityConfigurations
        StringBuilder convertedPccString = new StringBuilder();
        // Add `community=` string into start of the property
        for (String pcc : pccWithoutCommunity) {
            if (StringUtils.isEmpty(pcc)) {
                continue;
            }
            pcc = "community=" + pcc;
            // If last character is `,` replace it with `;`
            if (pcc.endsWith(",")) {
                int indexToReplace = pcc.lastIndexOf(",");
                pcc = pcc.substring(0, indexToReplace) + ";";
            }
            convertedPccString.append(pcc);
        }
        return convertedPccString.toString();
    }

    /**
     * Reload community configuration. It is for testing purposes.
     */
    public void reloadPidCommunityConfigurations() {
        if (Objects.nonNull(pidCommunityConfigurations)) {
            pidCommunityConfigurations.clear();
            pidCommunityConfigurations = null;
        }
        initialize();
    }
}
