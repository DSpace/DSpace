/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.converter;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        extends DSpaceConverter<RequestItem, RequestItemRest> {
    private static final Logger LOG = LoggerFactory.getLogger(RequestItemConverter.class);

    @Autowired(required = true)
    protected BitstreamConverter bitstreamConverter;

    @Autowired(required = true)
    protected ItemConverter itemConverter;

    @Autowired(required = true)
    protected RequestItemService requestItemService;

    @Autowired(required = true)
    protected RequestService requestService;

    @Override
    public RequestItemRest fromModel(RequestItem requestItem) {
        RequestItemRest requestItemRest = new RequestItemRest();
        requestItemRest.setAcceptRequest(requestItem.isAccept_request());
        requestItemRest.setAllfiles(requestItem.isAllfiles());
        requestItemRest.setBitstream(bitstreamConverter.fromModel(requestItem.getBitstream()));
        requestItemRest.setDecisionDate(requestItem.getDecision_date());
        requestItemRest.setExpires(requestItem.getExpires());
        requestItemRest.setId(requestItem.getID());
        requestItemRest.setItem(itemConverter.fromModel(requestItem.getItem()));
        requestItemRest.setReqEmail(requestItem.getReqEmail());
        requestItemRest.setReqMessage(requestItem.getReqMessage());
        requestItemRest.setReqName(requestItem.getReqName());
        requestItemRest.setRequestDate(requestItem.getRequest_date());
        requestItemRest.setToken(requestItem.getToken());
        return requestItemRest;
    }

    @Override
    public RequestItem toModel(RequestItemRest obj) {
        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        Context context = ContextUtil.obtainContext(request);

        // Did we receive a token?  That is:  are we updating an existing request?
        String token = obj.getToken();
        if (StringUtils.isBlank(token)) { // No token, so create a new request.
            try {
                token = requestItemService.createRequest(context,
                        obj.getBitstream(),
                        obj.getItem(),
                        obj.isAllfiles(),
                        obj.getReqEmail(),
                        obj.getReqName(),
                        obj.getReqMessage());
            } catch (SQLException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        } // Otherwise (we have a token) this is updating an existing request.

        RequestItem requestItem = requestItemService.findByToken(context, token);
        requestItem.setAccept_request(obj.isAcceptRequest());
        requestItem.setDecision_date(obj.getDecisionDate());
        return requestItem;
    }
}
