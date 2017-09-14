/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoveryConfigurationService {

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

    public DiscoveryConfiguration getDiscoveryConfiguration(DSpaceObject dso) {
        String name;
        if(dso == null) {
            name = "site";
        } else {
            name = dso.getHandle();
        }

        return getDiscoveryConfiguration(name);
    }

    public DiscoveryConfiguration getDiscoveryConfiguration(final String name) {
        DiscoveryConfiguration result;

        result = StringUtils.isBlank(name) ? null : getMap().get(name);

        if (result == null) {
            //No specific configuration, get the default one
            result = getMap().get("default");
        }

        return result;
    }

    public DiscoveryConfiguration getDiscoveryConfigurationByNameOrDso(final String configurationName, final DSpaceObject dso) {
        if(StringUtils.isNotBlank(configurationName) && getMap().containsKey(configurationName)) {
            return getMap().get(configurationName);
        } else {
            return getDiscoveryConfiguration(dso);
        }
    }

    public static void main(String[] args) {
        System.out.println(DSpaceServicesFactory.getInstance().getServiceManager().getServicesNames().size());
        DiscoveryConfigurationService mainService = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(DiscoveryConfigurationService.class.getName(), DiscoveryConfigurationService.class);

        for(String key : mainService.getMap().keySet()){
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
            DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration = discoveryConfiguration.getRecentSubmissionConfiguration();
            System.out.println("\tMetadata sort field: " + recentSubmissionConfiguration.getMetadataSortField());
            System.out.println("\tMax recent submissions: " + recentSubmissionConfiguration.getMax());

            List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
            if(0 < defaultFilterQueries.size()){
                System.out.println("Default filter queries");
                for (String fq : defaultFilterQueries) {
                    System.out.println("\t" + fq);
                }
            }
        }
    }
}
