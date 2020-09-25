/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Class that evaluate DELETE and READ permissions
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class OrcidQueueRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(OrcidQueueRestPermissionEvaluatorPlugin.class);

    public static final String ORCID = "ORCID";

    @Autowired
    private RequestService requestService;

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Autowired
    private OrcidHistoryService orcidHistoryService;

    @Autowired
    private ItemService itemService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission) &&
            !DSpaceRestPermission.DELETE.equals(restPermission)) {
            return false;
        }
        if (!targetType.equals(ORCID)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());

        EPerson currentUser = context.getCurrentUser();
        Integer orcidObjectId = Integer.parseInt(targetId.toString());

        // anonymous user
        if (currentUser == null) {
            return false;
        } else if (StringUtils.isBlank(targetId.toString())) {
            return true;
        } else if (hasAccess(context, currentUser, orcidObjectId)) {
            return true;
        }
        return false;
    }

    private boolean hasAccess(Context context, EPerson currentUser, Integer orcidObjectId) {
        try {
            OrcidQueue orcidQueue = orcidQueueService.find(context, orcidObjectId);
            OrcidHistory orcidHistory = orcidHistoryService.find(context, orcidObjectId);
            if ((orcidQueue == null && orcidHistory == null)) {
                return true;
            }
            if (orcidQueue != null) {
                List<MetadataValue> value = itemService.getMetadata(orcidQueue.getOwner(), "cris", "owner", null, null);
                if (value.get(0).getAuthority().equals(currentUser.getID().toString())) {
                    return true;
                }
            } else {
                List<MetadataValue> value = itemService.getMetadata(orcidHistory.getOwner(),"cris","owner", null, null);
                if (value.get(0).getAuthority().equals(currentUser.getID().toString())) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
