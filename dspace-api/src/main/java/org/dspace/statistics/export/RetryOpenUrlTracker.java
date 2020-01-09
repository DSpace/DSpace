/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;


public class RetryOpenUrlTracker extends DSpaceRunnable {
    private static final Logger log = Logger.getLogger(RetryOpenUrlTracker.class);

    private Context context = null;
    private String lineToAdd = null;
    private boolean help = false;

    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }
        context.turnOffAuthorisationSystem();

        if (StringUtils.isNotBlank(lineToAdd)) {
            ExportUsageEventListener.logfailed(context, lineToAdd);
            log.info("Created dummy entry in OpenUrlTracker with URL: " + lineToAdd);
        } else {
            ExportUsageEventListener.reprocessFailedQueue(context);
        }
        context.restoreAuthSystemState();
        try {
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void setup() throws ParseException {
        context = new Context();

        if (commandLine.hasOption('h')) {
            help = true;
        }
        if (commandLine.hasOption('a')) {
            lineToAdd = commandLine.getOptionValue('a');
        }
    }

    private RetryOpenUrlTracker() {
        Options options = constructOptions();
        this.options = options;
    }

    private Options constructOptions() {
        Options options = new Options();

        options.addOption("a", true, "Add a new \"failed\" row to the table with a url (test purposes only)");
        options.getOption("a").setType(String.class);

        options.addOption("h", "help", false, "print this help message");
        options.getOption("h").setType(boolean.class);

        return options;
    }

}
