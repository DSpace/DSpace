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
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
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
 * Permission evaluator plugin that check if the current user can search for
 * ORCID queue records by owner.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class OrcidQueueSearchRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(OrcidQueueAndHistoryRestPermissionEvaluatorPlugin.class);

    public static final String ORCID_QUEUE_SEARCH = "ORCID_QUEUE_SEARCH";

    @Autowired
    private RequestService requestService;

    @Autowired
    private ItemService itemService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission)) {
            return false;
        }
        if (!targetType.equals(ORCID_QUEUE_SEARCH)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());
        EPerson ePerson = null;
        try {
            ePerson = context.getCurrentUser();
            UUID ownerUUID = UUID.fromString(targetId.toString());
            Item owner = itemService.find(context, ownerUUID);

            // anonymous user
            if (ePerson == null) {
                return false;
            } else if (owner == null || StringUtils.isBlank(targetId.toString()) || ownerUUID == null) {
                return false;
            } else if (hasAccess(ePerson, owner)) {
                return true;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private boolean hasAccess(EPerson ePerson, Item owner) {
        List<MetadataValue> values = itemService.getMetadata(owner, "dspace", "object", "owner", Item.ANY);
        if (values.get(0).getAuthority().equals(ePerson.getID().toString())) {
            return true;
        }
        return false;
    }

}
