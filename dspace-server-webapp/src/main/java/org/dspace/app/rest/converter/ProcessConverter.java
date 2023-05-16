/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.stream.Collectors;

import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This converter will convert an object of {@Link Process} to an object of {@link ProcessRest}
 */
@Component
public class ProcessConverter implements DSpaceConverter<Process, ProcessRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Autowired
    private ProcessService processService;

    @Override
    public ProcessRest convert(Process process, Projection projection) {
        ProcessRest processRest = new ProcessRest();
        processRest.setProjection(projection);
        processRest.setId(process.getID());
        processRest.setScriptName(process.getName());
        processRest.setProcessId(process.getID());
        processRest.setUserId(process.getEPerson().getID());
        processRest.setProcessStatus(process.getProcessStatus());
        processRest.setStartTime(process.getStartTime());
        processRest.setEndTime(process.getFinishedTime());
        processRest.setParameterRestList(processService.getParameters(process).stream()
                .map(x -> (ParameterValueRest) converter.toRest(x, projection)).collect(Collectors.toList()));
        return processRest;
    }

    @Override
    public Class<Process> getModelClass() {
        return Process.class;
    }
}
