/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.OrcidHistoryRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible of exposing OrcidHistory resources.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
@Component(OrcidHistoryRest.CATEGORY  + "." + OrcidHistoryRest.NAME)
public class OrcidHistoryRestRepository extends DSpaceRestRepository<OrcidHistoryRest, Integer> {

    @Autowired
    private OrcidHistoryService orcidHistoryService;

    @Override
    public Page<OrcidHistoryRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ORCID', 'READ')")
    public OrcidHistoryRest findOne(Context context, Integer id) {
        OrcidHistory orcidHistory = null;
        try {
            orcidHistory = orcidHistoryService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (orcidHistory == null) {
            return null;
        }
        return converter.toRest(orcidHistory, utils.obtainProjection());
    }

    @Override
    protected OrcidHistoryRest createAndReturn(Context context)
           throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {
        OrcidHistoryRest  orcidHistoryRest = null;
        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();

        String orcidQueueId = request.getParameter("orcidQueueId");
        orcidHistoryService.sendToOrcid(context, Integer.valueOf(orcidQueueId));
        return orcidHistoryRest;
    }

    @Override
    public Class<OrcidHistoryRest> getDomainClass() {
        return OrcidHistoryRest.class;
    }

}
