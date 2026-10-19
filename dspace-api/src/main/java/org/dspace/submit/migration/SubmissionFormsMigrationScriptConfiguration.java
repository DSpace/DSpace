/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.migration;

import java.util.List;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Subclass of {@link SubmissionFormsMigrationCliScriptConfiguration} to be use in rest/scripts.xml configuration so
 * this script is not runnable from REST
 *
 * @author Maria Verdonck (Atmire) on 05/01/2021
 */
public class SubmissionFormsMigrationScriptConfiguration<T extends SubmissionFormsMigration>
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

    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        // Script is not allowed to be executed from REST side
        return false;
    }
}
