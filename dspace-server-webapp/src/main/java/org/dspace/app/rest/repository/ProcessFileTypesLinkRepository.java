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
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ProcessFileTypesRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(ProcessRest.CATEGORY + "." + ProcessRest.NAME + "." + ProcessRest.FILE_TYPES)
public class ProcessFileTypesLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessRestRepository processRestRepository;

    @PreAuthorize("hasAuthority('ADMIN')")
    public ProcessFileTypesRest getFileTypesFromProcess(@Nullable HttpServletRequest request,
                                                        Integer processId,
                                                        @Nullable Pageable optionalPageable,
                                                        Projection projection) throws SQLException, AuthorizeException {

        Context context = obtainContext();
        Process process = processService.find(context, processId);
        if (process == null) {
            throw new ResourceNotFoundException("Process with id " + processId + " was not found");
        }
        List<String> fileTypes = processService.getFileTypesForProcessBitstreams(context, process);
        ProcessFileTypesRest processFileTypesRest = new ProcessFileTypesRest();
        processFileTypesRest.setId("filetypes-" + processId);
        processFileTypesRest.setValues(fileTypes);
        return processFileTypesRest;
    }
}
