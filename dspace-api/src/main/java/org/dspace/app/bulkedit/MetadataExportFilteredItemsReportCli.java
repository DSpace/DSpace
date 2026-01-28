/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The CLI version of the {@link MetadataExportFilteredItemsReport} script
 *
 * @author Jean-François Morin (Université Laval)
 */
public class MetadataExportFilteredItemsReportCli<T extends ScriptConfiguration<?>>
    extends MetadataExportFilteredItemsReport<T> {


    /**
     * Constructor for MetadataExportFilteredItemsReportCli.
     * Command-line interface wrapper for MetadataExportFilteredItemsReport script.
     * 
     * @param scriptConfiguration The CLI script configuration with command-line options
     */
    public MetadataExportFilteredItemsReportCli(T scriptConfiguration) {
        super(scriptConfiguration);
    }

    @Override
    protected String getFileNameOrExportFile() {
        return Optional.ofNullable(commandLine.getOptionValue('n'))
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> super.getFileNameOrExportFile());
    }

}
