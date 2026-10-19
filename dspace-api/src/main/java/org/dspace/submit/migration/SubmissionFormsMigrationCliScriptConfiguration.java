/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.migration;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link SubmissionFormsMigration} script
 *
 * @author Maria Verdonck (Atmire) on 13/11/2020
 */
public class SubmissionFormsMigrationCliScriptConfiguration<T extends SubmissionFormsMigration>
    extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return this.dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("f", "input-forms", true, "Path to source input-forms.xml file location");
            options.addOption("s", "item-submission", true, "Path to source item-submission.xml file location");
            options.addOption("h", "help", false, "help");

            super.options = options;
        }
        return options;
    }
}
