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
import org.dspace.app.rest.model.ProcessRest;
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

    @Override
    public Class<ProcessRest> getDomainClass() {
        return ProcessRest.class;
    }
}
