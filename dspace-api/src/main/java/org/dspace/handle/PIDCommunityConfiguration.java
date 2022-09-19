/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
/**
 * Class encapsulating community based PIDs configuration.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class PIDCommunityConfiguration {

    public static final String TYPE_LOCAL = "local";

    public static final String TYPE_EPIC = "epic";

    private static final String COMMUNITY_KEYWORD = "community";

    private static final String CANONICAL_PREFIX_KEYWORD = "canonical_prefix";

    private static final String ALTERNATIVE_PREFIXES_KEYWORD = "alternative_prefixes";

    private static final String SUBPREFIX_KEYWORD = "subprefix";

    public static final String ALTERNATIVE_PREFIXES_DELIMITER = "|";

    private static final String PREFIX_KEYWORD = "prefix";

    private static final String TYPE_KEYWORD = "type";

    public static final String ANY_KEYWORD = "*";

    private Map<String, String> configMap;

    PIDCommunityConfiguration(Map<String, String> configMap) {
        this.configMap = configMap;
    }

    /**
     * Returns PID type for given community
     * @return PID service type or null
     */
    public String getType() {
        return configMap.get(TYPE_KEYWORD);
    }

    /**
     * Returns canonical PID prefix for given community
     * @return PID prefix or null
     */
    public String getCanonicalPrefix() {
        return configMap.get(CANONICAL_PREFIX_KEYWORD);
    }

    /**
     * Returns PID prefix for given community
     * @return PID prefix or null
     */
    public String getPrefix() {
        return configMap.get(PREFIX_KEYWORD);
    }

    /**
     * Returns PID subprefix for given community
     * @return PID subprefix or null
     */
    public String getSubprefix() {
        return configMap.get(SUBPREFIX_KEYWORD);
    }

    public boolean isEpic() {
        return configMap.get(TYPE_KEYWORD).equals(TYPE_EPIC);
    }

    public boolean isLocal() {
        return configMap.get(TYPE_KEYWORD).equals(TYPE_LOCAL);
    }

    /**
     * Returns array of alternative prefixes for this community
     *
     * @return Array of alternative prefixes for this community
     */
    public String[] getAlternativePrefixes() {
        String[] alternativePrefixes = {};
        String alternativePrefixesString = configMap.get(ALTERNATIVE_PREFIXES_KEYWORD);
        if (Objects.nonNull(alternativePrefixesString)) {
            alternativePrefixes = StringUtils.split(alternativePrefixesString, ALTERNATIVE_PREFIXES_DELIMITER);
        }
        return alternativePrefixes;
    }

    /**
     * @return PID service type or null
     */
    public UUID getCommunityID() {
        UUID communityID;
        String value = configMap.get(COMMUNITY_KEYWORD);

        if (Objects.isNull(value)) {
            return null;
        }

        if (value.equals(ANY_KEYWORD)) {
            communityID = null;
        } else {
            communityID = UUID.fromString(value);
        }
        return communityID;
    }
}
