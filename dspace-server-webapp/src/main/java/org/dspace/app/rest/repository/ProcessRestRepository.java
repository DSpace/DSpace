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
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Process;
import org.dspace.content.service.ProcessService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
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

    @Autowired
    private AuthorizeService authorizeService;

    /**
     * This method will return a list of all Process objects converted to ProcessRest objects
     * @return  The list of ProcessRest objects coming forth from all Process objects in the database
     * @throws SQLException If something goes wrong
     */
    public List<ProcessRest> getAllProcesses(Pageable pageable) throws SQLException {
        Context context = obtainContext();
        List<Process> list = processService.findAll(context, pageable.getPageSize(), pageable.getOffset());

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
    public ProcessRest getProcessById(Integer processId) throws SQLException, AuthorizeException {
        Context context = obtainContext();
        Process process = processService.find(context, processId);
        if (process == null) {
            throw new ResourceNotFoundException("The process with ID: " + processId + " wasn't found");
        }
        if ((context.getCurrentUser() == null) || (!context.getCurrentUser().equals(process.getEPerson())
            && !authorizeService.isAdmin(context))) {
            throw new AuthorizeException("The current user is not eligible to view the process with id: " + processId);
        }
        return processConverter.fromModel(process);
    }

    /**
     * This method will return an integer describing the total amount of Process objects in the database
     * @return  The total amount of Process objects in the database
     * @throws SQLException If something goes wrong
     */
    public int getTotalAmountOfProcesses() throws SQLException {
        return processService.countTotal(obtainContext());
    }
}
