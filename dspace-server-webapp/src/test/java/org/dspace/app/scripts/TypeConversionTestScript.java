/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.scripts;

import org.apache.commons.cli.ParseException;
import org.dspace.app.rest.converter.ScriptConverter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Script used to test the type conversion in the {@link ScriptConverter}
 */
public class TypeConversionTestScript<T extends ScriptConfiguration<?>> extends DSpaceRunnable<T> {

    /**
     * Constructor for the TypeConversionTestScript
     *
     * @param scriptConfiguration
     */
    public TypeConversionTestScript(T scriptConfiguration) {
        super(scriptConfiguration);
    }

    public void setup() throws ParseException {
        // This script is only used to test rest exposure, no setup is required.
    }

    public void internalRun() throws Exception {
        // This script is only used to test rest exposure, no internal run is required.
    }
}
