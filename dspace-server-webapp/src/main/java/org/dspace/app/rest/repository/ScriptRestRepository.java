/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.converter.ScriptConverter;
import org.dspace.app.rest.converter.processes.ProcessConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.ScriptResource;
import org.dspace.app.rest.scripts.handler.impl.RestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.ProcessService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * This is the REST repository dealing with the Script logic
 */
@Component(ScriptRest.CATEGORY + "." + ScriptRest.NAME)
public class ScriptRestRepository extends DSpaceRestRepository<ScriptRest, String> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private List<DSpaceRunnable> dspaceRunnables;

    @Autowired
    private ScriptConverter scriptConverter;

    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessConverter processConverter;

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Override
    public ScriptRest findOne(Context context, String name) {
        for (DSpaceRunnable dSpaceRunnable : dspaceRunnables) {
            if (StringUtils.equalsIgnoreCase(dSpaceRunnable.getName(), name)) {
                if (dSpaceRunnable.isAllowedToExecute(context)) {
                    return scriptConverter.fromModel(dSpaceRunnable);
                } else {
                    throw new AccessDeniedException("The current user was not authorized to access this script");
                }
            }
        }
        throw new DSpaceBadRequestException("The script with name: " + name + " could not be found");
    }

    @Override
    public Page<ScriptRest> findAll(Context context, Pageable pageable) {
        return utils.getPage(dspaceRunnables.stream().filter(
            dSpaceRunnable -> dSpaceRunnable.isAllowedToExecute(context)).collect(Collectors.toList()), pageable)
                    .map(scriptConverter);
    }

    @Override
    public Class<ScriptRest> getDomainClass() {
        return ScriptRest.class;
    }

    @Override
    public DSpaceResource<ScriptRest> wrapResource(ScriptRest model, String... rels) {
        return new ScriptResource(model, utils, rels);
    }

    /**
     * This method will take a String scriptname parameter and it'll try to resolve this to a script known by DSpace.
     * If a script is found, it'll start a process for this script with the given properties to this request
     * @param scriptName    The name of the script that will try to be resolved and started
     * @return A ProcessRest object representing the started process for this script
     * @throws SQLException If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public ProcessRest startProcess(String scriptName) throws SQLException, IOException, AuthorizeException {
        Context context = obtainContext();
        String properties = requestService.getCurrentRequest().getServletRequest().getParameter("properties");
        List<DSpaceCommandLineParameter> dSpaceCommandLineParameters =
            processPropertiesToDSpaceCommandLineParameters(properties);
        DSpaceRunnable scriptToExecute = getdSpaceRunnableForName(scriptName);
        if (scriptToExecute == null) {
            throw new DSpaceBadRequestException("The script for name: " + scriptName + " wasn't found");
        }
        if (!scriptToExecute.isAllowedToExecute(context)) {
            throw new AuthorizeException("Current user is not eligible to execute script with name: " + scriptName);
        }
        RestDSpaceRunnableHandler restDSpaceRunnableHandler = new RestDSpaceRunnableHandler(
            context.getCurrentUser(), scriptName, dSpaceCommandLineParameters);
        List<String> args = constructArgs(dSpaceCommandLineParameters);
        try {
            runDSpaceScript(scriptToExecute, restDSpaceRunnableHandler, args);
            context.complete();
            return processConverter.fromModel(restDSpaceRunnableHandler.getProcess());
        } catch (SQLException e) {
            log.error("Failed to create a process with user: " + context.getCurrentUser() +
                          " scriptname: " + scriptName + " and parameters " + DSpaceCommandLineParameter
                .concatenate(dSpaceCommandLineParameters), e);
        }
        return null;
    }

    private List<DSpaceCommandLineParameter> processPropertiesToDSpaceCommandLineParameters(String propertiesJson)
        throws IOException {
        List<ParameterValueRest> parameterValueRestList = new LinkedList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        if (StringUtils.isNotBlank(propertiesJson)) {
            parameterValueRestList = Arrays.asList(objectMapper.readValue(propertiesJson, ParameterValueRest[].class));
        }

        List<DSpaceCommandLineParameter> dSpaceCommandLineParameters = new LinkedList<>();
        dSpaceCommandLineParameters.addAll(
            parameterValueRestList.stream().map(x -> dSpaceRunnableParameterConverter.toModel(x))
                                  .collect(Collectors.toList()));
        return dSpaceCommandLineParameters;
    }

    private DSpaceRunnable getdSpaceRunnableForName(String scriptName) {
        DSpaceRunnable scriptToExecute = null;
        for (DSpaceRunnable script : dspaceRunnables) {
            if (StringUtils.equalsIgnoreCase(script.getName(), scriptName)) {
                scriptToExecute = script;
                break;
            }
        }
        return scriptToExecute;
    }

    private List<String> constructArgs(List<DSpaceCommandLineParameter> dSpaceCommandLineParameters) {
        List<String> args = new ArrayList<>();
        for (DSpaceCommandLineParameter parameter : dSpaceCommandLineParameters) {
            args.add(parameter.getName());
            if (parameter.getValue() != null) {
                args.add(parameter.getValue());
            }
        }
        return args;
    }

    private void runDSpaceScript(DSpaceRunnable scriptToExecute,
                                 RestDSpaceRunnableHandler restDSpaceRunnableHandler, List<String> args) {
        try {
            scriptToExecute.initialize(args.toArray(new String[0]), restDSpaceRunnableHandler);
            restDSpaceRunnableHandler.schedule(scriptToExecute);
        } catch (ParseException e) {
            scriptToExecute.printHelp();
            restDSpaceRunnableHandler
                .handleException(
                    "Failed to parse the arguments given to the script with name: " + scriptToExecute.getName()
                        + " and args: " + args, e);
        }
    }

}
