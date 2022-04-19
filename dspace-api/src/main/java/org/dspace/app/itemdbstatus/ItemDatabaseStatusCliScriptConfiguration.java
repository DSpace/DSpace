package org.dspace.app.itemdbstatus;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Created by kristof on 19/04/2022
 */
public class ItemDatabaseStatusCliScriptConfiguration extends ScriptConfiguration<ItemDatabaseStatusCli> {
    private Class<ItemDatabaseStatusCli> dspaceRunnableClass;

    @Override
    public Class<ItemDatabaseStatusCli> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<ItemDatabaseStatusCli> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public boolean isAllowedToExecute(Context context) {
        return true;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            options = new Options();
        }
        return options;
    }
}
