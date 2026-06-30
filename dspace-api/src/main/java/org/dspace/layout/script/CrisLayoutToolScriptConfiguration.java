/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script;

import java.io.InputStream;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Configuration for {@link CrisLayoutToolScript}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisLayoutToolScriptConfiguration<T extends CrisLayoutToolScript> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("f", "file", true, "source file");
            options.getOption("f").setType(InputStream.class);
            options.getOption("f").setRequired(true);

            super.options = options;
        }
        return options;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

}
