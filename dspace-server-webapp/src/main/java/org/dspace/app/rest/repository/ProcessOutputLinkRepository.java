/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
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

/**
 * This linkRepository will deal with calls to the /output endpoint of a given Process.
 * It'll retrieve the output for the given Process and return this as a {@link BitstreamRest} object
 */
@Component(ProcessRest.CATEGORY + "." + ProcessRest.PLURAL_NAME + "." + ProcessRest.OUTPUT)
public class ProcessOutputLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ProcessService processService;

    @Autowired
    private AuthorizeService authorizeService;

    /**
     * This method will retrieve the output for the {@link Process} as defined through the
     * given ID in the rest call. This output is a {@link Bitstream} object that will be turned into a
     * {@link BitstreamRest} object to be returned
     * @param request           The current request
     * @param processId         The given processId for the {@link Process}
     * @param optionalPageable  Pageable if applicable
     * @param projection        The current projection
     * @return                  The {@link BitstreamRest} representing the output for the {@link Process}
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @PreAuthorize("hasPermission(#processId, 'PROCESS', 'READ')")
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
        if (bitstream == null) {
            return null;
        }
        return converter.toRest(bitstream, projection);
    }
}
