/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import org.apache.commons.cli.Options;

/**
 * This is the CLI version of the {@link MetadataExportSearchScriptConfiguration} class that handles the
 * configuration for the {@link MetadataExportSearchCli} script
 */
public class MetadataExportSearchCliScriptConfiguration
    extends MetadataExportSearchScriptConfiguration<MetadataExportSearchCli> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("n", "filename", true, "the filename to export to");
        return super.getOptions();
    }
}
