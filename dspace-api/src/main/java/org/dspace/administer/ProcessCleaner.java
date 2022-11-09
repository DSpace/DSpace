/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.time.DateUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.ProcessStatus;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.Process;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Script to cleanup the old processes in the specified state.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ProcessCleaner extends DSpaceRunnable<ProcessCleanerConfiguration<ProcessCleaner>> {

    private ConfigurationService configurationService;

    private ProcessService processService;


    private boolean cleanCompleted = false;

    private boolean cleanFailed = false;

    private boolean cleanRunning = false;

    private boolean help = false;

    private Integer days;


    @Override
    public void setup() throws ParseException {

        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.processService = ScriptServiceFactory.getInstance().getProcessService();

        this.help = commandLine.hasOption('h');
        this.cleanFailed = commandLine.hasOption('f');
        this.cleanRunning = commandLine.hasOption('r');
        this.cleanCompleted = commandLine.hasOption('c') || (!cleanFailed && !cleanRunning);

        this.days = configurationService.getIntProperty("process-cleaner.days", 14);

        if (this.days <= 0) {
            throw new IllegalStateException("The number of days must be a positive integer.");
        }

    }

    @Override
    public void internalRun() throws Exception {

        if (help) {
            printHelp();
            return;
        }

        Context context = new Context();

        try {
            context.turnOffAuthorisationSystem();
            performDeletion(context);
        } finally {
            context.restoreAuthSystemState();
            context.complete();
        }

    }

    /**
     * Delete the processes based on the specified statuses and the configured days
     * from their creation.
     */
    private void performDeletion(Context context) throws SQLException, IOException, AuthorizeException {

        List<ProcessStatus> statuses = getProcessToDeleteStatuses();
        Date creationDate = calculateCreationDate();

        handler.logInfo("Searching for processes with status: " + statuses);
        List<Process> processes = processService.findByStatusAndCreationTimeOlderThan(context, statuses, creationDate);
        handler.logInfo("Found " + processes.size() + " processes to be deleted");
        for (Process process : processes) {
            processService.delete(context, process);
        }

        handler.logInfo("Process cleanup completed");

    }

    /**
     * Returns the list of Process statuses do be deleted.
     */
    private List<ProcessStatus> getProcessToDeleteStatuses() {
        List<ProcessStatus> statuses = new ArrayList<ProcessStatus>();
        if (cleanCompleted) {
            statuses.add(ProcessStatus.COMPLETED);
        }
        if (cleanFailed) {
            statuses.add(ProcessStatus.FAILED);
        }
        if (cleanRunning) {
            statuses.add(ProcessStatus.RUNNING);
        }
        return statuses;
    }

    private Date calculateCreationDate() {
        return DateUtils.addDays(new Date(), -days);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProcessCleanerConfiguration<ProcessCleaner> getScriptConfiguration() {
        return new DSpace().getServiceManager()
            .getServiceByName("process-cleaner", ProcessCleanerConfiguration.class);
    }

}
