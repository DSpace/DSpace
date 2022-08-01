/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.scripts.handler.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.ProcessStatus;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessLogLevel;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.scripts.service.ProcessService;
import org.dspace.utils.DSpace;
import org.springframework.core.task.TaskExecutor;

/**
 * The {@link DSpaceRunnableHandler} dealing with Scripts started from the REST api
 */
public class RestDSpaceRunnableHandler implements DSpaceRunnableHandler {
    private static final Logger log = org.apache.logging.log4j.LogManager
        .getLogger(RestDSpaceRunnableHandler.class);

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private ProcessService processService = ScriptServiceFactory.getInstance().getProcessService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    private Integer processId;
    private String scriptName;
    private UUID ePersonId;

    /**
     * This constructor will initialise the handler with the process created from the parameters
     * @param ePerson       The eperson that creates the process
     * @param scriptName    The name of the script for which is a process will be created
     * @param parameters    The parameters for this process
     * @param specialGroups specialGroups The list of special groups related to eperson
     *                      creating process at process creation time
     */
    public RestDSpaceRunnableHandler(EPerson ePerson, String scriptName, List<DSpaceCommandLineParameter> parameters,
                                     final Set<Group> specialGroups) {
        Context context = new Context();
        try {
            ePersonId = ePerson.getID();
            Process process = processService.create(context, ePerson, scriptName, parameters, specialGroups);
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
            logInfo("The script has completed");

            addLogBitstreamToProcess(context);

            context.complete();
        } catch (SQLException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not be completed", e);
        } catch (IOException | AuthorizeException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not be completed due to an " +
                              "error with the logging bitstream", e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
        logError(message, e);

        Context context = new Context();
        try {
            Process process = processService.find(context, processId);
            processService.fail(context, process);

            addLogBitstreamToProcess(context);
            context.complete();
        } catch (SQLException sqlException) {
            log.error("SQL exception while handling another exception", e);
        } catch (IOException | AuthorizeException ioException) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not be completed due to an " +
                              "error with the logging bitstream", e);
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }

        // Make sure execution actually ends after we handle the exception
        throw new RuntimeException(e);
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

        appendLogToProcess(message, ProcessLogLevel.INFO);
    }

    @Override
    public void logWarning(String message) {
        String logMessage = getLogMessage(message);
        log.warn(logMessage);

        appendLogToProcess(message, ProcessLogLevel.WARNING);
    }

    @Override
    public void logError(String message) {
        String logMessage = getLogMessage(message);
        log.error(logMessage);

        appendLogToProcess(message, ProcessLogLevel.ERROR);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        String logMessage = getLogMessage(message);
        log.error(logMessage, throwable);

        appendLogToProcess(message, ProcessLogLevel.ERROR);
        if (throwable != null) {
            appendLogToProcess(ExceptionUtils.getStackTrace(throwable), ProcessLogLevel.ERROR);
        }
    }

    @Override
    public void printHelp(Options options, String name) {
        if (options != null) {
            HelpFormatter formatter = new HelpFormatter();
            StringWriter out = new StringWriter();
            PrintWriter pw = new PrintWriter(out);
            formatter.printHelp(pw, 1000, name, null, options, formatter.getLeftPadding(), formatter.getDescPadding(),
                                null, false);
            pw.flush();

            String helpString = out.toString();

            logInfo(helpString);
        }
    }

    @Override
    public Optional<InputStream> getFileStream(Context context, String fileName) throws IOException,
        AuthorizeException {
        try {
            Process process = processService.find(context, processId);
            Bitstream bitstream = processService.getBitstreamByName(context, process, fileName);
            InputStream inputStream = bitstreamService.retrieve(context, bitstream);
            if (inputStream == null) {
                return Optional.empty();
            } else {
                return Optional.of(inputStream);
            }
        } catch (SQLException sqlException) {
            log.error("SQL exception while attempting to find process", sqlException);
        }
        return null;
    }

    @Override
    public void writeFilestream(Context context, String fileName, InputStream inputStream, String type)
        throws IOException, SQLException, AuthorizeException {
        Process process = processService.find(context, processId);
        processService.appendFile(context, process, inputStream, type, fileName);
    }

    /**
     * This method will return the process created by this handler
     * @return The Process database object created by this handler
     * @param context
     */
    public Process getProcess(Context context) {
        try {
            return processService.find(context, processId);
        } catch (SQLException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not be found", e);
        }
        return null;
    }

    /**
     * This method will schedule a process to be run, it will trigger the run method for the Script passed along
     * to this method as well as updating the database logic for the Process representing the execution of this script
     * @param script    The script to be ran
     */
    public void schedule(DSpaceRunnable script) {
        TaskExecutor taskExecutor = new DSpace().getServiceManager()
                                                .getServiceByName("dspaceRunnableThreadExecutor", TaskExecutor.class);
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
        taskExecutor.execute(script);
    }

    private void appendLogToProcess(String message, ProcessLogLevel error) {
        try {
            processService.appendLog(processId, scriptName, message, error);
        }  catch (IOException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not write log to process", e);
        }
    }

    private void addLogBitstreamToProcess(Context context) throws SQLException, IOException, AuthorizeException {
        try {
            EPerson ePerson = ePersonService.find(context, ePersonId);
            Process process = processService.find(context, processId);

            context.setCurrentUser(ePerson);
            processService.createLogBitstream(context, process);
        } catch (SQLException | IOException | AuthorizeException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not write log to process", e);
        }
    }

    @Override
    public List<UUID> getSpecialGroups() {
        Context context = new Context();
        List<UUID> specialGroups = new ArrayList<>();
        try {
            Process process = processService.find(context, processId);
            for (Group group : process.getGroups()) {
                specialGroups.add(group.getID());
            }
        } catch (SQLException e) {
            log.error("RestDSpaceRunnableHandler with process: " + processId + " could not find the process", e);
        } finally {
            if (context.isValid()) {
                context.abort();
            }
        }
        return specialGroups;
    }
}
