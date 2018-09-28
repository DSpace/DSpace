/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.converter.RequestItemConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.model.hateoas.RequestItemResource;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Component;

/**
 * Component to expose item requests.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Component(RequestItemRest.CATEGORY + '.' + RequestItemRest.NAME)
public class RequestItemRepository
        extends DSpaceRestRepository<RequestItemRest, String> {
    private static final Logger LOG = LoggerFactory.getLogger(RequestItemRepository.class);

    @Autowired(required = true)
    protected RequestItemService requestItemService;

    @Autowired(required = true)
    protected RequestItemConverter requestItemConverter;

    @Override
    public RequestItemRest findOne(Context context, String id) {
        RequestItem requestItem = requestItemService.findByToken(context, id);
        return requestItemConverter.fromModel(requestItem);
    }

    @Override
    public Page<RequestItemRest> findAll(Context context, Pageable pageable) {
        // TODO ? There is no enumerator in RequestItemService
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RequestItemRest save(Context context, RequestItemRest ri) {
        try {
            requestItemService.createRequest(context, ri.getBitstream(),
                    ri.getItem(), ri.isAllfiles(), ri.getReqEmail(),
                    ri.getReqName(), ri.getReqMessage());
        } catch (SQLException ex) {
            LOG.error("New RequestItem not saved.", ex);
            throw new UncategorizedSQLException("New RequestItem save", null, ex);
        }
        return ri;
    }

    // NOTICE:  there is no service method for this -- requests are never deleted?
    @Override
    public void delete(Context context, String token)
            throws AuthorizeException, RepositoryMethodNotImplementedException {
        throw new RepositoryMethodNotImplementedException("RequestItemRest", "delete");
    }

    @Override
    public Class<RequestItemRest> getDomainClass() {
        return RequestItemRest.class;
    }
}
