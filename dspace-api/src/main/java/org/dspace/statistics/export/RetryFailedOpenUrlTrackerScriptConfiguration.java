/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link RetryFailedOpenUrlTracker} script
 */
public class RetryFailedOpenUrlTrackerScriptConfiguration<T extends RetryFailedOpenUrlTracker>
        extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Generic setter for the dspaceRunnableClass
     *
     * @param dspaceRunnableClass The dspaceRunnableClass to be set on this RetryFailedOpenUrlTrackerScriptConfiguration
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("a", true, "Add a new \"failed\" row to the table with a url (test purposes only)");

            options.addOption("r", false,
                              "Retry sending requests to all urls stored in the table with failed requests. " +
                                      "This includes the url that can be added through the -a option.");

            options.addOption("h", "help", false, "print this help message");

            super.options = options;
        }
        return options;
    }

}
