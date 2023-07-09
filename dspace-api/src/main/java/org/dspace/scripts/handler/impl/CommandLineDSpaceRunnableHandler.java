/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.handler.impl;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This is an implementation for the CommandLineDSpaceRunnables which means that these implementations
 * are used by DSpaceRunnables which are called from the CommandLine
 */
public class CommandLineDSpaceRunnableHandler implements DSpaceRunnableHandler {
    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(CommandLineDSpaceRunnableHandler.class);

    private ProcessService processService;
    private EPersonService ePersonService;
    private ConfigurationService configurationService;

    String scriptName;

    Integer processId;
    UUID ePersonUUID;
    public CommandLineDSpaceRunnableHandler(){}
    public CommandLineDSpaceRunnableHandler(String scriptName, List<DSpaceCommandLineParameter> parameters) {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        if (isSaveEnabled()) {
            Context context = new Context();
            processService = ScriptServiceFactory.getInstance().getProcessService();
            ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
            try {
                EPerson ePerson = getEpersonProcess(context);
                this.ePersonUUID = ePerson.getID();
                Process process = processService.create(context, ePerson, scriptName, parameters, new HashSet<>(context.getSpecialGroups()));
                processId = process.getID();
                this.scriptName = process.getName();
                context.complete();
            } catch (Exception e) {
                System.out.println("CommandLineDspaceRunnableHandler with ePerson: " + ePersonUUID + " for Script with name: " + scriptName +
                        " and parameters: " + parameters + " could not be created");
                e.printStackTrace();
            } finally {
                if (context.isValid()) {
                    context.abort();
                }
            }
        }
    }


    @Override
    public void start() {
        logInfo("The script has started");
        if (isSaveEnabled()) {
            Context context = new Context();
            try {
                Process process = processService.find(context, processId);
                processService.start(context, process);
                context.complete();
            } catch (SQLException e) {
                System.out.println("RestDSpaceRunnableHandler with process: " + processId + " could not be started");
                e.printStackTrace();
            } finally {
                if (context.isValid()) {
                    context.abort();
                }
            }
        }
    }

    @Override
    public void handleCompletion() {
        logInfo("The script has completed");
        if (isSaveEnabled()) {
            Context context = new Context();
            try {
                Process process = processService.find(context, processId);
                processService.complete(context, process);
                addLogBitstreamToProcess(context);
                context.complete();
            } catch (SQLException e) {
                System.out.println("CommandLineDSpaceRunnableHandler with process: " + processId + " could not be completed");
                e.printStackTrace();
            } catch (IOException | AuthorizeException e) {
                System.out.println("CommandLineDSpaceRunnableHandler with process: " + processId + " could not be completed due to an " +
                        "error with the logging bitstream");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } finally {
                if (context.isValid()) {
                    context.abort();
                }
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
            System.err.println(message);
            log.error(message);
        }
        if (e != null) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }

        if (isSaveEnabled()) {
            Context context = new Context();
            try {
                Process process = processService.find(context, processId);
                processService.fail(context, process);

                addLogBitstreamToProcess(context);
                context.complete();
            } catch (SQLException sqlException) {
                System.out.println("SQL exception while handling another exception");
                sqlException.printStackTrace();
            } catch (IOException | AuthorizeException ioException) {
                System.out.println("RestDSpaceRunnableHandler with process: " + processId + " could not be completed due to an " +
                        "error with the logging bitstream");
                ioException.printStackTrace();
            } catch (Exception exception) {
                System.out.println(exception.getMessage());
                exception.printStackTrace();
            } finally {
                if (context.isValid()) {
                    context.abort();
                }
            }
        }

        System.exit(1);
    }

    @Override
    public void logDebug(String message) {
        log.debug(message);
    }

    @Override
    public void logInfo(String message) {
        System.out.println(message);
        log.info(message);
    }

    @Override
    public void logWarning(String message) {
        System.out.println(message);
        log.warn(message);
    }

    @Override
    public void logError(String message) {
        System.err.println(message);
        log.error(message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        System.err.println(message);
        log.error(message, throwable);
    }

    @Override
    public void printHelp(Options options, String name) {
        if (options != null) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(name, options);
        }
    }

    @Override
    public Optional<InputStream> getFileStream(Context context, String fileName) throws IOException {
        File file = new File(fileName);
        if (!(file.exists() && file.isFile())) {
            return Optional.empty();
        }
        return Optional.of(FileUtils.openInputStream(file));
    }

    @Override
    public void writeFilestream(Context context, String fileName, InputStream inputStream, String type)
            throws IOException {
        File file = new File(fileName);
        FileUtils.copyInputStreamToFile(inputStream, file);
    }

    @Override
    public List<UUID> getSpecialGroups() {
        return Collections.emptyList();
    }

    private boolean isSaveEnabled() {
        return configurationService.getBooleanProperty("process.save-enable", false);
    }

    private EPerson getEpersonProcess(Context context) throws Exception {
        UUID epersonUUID = UUID.fromString(configurationService.getProperty("process.eperson"));
        EPerson ePerson = ePersonService.find(context, epersonUUID);
        if (ePerson == null) {
            throw new Exception("EPerson UUID not valid, no result found.");
        }
        return ePerson;
    }

    private void addLogBitstreamToProcess(Context context) throws SQLException, IOException, AuthorizeException {
        try {
            EPerson ePerson = ePersonService.find(context, ePersonUUID);
            Process process = processService.find(context, processId);

            context.setCurrentUser(ePerson);
            processService.createLogBitstream(context, process);
        } catch (SQLException | IOException | AuthorizeException e) {
            System.out.println("CommandLineDspaceRunnableHandler with process: " + processId + " could not write log to process" + e.getMessage());
        }
    }
}
