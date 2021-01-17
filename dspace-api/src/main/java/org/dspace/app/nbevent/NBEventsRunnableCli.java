/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.dspace.utils.DSpace;

public class NBEventsRunnableCli extends NBEventsRunnable {

    @Override
    @SuppressWarnings({ "rawtypes" })
    public NBEventsCliScriptConfiguration getScriptConfiguration() {
        NBEventsCliScriptConfiguration configuration = new DSpace().getServiceManager()
                .getServiceByName("import-nbevents", NBEventsCliScriptConfiguration.class);
        return configuration;
    }

    @Override
    public void setup() throws ParseException {
        super.setup();

        // in case of CLI we show the help prompt
        if (commandLine.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Import Notification event json file", getScriptConfiguration().getOptions());
            System.exit(0);
        }
    }

    /**
     * Get the events input stream from a local file.
     */
    @Override
    protected InputStream getNBEventsInputStream() throws Exception {
        return new FileInputStream(new File(fileLocation));
    }

}
