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
import org.dspace.content.service.ProcessService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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
                return scriptConverter.fromModel(dSpaceRunnable);
            }
        }
        throw new DSpaceBadRequestException("The script with name: " + name + " could not be found");
    }

    @Override
    public Page<ScriptRest> findAll(Context context, Pageable pageable) {
        List list = dspaceRunnables.stream().skip(pageable.getOffset()).limit(pageable.getPageSize())
                                   .collect(Collectors.toList());
        Page<ScriptRest> scriptRestPage = new PageImpl<>(list, pageable, dspaceRunnables.size()).map(scriptConverter);
        return scriptRestPage;
    }

    public Class<ScriptRest> getDomainClass() {
        return ScriptRest.class;
    }

    public DSpaceResource<ScriptRest> wrapResource(ScriptRest model, String... rels) {
        return new ScriptResource(model, utils, rels);
    }

    public ProcessRest startProcess(String scriptName) throws SQLException, IOException {
        Context context = obtainContext();
        List<ParameterValueRest> parameterValueRestList = new LinkedList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String propertiesJson = requestService.getCurrentRequest().getServletRequest().getParameter("properties");
        if (StringUtils.isNotBlank(propertiesJson)) {
            try {
                parameterValueRestList = Arrays
                    .asList(objectMapper.readValue(propertiesJson, ParameterValueRest[].class));
            } catch (IOException e) {
                log.error(
                    "Couldn't convert the given properties to proper ParameterValueRest objects: " + propertiesJson, e);
                throw e;
            }
        }

        List<DSpaceCommandLineParameter> dSpaceCommandLineParameters = new LinkedList<>();
        dSpaceCommandLineParameters.addAll(
            parameterValueRestList.stream().map(x -> dSpaceRunnableParameterConverter.toModel(x))
                                  .collect(Collectors.toList()));
        try {
            RestDSpaceRunnableHandler restDSpaceRunnableHandler = new RestDSpaceRunnableHandler(
                context.getCurrentUser(), scriptName, dSpaceCommandLineParameters);
            runDSpaceScriptWithArgs(dSpaceCommandLineParameters.stream().map(x -> x.toString()).toArray(String[]::new),
                                    restDSpaceRunnableHandler, scriptName);
            context.complete();
            return processConverter.fromModel(restDSpaceRunnableHandler.getProcess());
        } catch (SQLException e) {
            log.error("Failed to create a process with user: " + context.getCurrentUser() +
                          " scriptname: " + scriptName + " and parameters " + DSpaceCommandLineParameter
                .concatenate(dSpaceCommandLineParameters), e);
        }
        return null;
    }

    private void runDSpaceScriptWithArgs(String[] args, RestDSpaceRunnableHandler dSpaceRunnableHandler,
                                         String scriptName) {
        List<DSpaceRunnable> scripts = new DSpace().getServiceManager().getServicesByType(DSpaceRunnable.class);
        for (DSpaceRunnable script : scripts) {
            if (StringUtils.equalsIgnoreCase(script.getName(), scriptName)) {
                try {
                    script.initialize(args, dSpaceRunnableHandler);
                    dSpaceRunnableHandler.schedule(script);
                } catch (ParseException e) {
                    script.printHelp();
                    dSpaceRunnableHandler.handleException("Failed to parse the arguments given to the script with name: " + scriptName
                                                              + " and args: " + Arrays.toString(args), e);
                }
            }
        }
    }
}
