/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Metadata exporter to allow the batch export of metadata into a file
 *
 * @author Stuart Lewis
 */
public class MetadataExport extends DSpaceRunnable<MetadataExportScriptConfiguration> {

    private boolean help = false;
    private String filename = null;
    private String handle = null;
    private boolean exportAllMetadata = false;
    private boolean exportAllItems = false;

    private static final String EXPORT_CSV = "exportCSV";

    private MetadataDSpaceCsvExportService metadataDSpaceCsvExportService = new DSpace().getServiceManager()
                .getServicesByType(MetadataDSpaceCsvExportService.class).get(0);

    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    @Override
    public void internalRun() throws Exception {

        if (help) {
            logHelpInfo();
            printHelp();
            return;
        }
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        try {
            context.setCurrentUser(ePersonService.find(context, this.getEpersonIdentifier()));
        } catch (SQLException e) {
            handler.handleException(e);
        }
        DSpaceCSV dSpaceCSV = metadataDSpaceCsvExportService
            .handleExport(context, exportAllItems, exportAllMetadata, handle,
                          handler);
        handler.writeFilestream(context, filename, dSpaceCSV.getInputStream(), EXPORT_CSV);
        context.restoreAuthSystemState();
        context.complete();
    }

    protected void logHelpInfo() {
        handler.logInfo("\nfull export: metadata-export");
        handler.logInfo("partial export: metadata-export -i handle");
    }

    @Override
    public MetadataExportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("metadata-export",
                                                                 MetadataExportScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        filename = getFileNameForExportFile();

        exportAllMetadata = commandLine.hasOption('a');

        if (!commandLine.hasOption('i')) {
            exportAllItems = true;
        }
        handle = commandLine.getOptionValue('i');
    }

    protected String getFileNameForExportFile() {
        return UUID.randomUUID().toString() + ".csv";
    }
}
