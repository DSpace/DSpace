/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import org.apache.commons.cli.Options;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is the CLI version of the {@link MetadataExportFilteredItemsReportScriptConfiguration} class that handles the
 * configuration for the {@link MetadataExportFilteredItemsReportCli} script
 *
 * @author Jean-François Morin (Université Laval)
 */
public class MetadataExportFilteredItemsReportCliScriptConfiguration
    extends MetadataExportFilteredItemsReportScriptConfiguration<MetadataExportFilteredItemsReportCli> {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        String filename = configurationService.getProperty("contentreport.metadataquery.csv.filename.default",
                MetadataExportFilteredItemsReport.DEFAULT_FILENAME);
        options.addOption("n", "filename", true, "the filename to export to (default: " + filename + ")");
        return options;
    }

}
