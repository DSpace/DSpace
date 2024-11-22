/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.testing;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

public class TestingScriptConfiguration<T extends Testing> extends ScriptConfiguration<T> {

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
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();
            options.addOption("h", "help", false, "Display help message");
            super.options =  options;
        }
        return options;
    }
}
