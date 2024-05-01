/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.converter;

import jakarta.inject.Named;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;

/**
 * Convert between {@link org.dspace.app.requestitem.RequestItem} and
 * {@link org.dspace.app.rest.model.RequestItemRest}.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Named
public class RequestItemConverter
        implements DSpaceConverter<RequestItem, RequestItemRest> {
    @Override
    public RequestItemRest convert(RequestItem requestItem, Projection projection) {
        RequestItemRest requestItemRest = new RequestItemRest();
        requestItemRest.setProjection(projection);

        requestItemRest.setAcceptRequest(requestItem.isAccept_request());
        requestItemRest.setAllfiles(requestItem.isAllfiles());
        Bitstream bitstream = requestItem.getBitstream();
        if (null == bitstream) {
            requestItemRest.setBitstreamId(null);
        } else {
            requestItemRest.setBitstreamId(requestItem.getBitstream().getID().toString());
        }
        requestItemRest.setDecisionDate(requestItem.getDecision_date());
        requestItemRest.setExpires(requestItem.getExpires());
        requestItemRest.setId(requestItem.getID());
        requestItemRest.setItemId(requestItem.getItem().getID().toString());
        requestItemRest.setRequestEmail(requestItem.getReqEmail());
        requestItemRest.setRequestMessage(requestItem.getReqMessage());
        requestItemRest.setRequestName(requestItem.getReqName());
        requestItemRest.setRequestDate(requestItem.getRequest_date());
        requestItemRest.setToken(requestItem.getToken());
        return requestItemRest;
    }

    @Override
    public Class<RequestItem> getModelClass() {
        return RequestItem.class;
    }
}
