/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Configuration for {@link ExportCrisLayoutToolScript}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class ExportCrisLayoutToolScriptConfiguration<T extends ExportCrisLayoutToolScript>
    extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {
            options = new Options();
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
