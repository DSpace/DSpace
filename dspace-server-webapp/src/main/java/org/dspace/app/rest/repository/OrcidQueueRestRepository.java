/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static java.lang.Math.toIntExact;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.OrcidQueueRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible of exposing Orcid queue resources.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(OrcidQueueRest.CATEGORY + "." + OrcidQueueRest.NAME)
public class OrcidQueueRestRepository extends DSpaceRestRepository<OrcidQueueRest, Integer> {

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Override
    @PreAuthorize("permitAll()")
    public OrcidQueueRest findOne(Context context, Integer id) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Page<OrcidQueueRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    @PreAuthorize("permitAll()")
    protected void delete(Context context, Integer id) {
        try {
            orcidQueueService.deleteById(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected OrcidQueueRest createAndReturn(Context context)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {
        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();

        String orcidQueueId = request.getParameter("orcidQueueId");
        orcidQueueService.sendToOrcid(context, Integer.valueOf(orcidQueueId));
        return new OrcidQueueRest();
    }

    @SearchRestMethod(name = "findByOwner")
    @PreAuthorize("permitAll()")
    public Page<OrcidQueueRest> findByOwnerId(@Parameter(value = "ownerId", required = true) String ownerId,
        Pageable pageable) {

        Context context = obtainContext();
        try {
            UUID id = UUID.fromString(ownerId);
            List<OrcidQueue> result = orcidQueueService.findByOwnerId(context, id, pageable.getPageSize(),
                toIntExact(pageable.getOffset()));
            long totalCount = orcidQueueService.countByOwnerId(context, id);
            return converter.toRestPage(result, pageable, totalCount, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    @Override
    public Class<OrcidQueueRest> getDomainClass() {
        return OrcidQueueRest.class;
    }

}
