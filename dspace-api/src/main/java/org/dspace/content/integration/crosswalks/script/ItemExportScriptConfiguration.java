/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.script;

import java.util.List;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * {@link ScriptConfiguration} for the {@link ItemExport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 * @param <T> the ItemExport type
 */
public class ItemExportScriptConfiguration<T extends ItemExport> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        return true;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("i", "id", true, "the ID of the item to export");
            options.getOption("i").setType(String.class);
            options.getOption("i").setRequired(true);

            options.addOption("f", "format", true, "the format in which the item is to be exported");
            options.getOption("f").setType(String.class);
            options.getOption("f").setRequired(true);

            options.addOption("n", "name", true, "the name of the file to generate");
            options.getOption("n").setType(String.class);
            options.getOption("n").setRequired(false);

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
