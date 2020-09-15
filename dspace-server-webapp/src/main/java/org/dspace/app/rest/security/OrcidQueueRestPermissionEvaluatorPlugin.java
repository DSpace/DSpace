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
import org.dspace.app.orcid.OrcidQueue;
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

        EPerson ePerson = null;
        try {
            ePerson = context.getCurrentUser();
            Integer orcidQueueId = Integer.parseInt(targetId.toString());
            OrcidQueue orcidQueue = orcidQueueService.find(context, orcidQueueId);

            // anonymous user
            if (ePerson == null) {
                return false;
            } else if (orcidQueueId == null || StringUtils.isBlank(targetId.toString()) || orcidQueue == null) {
                return true;
            } else if (hasAccess(ePerson, orcidQueue)) {
                return true;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private boolean hasAccess(EPerson ePerson, OrcidQueue orcidQueue) {
        List<MetadataValue> values = itemService.getMetadata(orcidQueue.getOwner(), "cris", "owner", null, null);
        if (values.get(0).getAuthority().equals(ePerson.getID().toString())) {
            return true;
        }
        return false;
    }

}
