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

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.OrcidHistory;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.service.OrcidHistoryService;
import org.dspace.orcid.service.OrcidQueueService;
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
public class OrcidQueueAndHistoryRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(OrcidQueueAndHistoryRestPermissionEvaluatorPlugin.class);

    public static final String ORCID_QUEUE = "ORCID_QUEUE";
    public static final String ORCID_HISTORY = "ORCID_HISTORY";

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
        if (!ORCID_QUEUE.equals(targetType) && !ORCID_HISTORY.equals(targetType)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        EPerson currentUser = context.getCurrentUser();
        Integer orcidObjectId = Integer.parseInt(targetId.toString());

        // anonymous user
        if (currentUser == null || currentUser.getID() == null) {
            return false;
        } else if (StringUtils.isBlank(targetId.toString())) {
            return true;
        } else if (hasAccess(context, currentUser, orcidObjectId, targetType.equals(ORCID_QUEUE))) {
            return true;
        }
        return false;
    }

    private boolean hasAccess(Context context, EPerson currentUser, Integer orcidObjectId, boolean isOrcidQueueRecord) {

        try {

            Item profileItem = findProfileItem(context, orcidObjectId, isOrcidQueueRecord);
            if (profileItem == null) {
                return true;
            }

            return itemService.getMetadata(profileItem, "dspace", "object", "owner", Item.ANY).stream()
                .map(metadataValue -> metadataValue.getAuthority())
                .anyMatch(authority -> currentUser.getID().toString().equals(authority));

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    private Item findProfileItem(Context context, Integer orcidObjectId, boolean isOrcidQueueRecord)
        throws SQLException {
        if (isOrcidQueueRecord) {
            OrcidQueue orcidQueue = orcidQueueService.find(context, orcidObjectId);
            return orcidQueue != null ? orcidQueue.getProfileItem() : null;
        } else {
            OrcidHistory orcidHistory = orcidHistoryService.find(context, orcidObjectId);
            return orcidHistory != null ? orcidHistory.getProfileItem() : null;
        }
    }

}
