package org.dspace.app.statistics;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 *
 * @author mwood
 * @param <T> configuration for this class
 */
public class GetGithubReleaseScriptConfiguration<T extends GetGithubRelease>
        extends ScriptConfiguration<T> {
    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public boolean isAllowedToExecute(Context context) {
        return true;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            super.options = GetGithubReleaseOptions.constructOptions();
        }
        return options;
    }
}
