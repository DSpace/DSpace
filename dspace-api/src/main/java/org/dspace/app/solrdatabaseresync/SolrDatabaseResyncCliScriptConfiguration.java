/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solrdatabaseresync;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link SolrDatabaseResyncCli} script.
 */
public class SolrDatabaseResyncCliScriptConfiguration extends ScriptConfiguration<SolrDatabaseResyncCli> {
    private Class<SolrDatabaseResyncCli> dspaceRunnableClass;

    @Override
    public Class<SolrDatabaseResyncCli> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<SolrDatabaseResyncCli> dspaceRunnableClass) {
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
