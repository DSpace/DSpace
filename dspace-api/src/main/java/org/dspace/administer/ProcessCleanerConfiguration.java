/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link ProcessCleaner} script.
 */
public class ProcessCleanerConfiguration<T extends ProcessCleaner> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {

            Options options = new Options();

            options.addOption("h", "help", false, "help");

            options.addOption("r", "running", false, "delete the process with RUNNING status");
            options.getOption("r").setType(boolean.class);

            options.addOption("f", "failed", false, "delete the process with FAILED status");
            options.getOption("f").setType(boolean.class);

            options.addOption("c", "completed", false,
                "delete the process with COMPLETED status (default if no statuses are specified)");
            options.getOption("c").setType(boolean.class);

            super.options = options;
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}
