/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import org.apache.commons.cli.Options;

/**
 * This is the CLI version of the {@link CurationScriptConfiguration} class that handles the configuration for the
 * {@link CurationCli} script
 */
public class CurationCliScriptConfiguration extends CurationScriptConfiguration<Curation> {

    @Override
    public Options getOptions() {
        options = super.getOptions();
        options.addOption("e", "eperson", true, "email address of curating eperson");
        options.getOption("e").setRequired(true);
        options.addOption("r", "reporter", true,
            "relative or absolute path to the desired report file. Use '-' to report to console. If absent, no " +
            "reporting");
        options.addOption("T", "taskfile", true, "file containing curation task names");
        return options;
    }
}
