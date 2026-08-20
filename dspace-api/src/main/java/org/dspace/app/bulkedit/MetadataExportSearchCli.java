/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The cli version of the {@link MetadataExportSearch} script
 */
public class MetadataExportSearchCli<T extends ScriptConfiguration<?>> extends MetadataExportSearch<T> {

    /**
     * Constructor for MetadataExportSearchCli.
     * Command-line interface wrapper for MetadataExportSearch script.
     * 
     * @param scriptConfiguration The CLI script configuration with command-line options
     */
    public MetadataExportSearchCli(T scriptConfiguration) {
        super(scriptConfiguration);
    }

    @Override
    protected String getFileNameOrExportFile() {
        return commandLine.getOptionValue('n');
    }
}
