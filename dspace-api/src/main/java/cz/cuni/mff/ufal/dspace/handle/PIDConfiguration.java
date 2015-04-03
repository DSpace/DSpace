/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.handle;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;

/**
 * Class encapsulating PIDs configuration
 * 
 * @author Michal Josífko
 * 
 */

public class PIDConfiguration
{

    /** log4j category */
    private static Logger log = Logger.getLogger(PIDConfiguration.class);

    private static PIDConfiguration instance;

    private static final String LINDAT_PID_COMMUNITY_CONFIGURATIONS_KEYWORD = "lr.pid.community.configurations";

    private static Map<Integer, PIDCommunityConfiguration> pidCommunityConfigurations;

    private PIDConfiguration()
    {
        initialize();
    }

    /**
     * Initializes the singleton
     */
    private void initialize()
    {
        String pidCommunityConfigurationsString = ConfigurationManager.getProperty("lr", LINDAT_PID_COMMUNITY_CONFIGURATIONS_KEYWORD);

        pidCommunityConfigurations = new HashMap<Integer, PIDCommunityConfiguration>();
        if (pidCommunityConfigurationsString != null)
        {
            for (String pidCommunityConfigurationString : pidCommunityConfigurationsString
                    .split(";"))
            {
                PIDCommunityConfiguration pidCommunityConfiguration = PIDCommunityConfiguration
                        .fromString(pidCommunityConfigurationString);
                pidCommunityConfigurations.put(
                        pidCommunityConfiguration.getCommunityID(),
                        pidCommunityConfiguration);
            }
        }
    }

    /**
     * Returns the only instance of this singleton
     * 
     * @return PIDConfiguration
     */
    public static PIDConfiguration getInstance()
    {
        if (instance == null)
        {
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
            Integer communityID)
    {
        instance = getInstance();
        PIDCommunityConfiguration pidCommunityConfiguration = pidCommunityConfigurations
                .get(communityID);
        if (pidCommunityConfiguration == null)
        {
            pidCommunityConfiguration = pidCommunityConfigurations.get(null);
        }
        if (pidCommunityConfiguration == null)
        {
            throw new IllegalStateException("Missing configuration entry in "
                    + LINDAT_PID_COMMUNITY_CONFIGURATIONS_KEYWORD
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
    public static PIDCommunityConfiguration getPIDCommunityConfiguration(
            DSpaceObject dso) throws SQLException
    {
        instance = getInstance();
        Integer communityID = null;
        Community community = dso.getPrincipalCommunity();
        if (community != null)
        {
            communityID = community.getID();
        }
        PIDCommunityConfiguration pidCommunityConfiguration = getPIDCommunityConfiguration(communityID);
        return pidCommunityConfiguration;
    }
    
    /**
     * Returns map of PID community communications
     * 
     * @return Map of PID community communications
     */
    public static Map<Integer, PIDCommunityConfiguration> getPIDCommunityConfigurations()
    {
        instance = getInstance();
        return pidCommunityConfigurations;    
    }
    
    /**
     * Returns default PID community configuration
     * 
     * @return Default PID community configuration or null
     */
    public static PIDCommunityConfiguration getDefaultCommunityConfiguration()
    {        
        instance = getInstance();
        PIDCommunityConfiguration pidCommunityConfiguration = getPIDCommunityConfiguration((Integer)null);
        if(pidCommunityConfiguration == null)
        {   
            Integer[] keys = pidCommunityConfigurations.keySet().toArray(new Integer[0]);
            if(keys.length > 0)
            {
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
    public static String[] getAlternativePrefixes(String mainPrefix) 
    {
        instance = getInstance();
        Set<String> alternativePrefixes = new HashSet<String>(); 
        for(PIDCommunityConfiguration pidCommunityConfiguration : pidCommunityConfigurations.values())
        {                  
            if(mainPrefix != null && mainPrefix.equals(pidCommunityConfiguration.getPrefix()))
            {
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
    public static String getDefaultPrefix() 
    {        
        instance = getInstance();
        String prefix = null;
        PIDCommunityConfiguration pidCommunityConfiguration = getDefaultCommunityConfiguration();
        if(pidCommunityConfiguration != null)
        {
            prefix = pidCommunityConfiguration.getPrefix();
        }
        return prefix;
    }
    
    /**
     * Returns all possible prefixes for all communities
     * 
     * @return All possible prefixes for all communities
     */
    public static Set<String> getSupportedPrefixes() 
    {                
        instance = getInstance();
        Set<String> prefixes = new HashSet<String>(); 
        for(PIDCommunityConfiguration pidCommunityConfiguration : pidCommunityConfigurations.values())
        {                  
            prefixes.add(pidCommunityConfiguration.getPrefix());
            Collections.addAll(prefixes, pidCommunityConfiguration.getAlternativePrefixes());            
        }
        return prefixes;
    }

}