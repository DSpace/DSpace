/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.handle;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Class encapsulating community based PIDs configuration
 * 
 * @author Michal Josifko
 * 
 */

public class PIDCommunityConfiguration
{
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

    /**
     * Creates new AssignmentRules from given string
     * 
     * @param s
     *            String with assignment rules
     * @return New instance of this class
     */
    public static PIDCommunityConfiguration fromString(String s)
    {
        Map<String, String> configMap = new HashMap<String, String>();
        for (String part : s.split(","))
        {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2)
            {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                configMap.put(key, value);
            }
        }
        return new PIDCommunityConfiguration(configMap);
    }

    /**
     * Returns PID type for given community
     * 
     * @param communityID
     *            Community ID
     * @return PID service type or null
     */
    public Integer getCommunityID()
    {
        Integer communityID = null;
        String value = configMap.get(COMMUNITY_KEYWORD);
        if (value != null)
        {
            if (value.equals(ANY_KEYWORD))
            {
                communityID = null;
            }
            else
            {
                communityID = Integer.valueOf(value);
            }
        }
        return communityID;
    }

    /**
     * Returns PID type for given community
     * 
     * @param communityID
     *            Community ID
     * @return PID service type or null
     */
    public String getType()
    {
        return configMap.get(TYPE_KEYWORD);
    }

    /**
     * Returns canonical PID prefix for given community
     * 
     * @param communityID
     *            Community ID
     * @return PID prefix or null
     */
    public String getCanonicalPrefix()
    {
        return configMap.get(CANONICAL_PREFIX_KEYWORD);
    }

    /**
     * Returns PID prefix for given community
     * 
     * @param communityID
     *            Community ID
     * @return PID prefix or null
     */
    public String getPrefix()
    {
        return configMap.get(PREFIX_KEYWORD);
    }
    
    /**
     * Returns PID subprefix for given community
     * 
     * @param communityID
     *            Community ID
     * @return PID subprefix or null
     */
    public String getSubprefix()
    {
        return configMap.get(SUBPREFIX_KEYWORD);
    }

    /**
     * Creates new AssignmentRules from given list of rules
     * 
     * @param rulesList
     *            List of key value maps representing individual rules
     */

    PIDCommunityConfiguration(Map<String, String> configMap)
    {
        this.configMap = configMap;
    }

    public boolean isEpic()
    {        
        return configMap.get(TYPE_KEYWORD).equals(TYPE_EPIC);
    }

    public boolean isLocal()
    {
        return configMap.get(TYPE_KEYWORD).equals(TYPE_LOCAL);
    }
    
    /**
     * Returns array of alternative prefixes for this community
     * 
     * @return Array of alternative prefixes for this community
     */
    public String[] getAlternativePrefixes()
    {
        String[] alternativePrefixes = {};
        String alternativePrefixesString = configMap.get(ALTERNATIVE_PREFIXES_KEYWORD); 
        if(alternativePrefixesString != null)
        {
            alternativePrefixes = StringUtils.split(alternativePrefixesString, ALTERNATIVE_PREFIXES_DELIMITER);
        }
        return alternativePrefixes;
    }

}
