/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import org.apache.commons.cli.Options;

/**
 * Extension of {@link OAIREPublicationLoaderScriptConfiguration} for CLI.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class OAIREPublicationLoaderCliScriptConfiguration<T extends OAIREPublicationLoaderRunnable>
    extends OAIREPublicationLoaderScriptConfiguration<T> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addOption("h", "help", false, "help");
        options.getOption("h").setType(boolean.class);
        super.options = options;
        return options;
    }

}
