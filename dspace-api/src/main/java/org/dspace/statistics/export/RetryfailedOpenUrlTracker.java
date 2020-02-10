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
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.OpenUrlService;

/**
 * Script to retry the failed url transmissions to IRUS
 * This script also has an option to add new failed urls for testing purposes
 */
public class RetryfailedOpenUrlTracker extends DSpaceRunnable {

    private Context context = null;
    private String lineToAdd = null;
    private boolean help = false;
    private boolean retryFailed = false;

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
            handler.logInfo("Created dummy entry in OpenUrlTracker with URL: " + lineToAdd);
        }
        if (retryFailed) {
            handler.logInfo("Reprocessing failed URLs stored in the db");
            openUrlService.reprocessFailedQueue(context);
        }
        context.restoreAuthSystemState();
        try {
            context.complete();
        } catch (Exception e) {
            handler.logError(e.getMessage());
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

        if (!(commandLine.hasOption('a') || commandLine.hasOption('r') || commandLine.hasOption('h'))) {
            throw new ParseException("At least one of the parameters (-a, -r, -h) is required!");
        }


        if (commandLine.hasOption('h')) {
            help = true;
        }
        if (commandLine.hasOption('a')) {
            lineToAdd = commandLine.getOptionValue('a');
        }
        if (commandLine.hasOption('r')) {
            retryFailed = true;
        }
    }

    private RetryfailedOpenUrlTracker() {
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

        options.addOption("r", false, "Retry sending requests to all urls stored in the table with failed requests. " +
                "This includes the url that can be added through the -a option.");
        options.getOption("r").setType(boolean.class);

        options.addOption("h", "help", false, "print this help message");
        options.getOption("h").setType(boolean.class);

        return options;
    }

}
