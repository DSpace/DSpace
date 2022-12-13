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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    public DiscoveryConfiguration getDiscoveryConfiguration(final Context context,
                                                            IndexableObject dso) {
        String name;
        if (dso == null) {
            name = "default";
        } else if (dso instanceof IndexableDSpaceObject) {
            return getDiscoveryDSOConfiguration(context, ((IndexableDSpaceObject) dso).getIndexedObject());
        } else {
            name = dso.getUniqueIndexID();
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
    public DiscoveryConfiguration getDiscoveryDSOConfiguration(final Context context,
                                                               DSpaceObject dso) {
        String name;
        if (dso == null) {
            name = "default";
        } else {
            name = dso.getHandle();
        }

        DiscoveryConfiguration configuration = getDiscoveryConfiguration(name, false);
        if (configuration != null) {
            return configuration;
        }
        DSpaceObjectService<DSpaceObject> dSpaceObjectService =
                ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
        DSpaceObject parentObject = null;
        try {
            parentObject = dSpaceObjectService.getParentObject(context, dso);
        } catch (SQLException e) {
            log.error(e);
        }
        return getDiscoveryDSOConfiguration(context, parentObject);
    }

    public DiscoveryConfiguration getDiscoveryConfiguration(final String name) {
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

    public DiscoveryConfiguration getDiscoveryConfigurationByNameOrDso(final String configurationName,
                                                                       final Context context,
                                                                       final IndexableObject dso) {
        if (StringUtils.isNotBlank(configurationName) && getMap().containsKey(configurationName)) {
            return getMap().get(configurationName);
        } else {
            return getDiscoveryConfiguration(context, dso);
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
}
