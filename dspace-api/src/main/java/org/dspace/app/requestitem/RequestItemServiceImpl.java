/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.dao.RequestItemDAO;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.core.Utils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the RequestItem object.
 * This class is responsible for all business logic calls for the RequestItem
 * object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class RequestItemServiceImpl implements RequestItemService {

    private final Logger log = LogManager.getLogger();

    @Autowired(required = true)
    protected RequestItemDAO requestItemDAO;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;

    protected RequestItemServiceImpl() {

    }

    @Override
    public String createRequest(Context context, Bitstream bitstream, Item item,
            boolean allFiles, String reqEmail, String reqName, String reqMessage)
            throws SQLException {
        RequestItem requestItem = requestItemDAO.create(context, new RequestItem());

        requestItem.setToken(Utils.generateHexKey());
        requestItem.setBitstream(bitstream);
        requestItem.setItem(item);
        requestItem.setAllfiles(allFiles);
        requestItem.setReqEmail(reqEmail);
        requestItem.setReqName(reqName);
        requestItem.setReqMessage(reqMessage);
        requestItem.setRequest_date(new Date());

        requestItemDAO.save(context, requestItem);

        log.debug("Created RequestItem with ID {} and token {}",
                requestItem::getID, requestItem::getToken);
        return requestItem.getToken();
    }

    @Override
    public List<RequestItem> findAll(Context context)
            throws SQLException {
        return requestItemDAO.findAll(context, RequestItem.class);
    }

    @Override
    public RequestItem findByToken(Context context, String token) {
        try {
            return requestItemDAO.findByToken(context, token);
        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public Iterator<RequestItem> findByItem(Context context, Item item) throws SQLException {
        return requestItemDAO.findByItem(context, item);
    }

    @Override
    public void update(Context context, RequestItem requestItem) {
        try {
            requestItemDAO.save(context, requestItem);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void delete(Context context, RequestItem requestItem) {
        log.debug(LogHelper.getHeader(context, "delete_itemrequest", "request_id={}"),
                requestItem.getID());
        try {
            requestItemDAO.delete(context, requestItem);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public boolean isRestricted(Context context, DSpaceObject o)
            throws SQLException {
        List<ResourcePolicy> policies = authorizeService
                .getPoliciesActionFilter(context, o, Constants.READ);
        for (ResourcePolicy rp : policies) {
            if (resourcePolicyService.isDateValid(rp)) {
                return false;
            }
        }
        return true;
    }
}
