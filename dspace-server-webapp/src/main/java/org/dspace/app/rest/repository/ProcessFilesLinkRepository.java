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

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the {@link LinkRestRepository} implementation that takes care of retrieving the list of
 * {@link org.dspace.content.Bitstream} objects for the Process endpoints
 *
 */
@Component(ProcessRest.CATEGORY + "." + ProcessRest.PLURAL_NAME + "." + ProcessRest.FILES)
public class ProcessFilesLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ProcessRestRepository processRestRepository;

    /**
     * This method will retrieve all the files from the process
     * @param request           The current request
     * @param processId         The processId for the Process to use
     * @param optionalPageable  Pageable if applicable
     * @param projection        Projection if applicable
     * @return A list of {@link BitstreamRest} objects filled
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @PreAuthorize("hasPermission(#processId, 'PROCESS', 'READ')")
    public Page<BitstreamRest> getFilesFromProcess(@Nullable HttpServletRequest request,
                                                   Integer processId,
                                                   @Nullable Pageable optionalPageable,
                                                   Projection projection) throws SQLException, AuthorizeException {

        List<BitstreamRest> list = processRestRepository.getProcessBitstreams(processId);
        Pageable pageable = utils.getPageable(optionalPageable);
        return utils.getPage(list, pageable);
    }

    /**
     * This method will retrieve a bitstream for the given processId for the given fileType
     * @param request       The current request
     * @param processId     The processId for the process to search in
     * @param fileType      The filetype that the bitstream has to be
     * @param pageable      Pageable if applicable
     * @param projection    The current projection
     * @return The BitstreamRest object that corresponds with the Process and type
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @PreAuthorize("hasPermission(#processId, 'PROCESS', 'READ')")
    public BitstreamRest getResource(HttpServletRequest request, String processId, String fileType,
                                     Pageable pageable, Projection projection)
        throws SQLException, AuthorizeException {
        if (log.isTraceEnabled()) {
            log.trace("Retrieving Files with type " + fileType + " from Process with ID: " + processId);
        }

        return processRestRepository.getProcessBitstreamByType(Integer.parseInt(processId), fileType);
    }
}
