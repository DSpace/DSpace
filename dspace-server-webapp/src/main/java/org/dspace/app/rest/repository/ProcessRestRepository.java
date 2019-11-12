/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.processes.ProcessConverter;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.ProcessResource;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private ProcessConverter processConverter;

    @Autowired
    private AuthorizeService authorizeService;

    /**
     * This method will return an integer describing the total amount of Process objects in the database
     * @return The total amount of Process objects in the database
     * @throws SQLException If something goes wrong
     */
    public int getTotalAmountOfProcesses() throws SQLException {
        return processService.countTotal(obtainContext());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'PROCESS', 'READ')")
    public ProcessRest findOne(Context context, Integer id) {
        try {
            Process process = processService.find(context, id);
            if (process == null) {
                return null;
            }
            return processConverter.fromModel(process);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ProcessRest> findAll(Context context, Pageable pageable) {
        List<Process> processes = null;
        try {
            processes = processService.findAll(context);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<ProcessRest> page = utils.getPage(processes, pageable).map(processConverter);
        return page;
    }

    public Class<ProcessRest> getDomainClass() {
        return null;
    }

    public DSpaceResource<ProcessRest> wrapResource(ProcessRest model, String... rels) {
        return new ProcessResource(model, utils, rels);
    }
}
