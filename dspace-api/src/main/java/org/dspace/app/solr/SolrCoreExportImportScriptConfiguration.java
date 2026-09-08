/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration for SOLR core export/import operations.
 * Allows complete export and import of SOLR cores with multithreading support.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class SolrCoreExportImportScriptConfiguration<T extends SolrCoreExportImport> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("m", "mode", true,
                "Operation mode: 'export' to export core data, 'import' to import core data (required)");

            options.addOption("c", "core", true,
                "SOLR core name to export from or import to (required)");

            options.addOption("d", "directory", true,
                "Directory path for export/import files (required)");

            options.addOption("f", "format", true,
                "File format: 'csv' or 'json' (default: csv)");

            options.addOption("t", "threads", true,
                "Number of threads for parallel processing (default: 1)");

            options.addOption("s", "start-date", true,
                "Start date for export (format: 2023-01-01). If not specified, will be retrieved from SOLR");

            options.addOption("e", "end-date", true,
                "End date for export (format: 2023-12-31). If not specified, will be retrieved from SOLR");

            options.addOption("i", "increment", true,
                "Date increment for range splitting: WEEK, MONTH, or YEAR (default: MONTH)");

            options.addOption("h", "help", false, "Display help information");

            super.options = options;
        }
        return options;
    }
}
