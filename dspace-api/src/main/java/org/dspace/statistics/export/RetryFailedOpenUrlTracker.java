/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.statistics.export.factory.OpenURLTrackerLoggerServiceFactory;
import org.dspace.statistics.export.service.OpenUrlService;
import org.dspace.utils.DSpace;

/**
 * Script to retry the failed url transmissions to IRUS
 * This script also has an option to add new failed urls for testing purposes
 */
public class RetryFailedOpenUrlTracker extends DSpaceRunnable<RetryFailedOpenUrlTrackerScriptConfiguration> {

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
        Context context = new Context();
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
        context.complete();
    }

    public RetryFailedOpenUrlTrackerScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("retry-tracker",
                                                                 RetryFailedOpenUrlTrackerScriptConfiguration.class);
    }

    /**
     * Setups the parameters
     *
     * @throws ParseException
     */
    public void setup() throws ParseException {
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

}
