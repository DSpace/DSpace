/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.impl;

import org.apache.commons.cli.ParseException;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;

public class MockDSpaceRunnableScript<T extends ScriptConfiguration<?>> extends DSpaceRunnable<T> {

    /**
     * Constructor for the MockDSpaceRunnableScript
     *
     * @param scriptConfiguration
     */
    public MockDSpaceRunnableScript(T scriptConfiguration) {
        super(scriptConfiguration);
    }

    @Override
    public void internalRun() {
        handler.logInfo("Logging INFO for Mock DSpace Script");
        handler.logError("Logging ERROR for Mock DSpace Script");
        handler.logWarning("Logging WARNING for Mock DSpace Script");
        handler.logDebug("Logging DEBUG for Mock DSpace Script");
    }

    @Override
    public void setup() throws ParseException {
        if (!commandLine.hasOption("i")) {
            throw new ParseException("-i is a mandatory parameter");
        }
    }
}
