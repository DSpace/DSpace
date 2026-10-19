/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.io.InputStream;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link MetadataImport} script
 */
public class MetadataImportScriptConfiguration<T extends MetadataImport> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Generic setter for the dspaceRunnableClass
     * @param dspaceRunnableClass   The dspaceRunnableClass to be set on this MetadataImportScriptConfiguration
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("f", "file", true, "source file");
            options.getOption("f").setType(InputStream.class);
            options.getOption("f").setRequired(true);
            options.addOption("s", "silent", false,
                              "silent operation - doesn't request confirmation of changes USE WITH CAUTION");
            options.addOption("w", "workflow", false, "workflow - when adding new items, use collection workflow");
            options.addOption("n", "notify", false,
                              "notify - when adding new items using a workflow, send notification emails");
            options.addOption("v", "validate-only", false,
                              "validate - just validate the csv, don't run the import");
            options.addOption("t", "template", false,
                              "template - when adding new items, use the collection template (if it exists)");
            options.addOption("h", "help", false, "help");

            super.options = options;
        }
        return options;
    }
}
