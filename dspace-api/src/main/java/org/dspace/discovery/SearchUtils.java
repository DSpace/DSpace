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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.utils.DiscoverQueryBuilder;
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

    public static final String AUTHORITY_SEPARATOR = "###";
    public static final String LAST_INDEXED_FIELD = "SolrIndexer.lastIndexed";
    public static final String RESOURCE_UNIQUE_ID = "search.uniqueid";
    public static final String RESOURCE_TYPE_FIELD = "search.resourcetype";
    public static final String RESOURCE_ID_FIELD = "search.resourceid";
    public static final String NAMED_RESOURCE_TYPE = "namedresourcetype";
    public static final String FILTER_SEPARATOR = "\n|||\n";

    /**
     * Cached search service
     **/
    private static SearchService searchService;

    /**
     * Default constructor
     */
    private SearchUtils() { }

    /**
     * Return an instance of the {@link SearchService}.
     */
    public static SearchService getSearchService() {
        if (searchService == null) {
            org.dspace.kernel.ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();
            searchService = manager.getServiceByName(SearchService.class.getName(), SearchService.class);
        }
        return searchService;
    }

    /**
     * Clear the cached {@link SearchService} instance, forcing it to be retrieved from the service manager again
     * next time {@link SearchUtils#getSearchService} is called.
     * In practice, this is only necessary for integration tests in some environments
     * where the cached version may no longer be up to date between tests.
     */
    public static void clearCachedSearchService() {
        searchService = null;
    }

    /**
     * Retrieves the Discovery Configuration for a null context, prefix and DSpace object.
     * This will result in returning the default configuration
     * @return the default configuration
     */
    public static DiscoveryConfiguration getDiscoveryConfiguration() {
        return getDiscoveryConfiguration(null, null, null);
    }

    /**
     * Retrieves the Discovery Configuration with a null prefix for a DSpace object.
     * @param context
     *              the dabase context
     * @param dso
     *              the DSpace object
     * @return the Discovery Configuration for the specified DSpace object
     */
    public static DiscoveryConfiguration getDiscoveryConfiguration(Context context, DSpaceObject dso) {
        return getDiscoveryConfiguration(context, null, dso);
    }

    /**
     * Return the discovery configuration to use in a specific scope for the king of search identified by the prefix. A
     * null prefix mean the normal query, other predefined values are workspace or workflow
     *
     *
     * @param context
     *            the database context
     * @param prefix
     *            the namespace of the configuration to lookup if any
     * @param dso
     *            the DSpaceObject
     * @return the discovery configuration for the specified scope
     */
    public static DiscoveryConfiguration getDiscoveryConfiguration(Context context, String prefix,
                                                                   DSpaceObject dso) {
        if (prefix != null) {
            return getDiscoveryConfigurationByName(dso != null ? prefix + "." + dso.getHandle() : prefix);
        } else {
            return getDiscoveryConfigurationByDSO(context, dso);
        }
    }

    /**
     * Retrieve the configuration for the current dspace object and all its parents and add it to the provided set
     * @param context           - The database context
     * @param configurations    - The set of configurations to add the retrieved configurations to
     * @param prefix            - The namespace of the configuration to lookup if any
     * @param dso               - The DSpace Object
     * @return the set of configurations with additional retrieved ones for the dspace object and parents
     * @throws SQLException
     */
    public static Set<DiscoveryConfiguration> addDiscoveryConfigurationForParents(
            Context context, Set<DiscoveryConfiguration> configurations, String prefix, DSpaceObject dso)
            throws SQLException {
        if (dso == null) {
            configurations.add(getDiscoveryConfigurationByName(null));
            return configurations;
        }
        if (prefix != null) {
            configurations.add(getDiscoveryConfigurationByName(prefix + "." + dso.getHandle()));
        } else {
            configurations.add(getDiscoveryConfigurationByName(dso.getHandle()));
        }

        DSpaceObjectService<DSpaceObject> dSpaceObjectService = ContentServiceFactory.getInstance()
                                                                                     .getDSpaceObjectService(dso);
        DSpaceObject parentObject = dSpaceObjectService.getParentObject(context, dso);
        return addDiscoveryConfigurationForParents(context, configurations, prefix, parentObject);
    }

    /**
     * Return the discovery configuration identified by the specified name
     *
     * @param configurationName the configuration name assigned to the bean in the
     *                          discovery.xml
     * @return the discovery configuration
     */
    public static DiscoveryConfiguration getDiscoveryConfigurationByName(
        String configurationName) {
        DiscoveryConfigurationService configurationService = getConfigurationService();

        return configurationService.getDiscoveryConfiguration(configurationName);
    }

    /**
     * Return the discovery configuration for the provided DSO
     * @param context   - The database context
     * @param dso       - The DSpace object to retrieve the configuration for
     * @return the discovery configuration for the provided DSO
     */
    public static DiscoveryConfiguration getDiscoveryConfigurationByDSO(
        Context context, DSpaceObject dso) {
        DiscoveryConfigurationService configurationService = getConfigurationService();
        return configurationService.getDiscoveryDSOConfiguration(context, dso);
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
     * @param context   the database context
     * @param item the DSpace item
     * @return a list of configuration objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(Context context, Item item)
            throws SQLException {
        List<Collection> collections = item.getCollections();
        return getAllDiscoveryConfigurations(context, null, collections, item);
    }

    /**
     * Return all the discovery configuration applicable to the provided workspace item
     *
     * @param context
     * @param witem a workspace item
     * @return a list of discovery configuration
     * @throws SQLException
     */
    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(final Context context,
                                                                             WorkspaceItem witem) throws SQLException {
        List<Collection> collections = new ArrayList<Collection>();
        collections.add(witem.getCollection());
        return getAllDiscoveryConfigurations(context, "workspace", collections, witem.getItem());
    }

    /**
     * Return all the discovery configuration applicable to the provided workflow item
     *
     * @param context
     * @param witem a workflow item
     * @return a list of discovery configuration
     * @throws SQLException
     */
    public static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(final Context context,
                                                                             WorkflowItem witem) throws SQLException {
        List<Collection> collections = new ArrayList<Collection>();
        collections.add(witem.getCollection());
        return getAllDiscoveryConfigurations(context, "workflow", collections, witem.getItem());
    }

    private static List<DiscoveryConfiguration> getAllDiscoveryConfigurations(final Context context,
                                                                              String prefix,
                                                                              List<Collection> collections, Item item)
        throws SQLException {
        Set<DiscoveryConfiguration> result = new HashSet<>();

        for (Collection collection : collections) {
            addDiscoveryConfigurationForParents(context, result, prefix, collection);
        }

        //Add alwaysIndex configurations
        DiscoveryConfigurationService configurationService = getConfigurationService();
        result.addAll(configurationService.getIndexAlwaysConfigurations());

        //Also add one for the default
        addConfigurationIfExists(result, prefix);

        return Arrays.asList(result.toArray(new DiscoveryConfiguration[result.size()]));
    }

    private static void addConfigurationIfExists(Set<DiscoveryConfiguration> result, String confName) {
        DiscoveryConfiguration configurationExtra = getDiscoveryConfigurationByName(confName);
        result.add(configurationExtra);
    }

    public static DiscoverQueryBuilder getQueryBuilder() {
        ServiceManager manager = DSpaceServicesFactory.getInstance().getServiceManager();
        return manager
            .getServiceByName(DiscoverQueryBuilder.class.getName(), DiscoverQueryBuilder.class);
    }
}
