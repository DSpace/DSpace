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
import java.util.UUID;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.TemplateItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * {@link RestObjectPermissionEvaluatorPlugin} class that evaluate READ, WRITE and DELETE permission over a
 * TemplateItem. READ follows the nested collection endpoint and requires ADD on the owning collection.
 *
 * @author Bui Thai Hai (thaihai.bui@dlcorp.com.vn)
 */
@Component
public class TemplateItemRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private RequestService requestService;

    @Autowired
    ItemService its;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission) &&
                !DSpaceRestPermission.WRITE.equals(restPermission) &&
                !DSpaceRestPermission.DELETE.equals(restPermission)) {
            return false;
        }
        if (!Strings.CI.equals(targetType, TemplateItemRest.NAME)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        EPerson ePerson = context.getCurrentUser();
        UUID dsoId = UUID.fromString(targetId.toString());
        if (DSpaceRestPermission.READ.equals(restPermission)) {
            return hasReadPermission(context, ePerson, dsoId);
        }
        if (ePerson == null) {
            return false;
        }
        // Allow collection's admin to edit/delete the template

        requestService.getCurrentRequest().getHttpServletRequest().getRequestURL();
        try {
            Collection coll = its.find(context, dsoId).getTemplateItemOf();
            if (authorizeService.isAdmin(context, coll)) {
                return true;
            }
        } catch (SQLException e) {
            log.error(e::getMessage, e);
        }
        return false;
    }

    /**
     * Reading a template item requires ADD on its collection, the same as
     * GET /api/core/collections/{uuid}/itemtemplate. A missing item or an item that is not a template is allowed
     * through, so that the endpoint can answer with 404.
     */
    private boolean hasReadPermission(Context context, EPerson ePerson, UUID itemId) {
        try {
            Item item = its.find(context, itemId);
            if (item == null || item.getTemplateItemOf() == null) {
                return true;
            }
            return authorizeService.authorizeActionBoolean(context, ePerson, item.getTemplateItemOf(),
                                                           Constants.ADD, true);
        } catch (SQLException e) {
            log.error(e::getMessage, e);
        }
        return false;
    }
}
