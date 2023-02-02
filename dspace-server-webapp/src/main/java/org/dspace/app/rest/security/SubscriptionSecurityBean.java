/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Methods of this class are used on PreAuthorize annotations
 * to check security on subscriptions endpoint
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(value = "subscriptionSecurity")
public class SubscriptionSecurityBean {

    @Autowired
    private EPersonService ePersonService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;

    public boolean isEnabelToCreateSubscription(Context context) throws SQLException {
        RequestService requestService = new DSpace().getRequestService();
        HttpServletRequest req = requestService.getCurrentRequest().getHttpServletRequest();
        String epersonId = req.getParameter("eperson_id");
        String dsoId = req.getParameter("resource");

        if (Objects.isNull(dsoId) || Objects.isNull(epersonId)) {
            return true;
        }

        try {
            DSpaceObject dSpaceObject = dspaceObjectUtil.findDSpaceObject(context, UUID.fromString(dsoId));
            EPerson ePerson = ePersonService.findByIdOrLegacyId(context, epersonId);
            if (Objects.isNull(ePerson) || Objects.isNull(dSpaceObject)) {
                return true;
            }

            if (!authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObject,  Constants.READ, true)) {
                return false;
            }

            if (!authorizeService.isAdmin(context)) {
                if (!ePerson.equals(context.getCurrentUser())) {
                    return false;
                }
            }
        } catch (SQLException sqlException) {
            throw new SQLException(sqlException.getMessage(), sqlException);
        }
        return true;
    }

}
