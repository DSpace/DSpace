/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

/**
 * The cli version of the {@link MetadataExportSearch} script
 */
public class MetadataExportSearchCli extends MetadataExportSearch {

    @Override
    protected String getFileNameOrExportFile() {
        return commandLine.getOptionValue('n');
    }
}
