/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.script;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration for {@link OrcidBulkPush}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 * @param  <T> the OrcidBulkPush type
 */
public class OrcidBulkPushScriptConfiguration<T extends OrcidBulkPush> extends ScriptConfiguration<T> {

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
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("f", "force", false, "force the synchronization ignoring maximum attempts");
            options.getOption("f").setType(boolean.class);
            options.getOption("f").setRequired(false);

            super.options = options;
        }
        return options;
    }

}
