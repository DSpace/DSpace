/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.script;

import org.apache.commons.cli.Options;
import org.dspace.app.suggestion.runnable.PublicationLoaderRunnable;


/**
 * Extension of {@link org.dspace.app.suggestion.openaire.PublicationLoaderScriptConfiguration} for CLI.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class PublicationLoaderCliScriptConfiguration<T extends PublicationLoaderRunnable>
    extends PublicationLoaderScriptConfiguration<T> {

    /**
     * Retrieves the command-line options available for this script.
     * In addition to the options provided by {@link PublicationLoaderScriptConfiguration},
     * this method adds a "help" option.
     *
     * @return The configured {@link Options} object containing all available command-line parameters.
     */
    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("h", "help", false, "help");
        options.getOption("h").setType(boolean.class);
        super.options = options;
        return options;
    }

}
