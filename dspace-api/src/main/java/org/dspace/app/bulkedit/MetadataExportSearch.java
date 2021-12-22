/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFilterQuery;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

public class MetadataExportSearch extends DSpaceRunnable<MetadataExportSearchScriptConfiguration> {
    private static final String EXPORT_CSV = "exportCSV";
    private boolean help = false;
    private DiscoverQuery discoverQuery = new DiscoverQuery();
    private String identifier;
    private String discoveryConfigName;
    private String advancedFilter;
    private String[] filterQueryStrings;
    private boolean exportAllItems = true;

    private SearchService searchService =
        new DSpace().getServiceManager().getServicesByType(SearchService.class).get(0);
    private MetadataDSpaceCsvExportService metadataDSpaceCsvExportService = new DSpace().getServiceManager()
        .getServicesByType(MetadataDSpaceCsvExportService.class).get(0);
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private DiscoveryConfigurationService discoveryConfigurationService =
        new DSpace().getServiceManager().getServicesByType(DiscoveryConfigurationService.class).get(0);
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    @Override
    public MetadataExportSearchScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("metadata-export-search", MetadataExportSearchCliScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        if (commandLine.hasOption('q')) {
            discoverQuery.setQuery(commandLine.getOptionValue('q'));
        }

        if (commandLine.hasOption('s')) {
            exportAllItems = false;
            identifier = commandLine.getOptionValue('s');
        }

        if (commandLine.hasOption('c')) {
            discoveryConfigName = commandLine.getOptionValue('c');
            discoverQuery.setDiscoveryConfigurationName(commandLine.getOptionValue('c'));
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

        IndexableObject dso = null;
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(ePersonService.find(context, this.getEpersonIdentifier()));

        if (! exportAllItems) {
            dso = resolveScope(context, identifier);
        }


        DiscoveryConfiguration discoveryConfiguration =
            discoveryConfigurationService.getDiscoveryConfiguration(discoveryConfigName);

        if (filterQueryStrings != null) {
            for(String filterQueryString: filterQueryStrings) {
                String field = filterQueryString.split(",", 2)[0];
                String operator = filterQueryString.split("('|=)", 3)[1];
                String value = filterQueryString.split("=", 2)[1];
                DiscoverFilterQuery filterQuery = searchService.toFilterQuery(
                    context,
                    field,
                    operator,
                    value,
                    discoveryConfiguration);
                discoverQuery.addFilterQueries(filterQuery.getFilterQuery());
            }
        }
        Iterator<Item> itemIterator = searchService.iteratorSearch(context, dso, discoverQuery);
        DSpaceCSV dSpaceCSV = metadataDSpaceCsvExportService.export(context, itemIterator, true);
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
