/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.dspace.content.Item;
import org.dspace.content.MetadataDSpaceCsvExportServiceImpl;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.utils.DiscoverQueryBuilder;
import org.dspace.discovery.utils.parameter.QueryBuilderSearchFilter;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;

/**
 * Metadata exporter to allow the batch export of metadata from a discovery search into a file
 *
 */
public class MetadataExportSearch extends DSpaceRunnable<MetadataExportSearchScriptConfiguration> {
    private static final String EXPORT_CSV = "exportCSV";
    private boolean help = false;
    private String identifier;
    private String discoveryConfigName;
    private String[] filterQueryStrings;
    private boolean hasScope = false;
    private String query;

    private SearchService searchService;
    private MetadataDSpaceCsvExportService metadataDSpaceCsvExportService;
    private EPersonService ePersonService;
    private DiscoveryConfigurationService discoveryConfigurationService;
    private CommunityService communityService;
    private CollectionService collectionService;
    private DiscoverQueryBuilder queryBuilder;

    @Override
    public MetadataExportSearchScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("metadata-export-search", MetadataExportSearchScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        searchService = SearchUtils.getSearchService();
        metadataDSpaceCsvExportService = new DSpace().getServiceManager()
                                                     .getServiceByName(
                                                         MetadataDSpaceCsvExportServiceImpl.class.getCanonicalName(),
                                                         MetadataDSpaceCsvExportService.class
                                                     );
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        discoveryConfigurationService = SearchUtils.getConfigurationService();
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        queryBuilder = SearchUtils.getQueryBuilder();

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        if (commandLine.hasOption('q')) {
            query = commandLine.getOptionValue('q');
        }

        if (commandLine.hasOption('s')) {
            hasScope = true;
            identifier = commandLine.getOptionValue('s');
        }

        if (commandLine.hasOption('c')) {
            discoveryConfigName = commandLine.getOptionValue('c');
        }

        if (commandLine.hasOption('f')) {
            filterQueryStrings = commandLine.getOptionValues('f');
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            loghelpinfo();
            printHelp();
            return;
        }
        handler.logDebug("starting search export");

        IndexableObject dso = null;
        Context context = new Context();
        context.setCurrentUser(ePersonService.find(context, this.getEpersonIdentifier()));

        if (hasScope) {
            dso = resolveScope(context, identifier);
        }

        DiscoveryConfiguration discoveryConfiguration =
            discoveryConfigurationService.getDiscoveryConfiguration(discoveryConfigName);

        List<QueryBuilderSearchFilter> queryBuilderSearchFilters = new ArrayList<>();

        handler.logDebug("processing filter queries");
        if (filterQueryStrings != null) {
            for (String filterQueryString: filterQueryStrings) {
                String field = filterQueryString.split(",", 2)[0];
                String operator = filterQueryString.split("(,|=)", 3)[1];
                String value = filterQueryString.split("=", 2)[1];
                QueryBuilderSearchFilter queryBuilderSearchFilter =
                    new QueryBuilderSearchFilter(field, operator, value);
                queryBuilderSearchFilters.add(queryBuilderSearchFilter);
            }
        }
        handler.logDebug("building query");
        DiscoverQuery discoverQuery =
            queryBuilder.buildQuery(context, dso, discoveryConfiguration, query, queryBuilderSearchFilters,
            "Item", 10, Long.getLong("0"), null, SortOption.DESCENDING);
        handler.logDebug("creating iterator");

        Iterator<Item> itemIterator = searchService.iteratorSearch(context, dso, discoverQuery);
        handler.logDebug("creating dspacecsv");
        DSpaceCSV dSpaceCSV = metadataDSpaceCsvExportService.export(context, itemIterator, true);
        handler.logDebug("writing to file " + getFileNameOrExportFile());
        handler.writeFilestream(context, getFileNameOrExportFile(), dSpaceCSV.getInputStream(), EXPORT_CSV);
        context.restoreAuthSystemState();
        context.complete();

    }

    protected void loghelpinfo() {
        handler.logInfo("metadata-export");
    }

    protected String getFileNameOrExportFile() {
        return "metadataExportSearch.csv";
    }

    public IndexableObject resolveScope(Context context, String id) throws SQLException {
        UUID uuid = UUID.fromString(id);
        IndexableObject scopeObj = new IndexableCommunity(communityService.find(context, uuid));
        if (scopeObj.getIndexedObject() == null) {
            scopeObj = new IndexableCollection(collectionService.find(context, uuid));
        }
        return scopeObj;
    }
}
