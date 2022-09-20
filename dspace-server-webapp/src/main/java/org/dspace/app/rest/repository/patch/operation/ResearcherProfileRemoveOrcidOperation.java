/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import static org.dspace.profile.OrcidProfileDisconnectionMode.ADMIN_AND_OWNER;
import static org.dspace.profile.OrcidProfileDisconnectionMode.DISABLED;
import static org.dspace.profile.OrcidProfileDisconnectionMode.ONLY_ADMIN;
import static org.dspace.profile.OrcidProfileDisconnectionMode.ONLY_OWNER;

import java.sql.SQLException;

import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.profile.OrcidProfileDisconnectionMode;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResearcherProfile ORCID disconnection.
 *
 * Example: <code><br/>
 * curl -X PATCH http://${dspace.server.url}/api/eperson/profiles/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "remove", "path": "/orcid" }]'
 * </code>
 */
@Component
public class ResearcherProfileRemoveOrcidOperation extends PatchOperation<ResearcherProfile> {

    private static final String OPERATION_ORCID = "/orcid";

    @Autowired
    private ResearcherProfileService profileService;

    @Autowired
    private OrcidSynchronizationService synchronizationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public ResearcherProfile perform(Context context, ResearcherProfile profile, Operation operation)
        throws SQLException {

        checkProfileDisconnectionPermissions(context, profile);

        synchronizationService.unlinkProfile(context, profile.getItem());

        try {
            return profileService.findById(context, profile.getId());
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        }

    }

    private void checkProfileDisconnectionPermissions(Context context, ResearcherProfile profile) throws SQLException {

        OrcidProfileDisconnectionMode mode = synchronizationService.getDisconnectionMode();

        if (mode == ADMIN_AND_OWNER) {
            return;
        }

        if (mode == DISABLED) {
            throw new RESTAuthorizationException("Profile disconnection from ORCID is disabled");
        }

        if (mode == ONLY_OWNER && isNotOwner(context, profile)) {
            throw new RESTAuthorizationException("Only the profile's owner can perform the ORCID disconnection");
        }

        if (mode == ONLY_ADMIN && isNotAdmin(context)) {
            throw new RESTAuthorizationException("Only admins can perform the profile disconnection from ORCID");
        }

    }

    private boolean isNotAdmin(Context context) throws SQLException {
        return !authorizeService.isAdmin(context);
    }

    private boolean isNotOwner(Context context, ResearcherProfile profile) {
        EPerson currentUser = context.getCurrentUser();
        return currentUser == null || !currentUser.getID().equals(profile.getId());
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return objectToMatch instanceof ResearcherProfile
            && operation.getOp().trim().equalsIgnoreCase(OPERATION_REMOVE)
            && operation.getPath().trim().toLowerCase().startsWith(OPERATION_ORCID);
    }

}
