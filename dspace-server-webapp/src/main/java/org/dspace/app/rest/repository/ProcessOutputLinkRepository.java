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
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(ProcessRest.CATEGORY + "." + ProcessRest.NAME + "." + ProcessRest.OUTPUT)
public class ProcessOutputLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ProcessService processService;

    @Autowired
    private AuthorizeService authorizeService;

    //    /**
//     * This method will retrieve the list of {@link ProcessLog} objects from the {@link Process} as defined through
//     the
//     * given ID in the rest call and it'll wrap this in a {@link ProcessOutputRest} object to return these
//     * @param request           The current request
//     * @param processId         The given processId for the {@link Process}
//     * @param optionalPageable  Pageable if applicable
//     * @param projection        The current projection
//     * @return                  The {@link ProcessOutputRest} containing the list of all {@link ProcessLog} for the
//     *                          given {@link Process}
//     * @throws SQLException         If something goes wrong
//     * @throws AuthorizeException   If something goes wrong
//     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public BitstreamRest getOutputFromProcess(@Nullable HttpServletRequest request,
                                              Integer processId,
                                              @Nullable Pageable optionalPageable,
                                              Projection projection) throws SQLException, AuthorizeException {

        Context context = obtainContext();
        Process process = processService.find(context, processId);
        if ((context.getCurrentUser() == null) || (!context.getCurrentUser().equals(process.getEPerson())
                && !authorizeService.isAdmin(context))) {
            throw new AuthorizeException("The current user is not eligible to view the process with id: " + processId);
        }
        Bitstream bitstream = processService.getBitstream(context, process, Process.OUTPUT_TYPE);
        return converter.toRest(bitstream, projection);
    }
}