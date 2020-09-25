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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class OrcidHistorySendToOrcidRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    @Autowired
    private RequestService requestService;

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Autowired
    private ItemService itemService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.ADD.equals(restPermission)) {
            return false;
        }
        if (!targetType.equals(OrcidQueueRestPermissionEvaluatorPlugin.ORCID)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());

        EPerson currentUser = context.getCurrentUser();
        String url = targetId.toString();
        Pattern pattern = Pattern.compile("\\[\\/api\\/cris\\/orcidqueues\\/(.*)\\]");
        Matcher matcher = pattern.matcher(url);

        matcher.find();
        String id = matcher.group(1);
        Integer queueId = Integer.parseInt(id);
        OrcidQueue orcidQueue = null;
        try {
            orcidQueue = orcidQueueService.find(context, queueId);
            // anonymous user
            if (currentUser == null) {
                return false;
            } else if (orcidQueue == null) {
                return true;
            } else if (hasAccess(context, currentUser, orcidQueue)) {
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return false;
    }

    private boolean hasAccess(Context context, EPerson currentUser, OrcidQueue orcidQueue) {
        if (orcidQueue != null) {
            List<MetadataValue> value = itemService.getMetadata(orcidQueue.getOwner(), "cris", "owner", null, null);
            if (value.get(0).getAuthority().equals(currentUser.getID().toString())) {
                return true;
            }
        }
        return false;
    }

}
