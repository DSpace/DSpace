/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import java.util.Iterator;

import org.apache.commons.cli.ParseException;
import org.dspace.content.Item;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

public class MetadataExportSearch extends DSpaceRunnable<MetadataExportSearchScriptConfiguration> {
    private static final String EXPORT_CSV = "exportCSV";
    private boolean help = false;
    private DiscoverQuery discoverQuery = new DiscoverQuery();
    private String identifier;


    private SearchService searchService =
        new DSpace().getServiceManager().getServicesByType(SearchService.class).get(0);
    private MetadataDSpaceCsvExportService metadataDSpaceCsvExportService = new DSpace().getServiceManager()
        .getServicesByType(MetadataDSpaceCsvExportService.class).get(0);
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();


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
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            loghelpinfo();
            printHelp();
            return;
        }

        Context context = new Context();
        context.turnOffAuthorisationSystem();
        context.setCurrentUser(ePersonService.find(context, this.getEpersonIdentifier()));
        Iterator<Item> itemIterator = searchService.iteratorSearch(context, discoverQuery);
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
}
