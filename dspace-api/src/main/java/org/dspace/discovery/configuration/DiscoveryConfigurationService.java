/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.indexobject.IndexableDSpaceObject;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoveryConfigurationService {

    private static final Logger log = LogManager.getLogger();

    private Map<String, DiscoveryConfiguration> map;
    private Map<Integer, List<String>> toIgnoreMetadataFields = new HashMap<>();

    /**
     * Discovery configurations, cached by Community/Collection UUID. When a  Community or Collection does not have its
     * own configuration, we take the one of the first parent that does.
     * This cache ensures we do not have to go up the hierarchy every time.
     */
    private final Map<UUID, DiscoveryConfiguration> comColToDiscoveryConfigurationMap = new ConcurrentHashMap<>();

    public Map<String, DiscoveryConfiguration> getMap() {
        return map;
    }

    public void setMap(Map<String, DiscoveryConfiguration> map) {
        this.map = map;
    }

    public Map<Integer, List<String>> getToIgnoreMetadataFields() {
        return toIgnoreMetadataFields;
    }

    public void setToIgnoreMetadataFields(Map<Integer, List<String>> toIgnoreMetadataFields) {
        this.toIgnoreMetadataFields = toIgnoreMetadataFields;
    }

    /**
     * Retrieve the discovery configuration for the provided IndexableObject. When a DSpace Object can be retrieved from
     * the IndexableObject, the discovery configuration will be returned for the DSpace Object. Otherwise, a check will
     * be done to look for the unique index ID of the IndexableObject. When the IndexableObject is null, the default
     * configuration will be retrieved
     *
     * When no direct match is found, the parent object will
     * be checked until there is no parent left, in which case the "default" configuration will be returned.
     * @param context   - The database context
     * @param indexableObject       - The IndexableObject to retrieve the configuration for
     * @return the discovery configuration for the provided IndexableObject.
     */
    public DiscoveryConfiguration getDiscoveryConfiguration(Context context, IndexableObject indexableObject) {
        String name;
        if (indexableObject == null) {
            return getDiscoveryConfiguration(null);
        } else if (indexableObject instanceof IndexableDSpaceObject) {
            return getDiscoveryDSOConfiguration(context, ((IndexableDSpaceObject) indexableObject).getIndexedObject());
        } else {
            name = indexableObject.getUniqueIndexID();
        }
        return getDiscoveryConfiguration(name);
    }

    /**
     * Retrieve the discovery configuration for the provided DSO. When no direct match is found, the parent object will
     * be checked until there is no parent left, in which case the "default" configuration will be returned.
     * @param context   - The database context
     * @param dso       - The DSpace object to retrieve the configuration for
     * @return the discovery configuration for the provided DSO.
     */
    public DiscoveryConfiguration getDiscoveryDSOConfiguration(final Context context, DSpaceObject dso) {
        // Fall back to default configuration
        if (dso == null) {
            return getDiscoveryConfiguration(null, true);
        }

        // Attempt to retrieve cached configuration by UUID
        if (comColToDiscoveryConfigurationMap.containsKey(dso.getID())) {
            return comColToDiscoveryConfigurationMap.get(dso.getID());
        }

        DiscoveryConfiguration configuration;

        // Attempt to retrieve configuration by DSO handle
        configuration = getDiscoveryConfiguration(dso.getHandle(), false);

        if (configuration == null) {
            // Recurse up the Comm/Coll hierarchy until a configuration is found
            DSpaceObjectService<DSpaceObject> dSpaceObjectService =
                ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
            DSpaceObject parentObject = null;
            try {
                parentObject = dSpaceObjectService.getParentObject(context, dso);
            } catch (SQLException e) {
                log.error(e);
            }
            configuration = getDiscoveryDSOConfiguration(context, parentObject);
        }

        // Cache the resulting configuration when the DSO is a Community or Collection
        if (dso instanceof Community || dso instanceof Collection) {
            comColToDiscoveryConfigurationMap.put(dso.getID(), configuration);
        }

        return configuration;
    }

    /**
     * Retrieve the Discovery Configuration for the provided name. When no configuration can be found for the name, the
     * default configuration will be returned.
     * @param name  - The name of the configuration to be retrieved
     * @return the Discovery Configuration for the provided name, or default when none was found.
     */
    public DiscoveryConfiguration getDiscoveryConfiguration(String name) {
        return getDiscoveryConfiguration(name, true);
    }

    /**
     * Retrieve the configuration for the provided name. When useDefault is set to true, the "default" configuration
     * will be returned when no match is found. When useDefault is set to false, null will be returned when no match is
     * found.
     * @param name          - The name of the configuration to retrieve
     * @param useDefault    - Whether the default configuration should be used when no match is found
     * @return the configuration for the provided name
     */
    public DiscoveryConfiguration getDiscoveryConfiguration(final String name, boolean useDefault) {
        DiscoveryConfiguration result;

        result = StringUtils.isBlank(name) ? null : getMap().get(name);

        if (result == null && useDefault) {
            //No specific configuration, get the default one
            result = getMap().get("default");
        }

        return result;
    }

    /**
     * Retrieve the Discovery configuration for the provided name or IndexableObject. The configuration will first be
     * checked for the provided name. When no match is found for the name, the configuration will be retrieved for the
     * IndexableObject
     *
     * @param context           - The database context
     * @param configurationName - The name of the configuration to be retrieved
     * @param indexableObject   - The indexable object to retrieve the configuration for
     * @return the Discovery configuration for the provided name, or when not found for the provided IndexableObject
     */
    public DiscoveryConfiguration getDiscoveryConfigurationByNameOrIndexableObject(Context context,
                                                                                   String configurationName,
                                                                                   IndexableObject indexableObject) {
        if (StringUtils.isNotBlank(configurationName) && getMap().containsKey(configurationName)) {
            return getMap().get(configurationName);
        } else {
            return getDiscoveryConfiguration(context, indexableObject);
        }
    }

    /**
     * Retrieves a list of all DiscoveryConfiguration objects where
     * {@link org.dspace.discovery.configuration.DiscoveryConfiguration#isIndexAlways()} is true
     * These configurations should always be included when indexing
     */
    public List<DiscoveryConfiguration> getIndexAlwaysConfigurations() {
        List<DiscoveryConfiguration> configs = new ArrayList<>();
        for (String key : map.keySet()) {
            DiscoveryConfiguration config = map.get(key);
            if (config.isIndexAlways()) {
                configs.add(config);
            }
        }
        return configs;
    }

    /**
     * @return All configurations for {@link org.dspace.discovery.configuration.DiscoverySearchFilterFacet}
     */
    public List<DiscoverySearchFilterFacet> getAllFacetsConfig() {
        List<DiscoverySearchFilterFacet> configs = new ArrayList<>();
        for (String key : map.keySet()) {
            DiscoveryConfiguration config = map.get(key);
            configs.addAll(config.getSidebarFacets());
        }
        return configs;
    }

    public static void main(String[] args) {
        System.out.println(DSpaceServicesFactory.getInstance().getServiceManager().getServicesNames().size());
        DiscoveryConfigurationService mainService = DSpaceServicesFactory.getInstance().getServiceManager()
                                                                         .getServiceByName(
                                                                                 DiscoveryConfigurationService.class
                                                                                         .getName(),
                                                                                 DiscoveryConfigurationService.class);

        for (String key : mainService.getMap().keySet()) {
            System.out.println(key);

            System.out.println("Facets:");
            DiscoveryConfiguration discoveryConfiguration = mainService.getMap().get(key);
            for (int i = 0; i < discoveryConfiguration.getSidebarFacets().size(); i++) {
                DiscoverySearchFilterFacet sidebarFacet = discoveryConfiguration.getSidebarFacets().get(i);
                System.out.println("\t" + sidebarFacet.getIndexFieldName());
                for (int j = 0; j < sidebarFacet.getMetadataFields().size(); j++) {
                    String metadataField = sidebarFacet.getMetadataFields().get(j);
                    System.out.println("\t\t" + metadataField);
                }
            }

            System.out.println("Search filters");
            List<DiscoverySearchFilter> searchFilters = discoveryConfiguration.getSearchFilters();
            for (DiscoverySearchFilter searchFilter : searchFilters) {
                for (int i = 0; i < searchFilter.getMetadataFields().size(); i++) {
                    String metadataField = searchFilter.getMetadataFields().get(i);
                    System.out.println("\t\t" + metadataField);
                }

            }

            System.out.println("Recent submissions configuration:");
            DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration = discoveryConfiguration
                    .getRecentSubmissionConfiguration();
            System.out.println("\tMetadata sort field: " + recentSubmissionConfiguration.getMetadataSortField());
            System.out.println("\tMax recent submissions: " + recentSubmissionConfiguration.getMax());

            List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
            if (0 < defaultFilterQueries.size()) {
                System.out.println("Default filter queries");
                for (String fq : defaultFilterQueries) {
                    System.out.println("\t" + fq);
                }
            }
        }
    }

    /**
     * Retrieves a list of all DiscoveryConfiguration objects where key starts with prefixConfigurationName
     * 
     * @param prefixConfigurationName string as prefix key
     */
    public List<DiscoveryConfiguration> getDiscoveryConfigurationWithPrefixName(final String prefixConfigurationName) {
        List<DiscoveryConfiguration> discoveryConfigurationList = new ArrayList<>();
        if (StringUtils.isNotBlank(prefixConfigurationName)) {
            for (String key : map.keySet()) {
                if (key.equals(prefixConfigurationName) || key.startsWith(prefixConfigurationName)) {
                    DiscoveryConfiguration config = map.get(key);
                    discoveryConfigurationList.add(config);
                }
            }
        }
        return discoveryConfigurationList;
    }

}
