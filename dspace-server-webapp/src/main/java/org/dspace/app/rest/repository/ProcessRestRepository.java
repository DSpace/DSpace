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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * The repository for the Process workload
 */
@Component(ProcessRest.CATEGORY + "." + ProcessRest.NAME)
public class ProcessRestRepository extends DSpaceRestRepository<ProcessRest, Integer> {

    private static final Logger log = Logger.getLogger(ProcessRestRepository.class);

    @Autowired
    private ProcessService processService;

    @Autowired
    private ConverterService converterService;


    @Autowired
    private AuthorizeService authorizeService;


    @Override
    @PreAuthorize("hasPermission(#id, 'PROCESS', 'READ')")
    public ProcessRest findOne(Context context, Integer id) {
        try {
            Process process = processService.find(context, id);
            if (process == null) {
                return null;
            }
            return converter.toRest(process, utils.obtainProjection());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ProcessRest> findAll(Context context, Pageable pageable) {
        try {
            int total = processService.countTotal(context);
            List<Process> processes = processService.findAll(context, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(processes, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Calls on the getBitstreams method to retrieve all the Bitstreams of this process
     * @param processId The processId of the Process to retrieve the Bitstreams for
     * @return          The list of Bitstreams of the given Process
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    public List<BitstreamRest> getProcessBitstreams(Integer processId) throws SQLException, AuthorizeException {
        Context context = obtainContext();
        Process process = getProcess(processId, context);
        List<Bitstream> bitstreams = processService.getBitstreams(context, process);
        return bitstreams.stream()
                  .map(bitstream -> (BitstreamRest) converterService.toRest(bitstream, Projection.DEFAULT))
                  .collect(Collectors.toList());
    }

    private Process getProcess(Integer processId, Context context) throws SQLException, AuthorizeException {
        Process process = processService.find(context, processId);
        if (process == null) {
            throw new ResourceNotFoundException("Process with id " + processId + " was not found");
        }
        if ((context.getCurrentUser() == null) || (!context.getCurrentUser()
                                                           .equals(process.getEPerson()) && !authorizeService
            .isAdmin(context))) {
            throw new AuthorizeException("The current user is not eligible to view the process with id: " + processId);
        }
        return process;
    }

    /**
     * Retrieves the Bitstream in the given Process of a given type
     * @param processId The processId of the Process to be used
     * @param type      The type of bitstreams to be returned, if null it'll return all the bitstreams
     * @return          The bitstream for the given parameters
     * @throws SQLException If something goes wrong
     * @throws AuthorizeException If something goes wrong
     */
    public BitstreamRest getProcessBitstreamByType(Integer processId, String type)
        throws SQLException, AuthorizeException {
        Context context = obtainContext();
        Process process = getProcess(processId, context);
        Bitstream bitstream = processService.getBitstream(context, process, type);

        return converterService.toRest(bitstream, utils.obtainProjection());
    }

    @Override
    protected void delete(Context context, Integer integer)
        throws AuthorizeException, RepositoryMethodNotImplementedException {
        try {
            processService.delete(context, processService.find(context, integer));
        } catch (SQLException | IOException e) {
            log.error("Something went wrong trying to find Process with id: " + integer, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<ProcessRest> getDomainClass() {
        return ProcessRest.class;
    }
}
