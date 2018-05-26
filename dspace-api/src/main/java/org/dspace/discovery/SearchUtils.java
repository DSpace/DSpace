/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItem;

/**
 * Util methods used by discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SearchUtils {
    /**
     * Cached search service
     **/
    private static SearchService searchService;

    /**
     * Default constructor
     */
    private SearchUtils() { }

    public static SearchService getSearchService() {
        if (searchService == null) {
            org.dspace.kernel.ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();
            searchService = manager.getServiceByName(SearchService.class.getName(), SearchService.class);
        }
        return searchService;
    }

    public static DiscoveryConfiguration getDiscoveryConfiguration() {
        return getDiscoveryConfiguration(null, null);
    }

    public static DiscoveryConfiguration getDiscoveryConfiguration(BrowsableDSpaceObject dso) {
        return getDiscoveryConfiguration(null, dso);
    }

    public static DiscoveryConfiguration getDiscoveryConfiguration(String prefix, BrowsableDSpaceObject dso) {
        if (prefix != null) {
            return getDiscoveryConfigurationByName(dso != null ? prefix + "." + dso.getHandle() : prefix);
        } else {
            return getDiscoveryConfigurationByName(dso != null ? dso.getHandle() : null);
        }
    }

    public static DiscoveryConfiguration getDiscoveryConfigurationByName(
        String configurationName) {
        DiscoveryConfigurationService configurationService = getConfigurationService();

        return configurationService.getDiscoveryConfiguration(configurationName);
    }

    public static DiscoveryConfigurationService getConfigurationService() {
        ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();
        return manager
            .getServiceByName(DiscoveryConfigurationService.class.getName(), DiscoveryConfigurationService.class);
    }

    public static List<String> getIgnoredMetadataFields(int type) {
        return getConfigurationService().getToIgnoreMetadataFields().get(type);
    }

    /**
     * Method that retrieves a list of all the configuration objects from the given item
     * A configuration object can be returned for each parent community/collection
     *
     * @param item the DSpace item
     * @return a list of configuration objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(Item item) throws SQLException {
        List<Collection> collections = item.getCollections();
        return getAllDiscoveryConfigurations(null, collections, item);
    }

    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(WorkspaceItem witem) throws SQLException {
        List<Collection> collections = new ArrayList<Collection>();
        collections.add(witem.getCollection());
        return getAllDiscoveryConfigurations("workspace", collections, witem.getItem());
    }

    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(WorkflowItem witem) throws SQLException {
        List<Collection> collections = new ArrayList<Collection>();
        collections.add(witem.getCollection());
        return getAllDiscoveryConfigurations("workflow", collections, witem.getItem());
    }

    private static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(String prefix,
                                                                              List<Collection> collections, Item item)
        throws SQLException {
        Map<String, DiscoveryConfiguration> result = new HashMap<String, DiscoveryConfiguration>();

        for (Collection collection : collections) {
            DiscoveryConfiguration configuration = getDiscoveryConfiguration(prefix, collection);
            if (!result.containsKey(configuration.getId())) {
                result.put(configuration.getId(), configuration);
            }
        }

        //Also add one for the default
        addConfigurationIfExists(result, prefix);

        return Arrays.asList(result.values().toArray(new DiscoveryConfiguration[result.size()]));
    }

    private static void addConfigurationIfExists(Map<String, DiscoveryConfiguration> result, String confName) {
        DiscoveryConfiguration configurationExtra = getDiscoveryConfigurationByName(confName);
        if (!result.containsKey(configurationExtra.getId())) {
            result.put(configurationExtra.getId(), configurationExtra);
        }
    }

}
