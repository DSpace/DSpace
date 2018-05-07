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

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * An authenicated user is allowed to view and update his or her own data. This {@link RestPermissionEvaluatorPlugin}
 * implemenents that requirement.
 */
@Component
public class EPersonRestPermissionEvaluatorPlugin extends DSpaceObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(EPersonRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;

    /**
     * Alternative method for evaluating a permission where only the identifier of the
     * target object is available, rather than the target instance itself.
     *
     * @param authentication represents the user in question. Should not be null.
     * @param targetId the UUID for the DSpace object
     * @param targetType represents the DSpace object type of the target object. Not null.
     * @param permission a representation of the permission object as supplied by the
     * expression system. This corresponds to the DSpace action. Not null.
     * @return true if the permission is granted by one of the plugins, false otherwise
     */
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        EPerson ePerson = null;
        try {
            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());
            UUID dsoId = UUID.fromString(targetId.toString());

            if (Constants.getTypeID(targetType) == Constants.EPERSON) {
                if (dsoId.equals(ePerson.getID())) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
