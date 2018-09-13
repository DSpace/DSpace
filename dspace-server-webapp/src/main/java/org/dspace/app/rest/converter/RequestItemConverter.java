/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.converter;

import javax.inject.Named;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Convert between {@link org.dspace.app.requestitem.RequestItem} and
 * {@link org.dspace.app.rest.model.RequestItemRest}.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Named
public class RequestItemConverter
        implements DSpaceConverter<RequestItem, RequestItemRest> {
    @Autowired(required=true)
    protected BitstreamConverter bitstreamConverter;

    @Autowired(required=true)
    protected ItemConverter itemConverter;

    @Autowired(required=true)
    protected RequestItemService requestItemService;

    @Autowired(required=true)
    protected RequestService requestService;

    @Override
    public RequestItemRest convert(RequestItem requestItem, Projection projection) {
        RequestItemRest requestItemRest = new RequestItemRest();
        requestItemRest.setProjection(projection);

        requestItemRest.setAcceptRequest(requestItem.isAccept_request());
        requestItemRest.setAllfiles(requestItem.isAllfiles());
        requestItemRest.setBitstream(
                bitstreamConverter.convert(requestItem.getBitstream(), projection));
        requestItemRest.setDecisionDate(requestItem.getDecision_date());
        requestItemRest.setExpires(requestItem.getExpires());
        requestItemRest.setId(requestItem.getID());
        requestItemRest.setItem(
                itemConverter.convert(requestItem.getItem(), projection));
        requestItemRest.setReqEmail(requestItem.getReqEmail());
        requestItemRest.setReqMessage(requestItem.getReqMessage());
        requestItemRest.setReqName(requestItem.getReqName());
        requestItemRest.setRequestDate(requestItem.getRequest_date());
        requestItemRest.setToken(requestItem.getToken());
        return requestItemRest;
    }

    @Override
    public Class<RequestItem> getModelClass() {
        return RequestItem.class;
    }
}
