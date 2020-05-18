/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ProcessFileWrapperRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(ProcessRest.CATEGORY + "." + ProcessRest.NAME + "." + ProcessRest.FILES)
public class ProcessFilesLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ProcessRestRepository processRestRepository;

    public ProcessFileWrapperRest getFilesFromProcess(@Nullable HttpServletRequest request,
                                                      Integer processId,
                                                      @Nullable Pageable optionalPageable,
                                                      Projection projection) throws SQLException, AuthorizeException {


        ProcessFileWrapperRest processFileWrapperRest = new ProcessFileWrapperRest();
        processFileWrapperRest.setBitstreams(processRestRepository.getProcessBitstreams(processId));
        processFileWrapperRest.setProcessId(processId);

        return processFileWrapperRest;
    }
}
