/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkedit;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link MetadataExportSearch} script
 */
public class MetadataExportSearchScriptConfiguration<T extends MetadataExportSearch> extends ScriptConfiguration<T> {

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
            options.addOption("q", "query", true,
                "The discovery search string to will be used to match records. Not URL encoded");
            options.getOption("q").setType(String.class);
            options.addOption("s", "scope", true,
                "UUID of a specific DSpace container (site, community or collection) to which the search has to be " +
                    "limited");
            options.getOption("s").setType(String.class);
            options.addOption("c", "configuration", true,
                "The name of a Discovery configuration that should be used by this search");
            options.getOption("c").setType(String.class);
            options.addOption("f", "filter", true,
                "Advanced search filter that has to be used to filter the result set, with syntax `<:filter-name>," +
                    "<:filter-operator>=<:filter-value>`. Not URL encoded. For example `author," +
                    "authority=5df05073-3be7-410d-8166-e254369e4166` or `title,contains=sample text`");
            options.getOption("f").setType(String.class);
            options.addOption("h", "help", false, "help");

            super.options =  options;
        }
        return options;
    }
}
