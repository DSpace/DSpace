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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ProcessOutputRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessLog;
import org.dspace.scripts.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(ProcessRest.CATEGORY + "." + ProcessRest.NAME + "." + ProcessRest.OUTPUT)
public class ProcessOutputLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ProcessService processService;

    @Autowired
    private AuthorizeService authorizeService;

    @PreAuthorize("hasAuthority('ADMIN')")
    public ProcessOutputRest getOutputFromProcess(@Nullable HttpServletRequest request,
                                                      Integer processId,
                                                      @Nullable Pageable optionalPageable,
                                                      Projection projection) throws SQLException, AuthorizeException {

        Context context = obtainContext();
        Process process = processService.find(context, processId);
        if ((context.getCurrentUser() == null) || (!context.getCurrentUser().equals(process.getEPerson())
            && !authorizeService.isAdmin(context))) {
            throw new AuthorizeException("The current user is not eligible to view the process with id: " + processId);
        }
        ProcessOutputRest output = new ProcessOutputRest();
        List<ProcessLog> processLogs = processService.getProcessLogsFromProcess(context, process);

        output.setLogs(processLogs.stream().map(processLog -> processLog.getOutput()).collect(Collectors.toList()));

        return output;
    }
}
