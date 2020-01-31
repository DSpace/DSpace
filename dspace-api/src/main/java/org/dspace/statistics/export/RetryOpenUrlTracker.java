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
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.OpenUrlService;

/**
 * Script to retry the failed url transmissions to IRUS
 * This script also has an option to add new failed urls for testing purposes
 */
public class RetryOpenUrlTracker extends DSpaceRunnable {
    private static final Logger log = Logger.getLogger(RetryOpenUrlTracker.class);

    private Context context = null;
    private String lineToAdd = null;
    private boolean help = false;

    private OpenUrlService openUrlService;

    /**
     * Run the script
     * When the -a option is used, a new "failed" url will be added to the database
     *
     * @throws Exception
     */
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }
        context.turnOffAuthorisationSystem();

        if (StringUtils.isNotBlank(lineToAdd)) {
            openUrlService.logfailed(context, lineToAdd);
            log.info("Created dummy entry in OpenUrlTracker with URL: " + lineToAdd);
        } else {
            openUrlService.reprocessFailedQueue(context);
        }
        context.restoreAuthSystemState();
        try {
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Setups the parameters
     *
     * @throws ParseException
     */
    public void setup() throws ParseException {
        context = new Context();
        openUrlService = OpenURLTrackerLoggerServiceFactory.getInstance().getOpenUrlService();

        lineToAdd = null;
        help = false;

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

    /**
     * Constructs the script options
     *
     * @return the constructed options
     */
    private Options constructOptions() {
        Options options = new Options();

        options.addOption("a", true, "Add a new \"failed\" row to the table with a url (test purposes only)");
        options.getOption("a").setType(String.class);

        options.addOption("h", "help", false, "print this help message");
        options.getOption("h").setType(boolean.class);

        return options;
    }

}
