/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.app.rest.model.SubscriptionRest.NAME;
import static org.dspace.app.rest.security.DSpaceRestPermission.DELETE;
import static org.dspace.app.rest.security.DSpaceRestPermission.READ;
import static org.dspace.app.rest.security.DSpaceRestPermission.WRITE;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * {@link RestPermissionEvaluatorPlugin} class that evaluate READ, WRITE and DELETE permissions over a Subscription
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@Component
public class SubscriptionRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;
    @Autowired
    private SubscribeService subscribeService;
    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);

        if (!READ.equals(restPermission) && !WRITE.equals(restPermission) && !DELETE.equals(restPermission)
            || !StringUtils.equalsIgnoreCase(targetType, NAME)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        try {
            EPerson currentUser = context.getCurrentUser();
            // anonymous user
            if (Objects.isNull(currentUser)) {
                return false;
            }
            // Admin user
            if (authorizeService.isAdmin(context, currentUser)) {
                return true;
            }

            Subscription subscription = subscribeService.findById(context, Integer.parseInt(targetId.toString()));
            return Objects.nonNull(subscription) ? currentUser.equals(subscription.getEPerson()) : false;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
