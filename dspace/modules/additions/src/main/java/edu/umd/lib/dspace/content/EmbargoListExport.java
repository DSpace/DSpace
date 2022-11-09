package edu.umd.lib.dspace.content;

import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.types.CharSet;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.util.StringInputStream;
import com.opencsv.CSVWriter;

import edu.umd.lib.dspace.content.factory.DrumServiceFactory;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;

/**
 * Metadata exporter to allow the batch export of metadata from a discovery search into a file
 *
 */
public class EmbargoListExport extends DSpaceRunnable<EmbargoListExportScriptConfiguration> {
    private static final String EXPORT_CSV = "exportCSV";
    private boolean help = false;
    // private String identifier;
    // private String discoveryConfigName;
    // private String[] filterQueryStrings;
    // private boolean hasScope = false;
    // private String query;

    // private SearchService searchService;
    // private MetadataDSpaceCsvExportService metadataDSpaceCsvExportService;
    private EPersonService ePersonService;
    // private DiscoveryConfigurationService discoveryConfigurationService;
    // private CommunityService communityService;
    // private CollectionService collectionService;
    // private DiscoverQueryBuilder queryBuilder;

    private EmbargoDTOService embargoService;

    @Override
    public EmbargoListExportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("embargo-list-export", EmbargoListExportScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        // searchService = SearchUtils.getSearchService();
        // metadataDSpaceCsvExportService = new DSpace().getServiceManager()
        //                                              .getServiceByName(
        //                                                  MetadataDSpaceCsvExportServiceImpl.class.getCanonicalName(),
        //                                                  MetadataDSpaceCsvExportService.class
        //                                              );
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        embargoService = DrumServiceFactory.getInstance().getEmbargoDTOService();
        // discoveryConfigurationService = SearchUtils.getConfigurationService();
        // communityService = ContentServiceFactory.getInstance().getCommunityService();
        // collectionService = ContentServiceFactory.getInstance().getCollectionService();
        // queryBuilder = SearchUtils.getQueryBuilder();

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        // if (commandLine.hasOption('q')) {
        //     query = commandLine.getOptionValue('q');
        // }

        // if (commandLine.hasOption('s')) {
        //     hasScope = true;
        //     identifier = commandLine.getOptionValue('s');
        // }

        // if (commandLine.hasOption('c')) {
        //     discoveryConfigName = commandLine.getOptionValue('c');
        // }

        // if (commandLine.hasOption('f')) {
        //     filterQueryStrings = commandLine.getOptionValues('f');
        // }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            loghelpinfo();
            printHelp();
            return;
        }
        handler.logDebug("starting search export");

        Context context = new Context();
        context.setCurrentUser(ePersonService.find(context, this.getEpersonIdentifier()));
        List<EmbargoDTO> embargoes = embargoService.getEmbargoList(context);


        StringWriter stringWriter = new StringWriter();
        try(CSVWriter writer = new CSVWriter(stringWriter)) {
          writer.writeNext(new String[] { "Handle", "Item ID", "Bitstream ID",
                  "Title", "Advisor", "Author", "Department", "Type", "End Date" });



          for (EmbargoDTO embargo : embargoes)
          {
              String[] entryData = new String[9];

              entryData[0] = embargo.getHandle();
              entryData[1] = embargo.getItemIdString();
              entryData[2] = embargo.getBitstreamIdString();
              entryData[3] = embargo.getTitle();
              entryData[4] = embargo.getAdvisor();
              entryData[5] = embargo.getAuthor();
              entryData[6] = embargo.getDepartment();
              entryData[7] = embargo.getType();
              entryData[8] = embargo.getEndDateString();
              writer.writeNext(entryData);
          }
          writer.flush();
        }
        stringWriter.close();

        InputStream inputStream = IOUtils.toInputStream(stringWriter.getBuffer(), StandardCharsets.UTF_8);
        handler.logDebug("writing to file " + getFileNameOrExportFile());
        handler.writeFilestream(context, getFileNameOrExportFile(), inputStream, EXPORT_CSV);
        context.restoreAuthSystemState();
        context.complete();

        // if (hasScope) {
        //     dso = resolveScope(context, identifier);
        // }

        // DiscoveryConfiguration discoveryConfiguration =
        //     discoveryConfigurationService.getDiscoveryConfiguration(discoveryConfigName);

        // List<QueryBuilderSearchFilter> queryBuilderSearchFilters = new ArrayList<>();

        // handler.logDebug("processing filter queries");
        // if (filterQueryStrings != null) {
        //     for (String filterQueryString: filterQueryStrings) {
        //         String field = filterQueryString.split(",", 2)[0];
        //         String operator = filterQueryString.split("(,|=)", 3)[1];
        //         String value = filterQueryString.split("=", 2)[1];
        //         QueryBuilderSearchFilter queryBuilderSearchFilter =
        //             new QueryBuilderSearchFilter(field, operator, value);
        //         queryBuilderSearchFilters.add(queryBuilderSearchFilter);
        //     }
        // }
        // handler.logDebug("building query");
        // DiscoverQuery discoverQuery =
        //     queryBuilder.buildQuery(context, dso, discoveryConfiguration, query, queryBuilderSearchFilters,
        //     "Item", 10, Long.getLong("0"), null, SortOption.DESCENDING);
        // handler.logDebug("creating iterator");

        // Iterator<Item> itemIterator = searchService.iteratorSearch(context, dso, discoverQuery);
        // handler.logDebug("creating dspacecsv");
        // DSpaceCSV dSpaceCSV = metadataDSpaceCsvExportService.export(context, itemIterator, true);
        // handler.logDebug("writing to file " + getFileNameOrExportFile());
        // handler.writeFilestream(context, getFileNameOrExportFile(), dSpaceCSV.getInputStream(), EXPORT_CSV);
        // context.restoreAuthSystemState();
        // context.complete();

    }

    protected void loghelpinfo() {
        handler.logInfo("embargo-list-export");
    }

    protected String getFileNameOrExportFile() {
        return "embargo-list.csv";
    }

    // public IndexableObject resolveScope(Context context, String id) throws SQLException {
    //     UUID uuid = UUID.fromString(id);
    //     IndexableObject scopeObj = new IndexableCommunity(communityService.find(context, uuid));
    //     if (scopeObj.getIndexedObject() == null) {
    //         scopeObj = new IndexableCollection(collectionService.find(context, uuid));
    //     }
    //     return scopeObj;
    // }
}
