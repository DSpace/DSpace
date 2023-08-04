/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.scripts;

import java.io.InputStream;

import org.apache.commons.cli.Options;
import org.dspace.app.rest.converter.ScriptConverter;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script configuration used to test the type conversion in the {@link ScriptConverter}
 */
public class TypeConversionTestScriptConfiguration<T extends TypeConversionTestScript> extends ScriptConfiguration<T> {


    public Class<T> getDspaceRunnableClass() {
        return null;
    }

    public void setDspaceRunnableClass(final Class<T> dspaceRunnableClass) {

    }

    public boolean isAllowedToExecute(final Context context) {
        return true;
    }

    public Options getOptions() {

        Options options = new Options();

        options.addOption("b", "boolean", false, "option set to the boolean class");
        options.addOption("s", "string", true, "string option with an argument");
        options.addOption("n", "noargument", false, "string option without an argument");
        options.addOption("f", "file", true, "file option with an argument");
        options.getOption("f").setType(InputStream.class);

        return options;
    }
}
