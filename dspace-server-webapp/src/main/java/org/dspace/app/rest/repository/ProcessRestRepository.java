/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.converter.processes.ProcessConverter;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.content.Process;
import org.dspace.content.service.ProcessService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(ProcessRest.CATEGORY + "." + ProcessRest.NAME)
public class ProcessRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessConverter processConverter;

    public List<ProcessRest> getAllProcesses() throws SQLException {
        Context context = obtainContext();
        List<Process> list = processService.findAll(context);

        List<ProcessRest> listToReturn = new LinkedList<>();
        for (Process process : list) {
            listToReturn.add(processConverter.fromModel(process));
        }

        return listToReturn;
    }

    public ProcessRest getProcessById(Integer processId) throws SQLException {
        Context context = obtainContext();
        Process process = processService.find(context, processId);
        return processConverter.fromModel(process);
    }
}
