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

/**
 * The CLI version of the {@link MetadataExportFilteredItemsReport} script
 *
 * @author Jean-François Morin (Université Laval)
 */
public class MetadataExportFilteredItemsReportCli extends MetadataExportFilteredItemsReport {

    @Override
    protected String getFileNameOrExportFile() {
        return Optional.ofNullable(commandLine.getOptionValue('n'))
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> super.getFileNameOrExportFile());
    }

}
