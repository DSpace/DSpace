/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.io.InputStream;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.impl.MockDSpaceRunnableScript;

public class MockDSpaceRunnableScriptConfiguration<T extends MockDSpaceRunnableScript> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Generic setter for the dspaceRunnableClass
     * @param dspaceRunnableClass   The dspaceRunnableClass to be set on this MetadataExportScriptConfiguration
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("r", "remove", true, "description r");
            options.addOption("i", "index", false, "description i");
            options.getOption("i").setRequired(true);
            options.addOption("f", "file", true, "source file");
            options.getOption("f").setType(InputStream.class);
            options.getOption("f").setRequired(false);
            super.options = options;
        }
        return options;
    }
}
