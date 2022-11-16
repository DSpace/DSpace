package edu.umd.lib.dspace.content;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link EmbargoListExport} script
 */
public class EmbargoListExportScriptConfiguration<T extends EmbargoListExport> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableclass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableclass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableclass = dspaceRunnableClass;
    }

    @Override
    public boolean isAllowedToExecute(Context context) {
        return true;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();
            options.addOption("h", "help", false, "help");
            super.options = options;
        }
        return options;
    }
}
