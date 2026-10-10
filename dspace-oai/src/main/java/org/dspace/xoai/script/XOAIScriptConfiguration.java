/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.script;

import org.apache.commons.cli.Options;
import org.dspace.curate.Curation;
import org.dspace.scripts.configuration.ScriptConfiguration;

public class XOAIScriptConfiguration<T extends Curation> extends ScriptConfiguration<T> {
    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return this.dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            super.options = XOAIScriptOptions.constructOptions();
        }
        return options;
    }
}
