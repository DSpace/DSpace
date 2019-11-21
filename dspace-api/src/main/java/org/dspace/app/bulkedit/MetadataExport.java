/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.content.service.MetadataExportService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Metadata exporter to allow the batch export of metadata into a file
 *
 * @author Stuart Lewis
 */
public class MetadataExport extends DSpaceRunnable {

    private Context c = null;
    private boolean help = false;
    private String filename = null;
    private String handle = null;
    private boolean exportAllMetadata = false;
    private boolean exportAllItems = false;

    @Autowired
    private MetadataExportService metadataExportService;

    protected Context context;

    private MetadataExport() {
        Options options = constructOptions();
        this.options = options;
    }

    private Options constructOptions() {
        Options options = new Options();

        options.addOption("i", "id", true, "ID or handle of thing to export (item, collection, or community)");
        options.addOption("f", "file", true, "destination where you want file written");
        options.getOption("f").setRequired(true);
        options.addOption("a", "all", false,
                          "include all metadata fields that are not normally changed (e.g. provenance)");
        options.addOption("h", "help", false, "help");

        return options;
    }

    public void internalRun() throws Exception {
        if (help) {
            handler.logInfo("\nfull export: metadataexport -f filename");
            handler.logInfo("partial export: metadataexport -i handle -f filename");
            printHelp();
            return;
        }

        DSpaceCSV dSpaceCSV = metadataExportService.handleExport(c, exportAllItems, exportAllMetadata, handle);
        handler.writeFilestream(c, filename, dSpaceCSV.getInputStream(), "exportCSV");
        c.restoreAuthSystemState();
        c.complete();
    }

    public void setup() throws ParseException {
        c = new Context();
        c.turnOffAuthorisationSystem();

        if (commandLine.hasOption('h')) {
            help = true;
        }

        // Check a filename is given
        if (!commandLine.hasOption('f')) {
            throw new ParseException("Required parameter -f missing!");
        }
        filename = commandLine.getOptionValue('f');

        exportAllMetadata = commandLine.hasOption('a');

        if (commandLine.hasOption('i')) {
            exportAllItems = true;
        }
        handle = commandLine.getOptionValue('i');
    }
}
