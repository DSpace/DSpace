/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link IdentifyFormatsScript}.
 */
public class IdentifyFormatsScriptConfiguration<T extends IdentifyFormatsScript> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {

            Options options = new Options();

            options.addOption("i", "identifier", true,
                "handle or UUID of the community, collection or item to process; "
                    + "if omitted, the whole repository is processed");

            options.addOption("a", "all", false,
                "re-identify every bitstream, not only those currently stored as Unknown");
            options.getOption("a").setType(boolean.class);

            options.addOption("h", "help", false, "help");

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
