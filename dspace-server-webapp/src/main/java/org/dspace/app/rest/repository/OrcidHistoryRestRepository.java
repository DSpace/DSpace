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
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.OrcidHistoryRest;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.orcid.OrcidHistory;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.service.OrcidHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty("orcid.synchronization-enabled")
public class OrcidHistoryRestRepository extends DSpaceRestRepository<OrcidHistoryRest, Integer> {

    @Autowired
    private OrcidHistoryService orcidHistoryService;

    @Autowired
    private UriListHandlerService uriListHandlerService;

    @Override
    public Page<OrcidHistoryRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ORCID_HISTORY', 'READ')")
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
    @PreAuthorize("hasPermission(#list, 'ORCID_HISTORY', 'ADD')")
    protected OrcidHistoryRest createAndReturn(Context context, List<String> list)
           throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {
        OrcidHistory orcidHistory = null;
        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();
        OrcidQueue orcidQueue = uriListHandlerService.handle(context, request, list, OrcidQueue.class);
        if (orcidQueue == null) {
            throw new IllegalArgumentException("No ORCID Queue record found, the uri-list does not contait a resource");
        }
        boolean forceAddition =  Boolean.parseBoolean(request.getParameter("forceAddition"));
        orcidHistory = orcidHistoryService.synchronizeWithOrcid(context, orcidQueue, forceAddition);
        return orcidHistory != null ? converter.toRest(orcidHistory, utils.obtainProjection()) : null;
    }

    @Override
    public Class<OrcidHistoryRest> getDomainClass() {
        return OrcidHistoryRest.class;
    }

}
