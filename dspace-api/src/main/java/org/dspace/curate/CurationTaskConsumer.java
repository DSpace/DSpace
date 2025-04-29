/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.event.Event;
import org.dspace.event.NamedConsumer;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.ProcessDSpaceRunnableHandler;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class CurationTaskConsumer extends NamedConsumer {

    ScriptService scriptService;
    Logger log = LogManager.getLogger();
    /**
     * Name of the curation task which this consumer will execute
     */
    private String curationTaskName;

    /**
     * Iniitalize the consumer, getting curation task details from named consumer configuration
     * @throws Exception
     */
    @Override
    public void initialize() throws Exception {
        this.scriptService = ScriptServiceFactory.getInstance().getScriptService();
        this.curationTaskName = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("event.consumer." + name + ".taskname");
        if (this.curationTaskName == null) {
            throw new Exception("No curation task name configured for consumer " + name);
        }
    }

    /**
     * Resolve and execute the configured curation task for this event's subject
     * @param ctx   the current DSpace session
     * @param event the content event
     * @throws Exception
     */
    @Override
    public void consume(Context ctx, Event event) throws Exception {
        log.debug("Consumer Event " + event.toString());
        ScriptConfiguration scriptToExecute = scriptService.getScriptConfiguration("curate");
        DSpaceRunnable dSpaceRunnable = scriptService.createDSpaceRunnableForScriptConfiguration(scriptToExecute);
        List<DSpaceCommandLineParameter> parameters = Arrays.asList(new DSpaceCommandLineParameter[] {
            new DSpaceCommandLineParameter("-t", this.curationTaskName),
            new DSpaceCommandLineParameter("-i", event.getSubjectID().toString())
        });
        List<String> args = new ArrayList<>(2 * parameters.size());
        for (DSpaceCommandLineParameter parameter : parameters) {
            args.add(parameter.getName());
            if (parameter.getValue() != null) {
                args.add(parameter.getValue());
            }
        }

        if (scriptToExecute == null) {
            log.error("Unable to find script curate");
            throw new RuntimeException("The script for name: curate wasn't found");
        }
        try {
            if (!scriptToExecute.isAllowedToExecute(ctx, parameters)) {
                log.error("Current user is not eligible to execute script with name: curate and the specified " +
                        "parameters " + StringUtils.join(parameters, ", "));
                return;
            }
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument " + e.getMessage(), e);
        }
        log.debug("Initializing process " + scriptToExecute.getName() + " for user " + ctx.getCurrentUser().getID() +
                "with parameters: " + StringUtils.join(parameters, " ") + ".");
        ProcessDSpaceRunnableHandler processDSpaceRunnableHandler = new ProcessDSpaceRunnableHandler(
                ctx.getCurrentUser(), scriptToExecute.getName(), parameters,
                new HashSet<>(ctx.getSpecialGroups()));
        try {
            dSpaceRunnable.initialize(args.toArray(new String[0]), processDSpaceRunnableHandler, ctx.getCurrentUser());
            processDSpaceRunnableHandler.schedule(dSpaceRunnable);
        } catch (ParseException e) {
            dSpaceRunnable.printHelp();
            try {
                processDSpaceRunnableHandler.handleException(
                        "Failed to parse the arguments given to the script with name: " + scriptToExecute.getName()
                                + " and args: " + StringUtils.join(parameters, " "), e
                );
            } catch (Exception re) {
                // ignore re-thrown exception
            }
        }
        log.debug("Process scheduled.");
    }

    @Override
    public void end(Context ctx) throws Exception {

    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

}
