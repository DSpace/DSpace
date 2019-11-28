/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.impl;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.scripts.DSpaceRunnable;

public class MockDSpaceRunnableScript extends DSpaceRunnable {

    private MockDSpaceRunnableScript() {
        Options options = constructOptions();
        this.options = options;
    }

    @Override
    public void internalRun() throws Exception {
    }

    @Override
    public void setup() throws ParseException {
        if (!commandLine.hasOption("i")) {
            throw new ParseException("-i is a mandatory parameter");
        }
    }

    private Options constructOptions() {
        Options options = new Options();

        options.addOption("r", "remove", true, "description r");
        options.getOption("r").setType(String.class);
        options.addOption("i", "index", false, "description i");
        options.getOption("i").setType(boolean.class);
        options.getOption("i").setRequired(true);
        return options;
    }
}
