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
import org.dspace.scripts.MockDSpaceRunnableScriptConfiguration;
import org.dspace.utils.DSpace;

public class MockDSpaceRunnableScript extends DSpaceRunnable<MockDSpaceRunnableScriptConfiguration> {
    @Override
    public void internalRun() throws Exception {
        handler.logInfo("Logging INFO for Mock DSpace Script");
        handler.logError("Logging ERROR for Mock DSpace Script");
        handler.logWarning("Logging WARNING for Mock DSpace Script");
        handler.logDebug("Logging DEBUG for Mock DSpace Script");
    }

    @Override
    public MockDSpaceRunnableScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                           .getServiceByName("mock-script", MockDSpaceRunnableScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        if (!commandLine.hasOption("i")) {
            throw new ParseException("-i is a mandatory parameter");
        }
    }
}
