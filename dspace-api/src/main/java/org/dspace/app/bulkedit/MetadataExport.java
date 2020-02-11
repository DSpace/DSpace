/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.io.OutputStream;
import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.core.Context;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Metadata exporter to allow the batch export of metadata into a file
 *
 * @author Stuart Lewis
 */
public class MetadataExport extends DSpaceRunnable {

    private Context context = null;
    private boolean help = false;
    private String filename = null;
    private String handle = null;
    private boolean exportAllMetadata = false;
    private boolean exportAllItems = false;

    @Autowired
    private MetadataDSpaceCsvExportService metadataDSpaceCsvExportService;

    @Autowired
    private EPersonService ePersonService;

    private MetadataExport() {
        this.options = constructOptions();
    }

    private Options constructOptions() {
        Options options = new Options();

        options.addOption("i", "id", true, "ID or handle of thing to export (item, collection, or community)");
        options.getOption("i").setType(String.class);
        options.addOption("f", "file", true, "destination where you want file written");
        options.getOption("f").setType(OutputStream.class);
        options.getOption("f").setRequired(true);
        options.addOption("a", "all", false,
                          "include all metadata fields that are not normally changed (e.g. provenance)");
        options.getOption("a").setType(boolean.class);
        options.addOption("h", "help", false, "help");
        options.getOption("h").setType(boolean.class);


        return options;
    }

    public void internalRun() throws Exception {
        if (help) {
            handler.logInfo("\nfull export: metadata-export -f filename");
            handler.logInfo("partial export: metadata-export -i handle -f filename");
            printHelp();
            return;
        }

        DSpaceCSV dSpaceCSV = metadataDSpaceCsvExportService
            .handleExport(context, exportAllItems, exportAllMetadata, handle,
                          handler);
        handler.writeFilestream(context, filename, dSpaceCSV.getInputStream(), "exportCSV");
        context.restoreAuthSystemState();
        context.complete();
    }

    public void setup() throws ParseException {
        context = new Context();
        context.turnOffAuthorisationSystem();

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        // Check a filename is given
        if (!commandLine.hasOption('f')) {
            throw new ParseException("Required parameter -f missing!");
        }
        filename = commandLine.getOptionValue('f');

        exportAllMetadata = commandLine.hasOption('a');

        if (!commandLine.hasOption('i')) {
            exportAllItems = true;
        }
        handle = commandLine.getOptionValue('i');

        try {
            context.setCurrentUser(ePersonService.find(context, getEpersonIdentifier()));
        } catch (SQLException e) {
            handler.handleException(e);
        }
    }
}
