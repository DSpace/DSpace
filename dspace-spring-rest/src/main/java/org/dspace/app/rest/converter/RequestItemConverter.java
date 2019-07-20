/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Convert between {@link org.dspace.app.requestitem.RequestItem} and
 * {@link org.dspace.app.rest.model.RequestItemRest}.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Component
public class RequestItemConverter
        implements DSpaceConverter<RequestItem, RequestItemRest> {
    private static final Logger LOG = LogManager.getLogger();

    @Autowired(required = true)
    protected BitstreamConverter bitstreamConverter;

    @Autowired(required = true)
    protected ItemConverter itemConverter;

    @Autowired(required = true)
    protected RequestItemService requestItemService;

    @Autowired(required = true)
    protected RequestService requestService;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Override
    public RequestItemRest fromModel(RequestItem requestItem) {
        RequestItemRest requestItemRest = new RequestItemRest();
        requestItemRest.setAccept_request(requestItem.isAccept_request());
        requestItemRest.setAllfiles(requestItem.isAllfiles());
        requestItemRest.setBitstream_id(requestItem.getBitstream().getID().toString());
        requestItemRest.setDecision_date(requestItem.getDecision_date());
        requestItemRest.setExpires(requestItem.getExpires());
        requestItemRest.setId(requestItem.getID());
        requestItemRest.setItem_id(requestItem.getItem().getID().toString());
        requestItemRest.setRequest_email(requestItem.getReqEmail());
        requestItemRest.setRequest_message(requestItem.getReqMessage());
        requestItemRest.setRequest_name(requestItem.getReqName());
        requestItemRest.setRequest_date(requestItem.getRequest_date());
        requestItemRest.setToken(requestItem.getToken());
        return requestItemRest;
    }

    @Override
    public RequestItem toModel(RequestItemRest rir) {
        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        Context context = ContextUtil.obtainContext(request);

        // Did we receive a token?  That is:  are we updating an existing request?
        String token = rir.getToken();
        if (StringUtils.isBlank(token)) { // No token, so create a new request.
            try {
                Bitstream bitstream = bitstreamService.find(context,
                        UUID.fromString(rir.getBitstream_id()));
                Item item = itemService.find(context,
                        UUID.fromString(rir.getItem_id()));
                token = requestItemService.createRequest(context,
                        bitstream,
                        item,
                        rir.isAllfiles(),
                        rir.getRequest_email(),
                        rir.getRequest_name(),
                        rir.getRequest_message());
            } catch (SQLException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        } // Otherwise (we have a token) this is updating an existing request.

        RequestItem requestItem = requestItemService.findByToken(context, token);
        requestItem.setAccept_request(rir.isAccept_request());
        requestItem.setDecision_date(rir.getDecision_date());
        requestItemService.update(context, requestItem);
        return requestItem;
    }
}
