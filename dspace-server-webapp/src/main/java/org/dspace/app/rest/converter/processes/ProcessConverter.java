/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter.processes;

import java.util.stream.Collectors;

import org.dspace.app.rest.converter.DSpaceConverter;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.content.Process;
import org.dspace.content.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessConverter implements DSpaceConverter<Process, ProcessRest> {

    @Autowired
    private ProcessService processService;

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    public ProcessRest fromModel(Process process) {
        ProcessRest processRest = new ProcessRest();
        processRest.setScriptName(process.getName());
        processRest.setProcessId(process.getID());
        processRest.setUserId(process.getEPerson().getID());
        processRest.setProcessStatus(process.getProcessStatus());
        processRest.setStartTime(process.getStartTime());
        processRest.setParameterRestList(
            processService.getParameters(process).stream().map(x -> dSpaceRunnableParameterConverter.fromModel(x))
                          .collect(Collectors.toList()));
        return processRest;
    }

    public Process toModel(ProcessRest obj) {
        return null;
    }
}
