/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.converter.RequestItemConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Component to expose item requests.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Component(RequestItemRest.CATEGORY + '.' + RequestItemRest.NAME)
public class RequestItemRepository
        extends DSpaceRestRepository<RequestItemRest, String> {
    private static final Logger LOG = LogManager.getLogger();

    @Autowired(required = true)
    protected RequestItemService requestItemService;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    protected RequestItemConverter requestItemConverter;

    @Override
    public RequestItemRest findOne(Context context, String id) {
        RequestItem requestItem = requestItemService.findByToken(context, id);
        if (null == requestItem) {
            return null;
        } else {
            return requestItemConverter.convert(requestItem, new DefaultProjection());
        }
    }

    @Override
    public Page<RequestItemRest> findAll(Context context, Pageable pageable) {
        // TODO ? There is no enumerator in RequestItemService
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RequestItemRest createAndReturn(Context ctx) {
        // Fill a RequestItemRest from the client's HTTP request.
        HttpServletRequest req = getRequestService()
                .getCurrentRequest()
                .getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        RequestItemRest rir;
        try {
            rir = mapper.readValue(req.getInputStream(), RequestItemRest.class);
        } catch (IOException ex) {
            throw new UnprocessableEntityException("error parsing the body", ex);
        }

        // Create the item request model object from the REST object.
        String token;
        try {
            Bitstream bitstream = bitstreamService.find(ctx,
                    UUID.fromString(rir.getBitstreamId()));
            Item item = itemService.find(ctx,
                    UUID.fromString(rir.getItemId()));
            token = requestItemService.createRequest(ctx, bitstream, item,
                    rir.isAllfiles(), rir.getRequestEmail(), rir.getRequestName(),
                    rir.getRequestMessage());
        } catch (SQLException ex) {
            throw new RuntimeException("Item request not created.", ex);
        }


        // Some fields are given values during creation, so return created request.
        RequestItem ri = requestItemService.findByToken(ctx, token);
        ri.setAccept_request(false); // Not accepted yet.  Must set:  DS-4032
        requestItemService.update(ctx, ri);
        return requestItemConverter.convert(ri, new DefaultProjection());
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
