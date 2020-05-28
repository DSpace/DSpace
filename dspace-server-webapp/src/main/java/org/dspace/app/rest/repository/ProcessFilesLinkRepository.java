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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the {@link LinkRestRepository} implementation that takes care of retrieving the
 * {@link ProcessFileWrapperRest} for the Process endpoints
 *
 */
@Component(ProcessRest.CATEGORY + "." + ProcessRest.NAME + "." + ProcessRest.FILES)
public class ProcessFilesLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ProcessRestRepository processRestRepository;

    /**
     * This method will retrieve all the files from the process and wrap them into a {@link ProcessFileWrapperRest}
     * object to return
     * @param request           The current request
     * @param processId         The processId for the Process to use
     * @param optionalPageable  Pageable if applicable
     * @param projection        Projection if applicable
     * @return                  A {@link ProcessFileWrapperRest} object filled with the bitstreams from the process
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @PreAuthorize("hasAuthority('ADMIN')")
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
