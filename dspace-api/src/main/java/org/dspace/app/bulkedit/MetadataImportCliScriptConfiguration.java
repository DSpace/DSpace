/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link org.dspace.app.bulkedit.MetadataImportCLI} CLI script
 */
public class MetadataImportCliScriptConfiguration extends MetadataImportScriptConfiguration<MetadataImportCLI> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("e", "email", true, "email address or user id of user (required if adding new items)");
        options.getOption("e").setRequired(true);
        super.options = options;
        return options;
    }
}
