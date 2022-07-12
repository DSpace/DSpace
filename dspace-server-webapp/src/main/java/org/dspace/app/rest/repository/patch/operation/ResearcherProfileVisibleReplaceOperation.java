/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResearcherProfile visibility patches.
 *
 * Example:
 * <code> curl -X PATCH http://${dspace.server.url}/api/eperson/profiles/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /visible", "value": true]'
 * </code>
 */
@Component
public class ResearcherProfileVisibleReplaceOperation extends PatchOperation<ResearcherProfile> {

    @Autowired
    private ResearcherProfileService researcherProfileService;

    /**
     * Path in json body of patch that uses this operation.
     */
    public static final String OPERATION_VISIBLE_CHANGE = "/visible";

    @Override
    public ResearcherProfile perform(Context context, ResearcherProfile profile, Operation operation)
        throws SQLException {

        Object value = operation.getValue();
        if (value == null | !(value instanceof Boolean)) {
            throw new UnprocessableEntityException("The /visible value must be a boolean (true|false)");
        }

        try {
            researcherProfileService.changeVisibility(context, profile, (boolean) value);
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException("Unauthorized user for profile visibility change");
        }

        return profile;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return (objectToMatch instanceof ResearcherProfile
            && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
            && operation.getPath().trim().equalsIgnoreCase(OPERATION_VISIBLE_CHANGE));
    }

}
