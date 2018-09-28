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
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        return requestItemConverter.convert(requestItem, new DefaultProjection());
    }

    @Override
    public Page<RequestItemRest> findAll(Context context, Pageable pageable) {
        // TODO ? There is no enumerator in RequestItemService
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected RequestItemRest createAndReturn(Context context)
            throws SQLException {
        HttpServletRequest httpRequest = getRequestService().getCurrentRequest()
                .getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        RequestItemRest requestItemRest = null;
        try {
            ServletInputStream input = httpRequest.getInputStream();
            requestItemRest = mapper.readValue(input, RequestItemRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }
        Bitstream bitstream = bitstreamService.find(context,
                UUID.fromString(requestItemRest.getBitstream().getUuid()));
        Item item = itemService.find(context,
                UUID.fromString(requestItemRest.getItem().getUuid()));
        String itemRequestId = requestItemService.createRequest(context,
                bitstream, item,
                requestItemRest.isAllfiles(),
                requestItemRest.getReqEmail(),
                requestItemRest.getReqName(),
                requestItemRest.getReqMessage());
        requestItemRest.setToken(itemRequestId);
        return requestItemRest;
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
