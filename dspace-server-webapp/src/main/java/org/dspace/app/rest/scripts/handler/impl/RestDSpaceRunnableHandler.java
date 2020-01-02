/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.scripts.handler.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.ProcessStatus;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.Process;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.scripts.service.ProcessService;

/**
 * The {@link DSpaceRunnableHandler} dealing with Scripts started from the REST api
 */
public class RestDSpaceRunnableHandler implements DSpaceRunnableHandler {
    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(RestDSpaceRunnableHandler.class);

    private ProcessService processService = ScriptServiceFactory.getInstance().getProcessService();

    private Integer processId;
    private String scriptName;

    /**
     * This constructor will initialise the handler with the process created from the parameters
     * @param ePerson       The eperson that creates the process
     * @param scriptName    The name of the script for which is a process will be created
     * @param parameters    The parameters for this process
     */
    public RestDSpaceRunnableHandler(EPerson ePerson, String scriptName, List<DSpaceCommandLineParameter> parameters) {
        Context context = new Context();
        try {
            Process process = processService.create(context, ePerson, scriptName, parameters);
            processId = process.getID();
            this.scriptName = process.getName();

            context.complete();
        } catch (SQLException e) {
            log.error("RestDSpaceRunnableHandler with ePerson: " + ePerson
                .getEmail() + " for Script with name: " + scriptName +
                          " and parameters: " + parameters + " could nto be created", e);
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }
    }

    @Override
    public void start() {
        Context context = new Context();
        try {
            Process process = processService.find(context, processId);
            processService.start(context, process);
            context.complete();
            logInfo("The script has started");
        } catch (SQLException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not be started", e);
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }
    }

    @Override
    public void handleCompletion() {
        Context context = new Context();
        try {
            Process process = processService.find(context, processId);
            processService.complete(context, process);
            context.complete();
            logInfo("The script has completed");
        } catch (SQLException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not be completed", e);
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }
    }

    @Override
    public void handleException(Exception e) {
        handleException(null, e);
    }

    @Override
    public void handleException(String message) {
        handleException(message, null);
    }

    @Override
    public void handleException(String message, Exception e) {
        if (message != null) {
            logError(message);
        }
        if (e != null) {
            logError(ExceptionUtils.getStackTrace(e));
        }

        Context context = new Context();
        try {
            Process process = processService.find(context, processId);
            processService.fail(context, process);
            context.complete();
        } catch (SQLException sqlException) {
            log.error("SQL exception while handling another exception", e);
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }
    }

    @Override
    public void logDebug(String message) {
        String logMessage = getLogMessage(message);
        log.debug(logMessage);
    }

    private String getLogMessage(String message) {
        return String
            .format("Process id: %d, script name: %s, message: %s", processId, scriptName, message);
    }

    @Override
    public void logInfo(String message) {
        String logMessage = getLogMessage(message);
        log.info(logMessage);

    }

    @Override
    public void logWarning(String message) {
        String logMessage = getLogMessage(message);
        log.warn(logMessage);
    }

    @Override
    public void logError(String message) {
        String logMessage = getLogMessage(message);
        log.error(logMessage);
    }

    @Override
    public void printHelp(Options options, String name) {
        if (options != null) {
            HelpFormatter formatter = new HelpFormatter();
            StringWriter out = new StringWriter();
            PrintWriter pw = new PrintWriter(out);

            formatter.printUsage(pw, 1000, name, options);
            pw.flush();

            String helpString = out.toString();

            logInfo(helpString);
        }
    }

    /**
     * This method will return the process created by this handler
     * @return The Process database object created by this handler
     */
    public Process getProcess() {
        Context context = new Context();
        try {
            return processService.find(context, processId);
        } catch (SQLException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not be found", e);
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }
        return null;
    }

    /**
     * This method will schedule a process to be run, it will trigger the run method for the Script passed along
     * to this method as well as updating the database logic for the Process representing the execution of this script
     * @param script    The script to be ran
     */
    public void schedule(DSpaceRunnable script) {
        Context context = new Context();
        try {
            Process process = processService.find(context, processId);
            process.setProcessStatus(ProcessStatus.SCHEDULED);
            processService.update(context, process);
            context.complete();
        } catch (SQLException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " ran into an SQLException", e);
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }
        script.run();
    }
}
