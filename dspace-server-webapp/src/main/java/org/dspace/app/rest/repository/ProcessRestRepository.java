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

/**
 * The repository for the Process workload
 */
@Component(ProcessRest.CATEGORY + "." + ProcessRest.NAME)
public class ProcessRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessConverter processConverter;

    /**
     * This method will return a list of all Process objects converted to ProcessRest objects
     * @return  The list of ProcessRest objects coming forth from all Process objects in the database
     * @throws SQLException If something goes wrong
     */
    public List<ProcessRest> getAllProcesses() throws SQLException {
        Context context = obtainContext();
        List<Process> list = processService.findAll(context);

        List<ProcessRest> listToReturn = new LinkedList<>();
        for (Process process : list) {
            listToReturn.add(processConverter.fromModel(process));
        }

        return listToReturn;
    }

    /**
     * This method will return a ProcessRest object that comes forth from the Process object found by the given id
     * @param processId     The id that will be used to retrieve a Process object to then convert that into a
     *                      ProcessRest object
     * @return              The converted ProcessRest object
     * @throws SQLException If something goes wrong
     */
    public ProcessRest getProcessById(Integer processId) throws SQLException {
        Context context = obtainContext();
        Process process = processService.find(context, processId);
        return processConverter.fromModel(process);
    }
}
