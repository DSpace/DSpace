/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.exception.OrcidClientException;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResearcherProfile ORCID connection.
 *
 * Example: <code><br/>
 * curl -X PATCH http://${dspace.server.url}/api/eperson/profiles/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": "/orcid", value: "code" }]'
 * </code>
 */
@Component
public class ResearcherProfileAddOrcidOperation extends PatchOperation<ResearcherProfile> {

    private static final String OPERATION_ORCID = "/orcid";

    private static final String INVALID_GRANT_MESSAGE = "invalid_grant";

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @Override
    public ResearcherProfile perform(Context context, ResearcherProfile profile, Operation operation)
        throws SQLException {

        Object code = operation.getValue();
        if (code == null | !(code instanceof String)) {
            throw new UnprocessableEntityException("The /code value must be a string");
        }

        OrcidTokenResponseDTO accessToken = getAccessToken((String) code);

        orcidSynchronizationService.linkProfile(context, profile.getItem(), accessToken);

        return profile;
    }

    private OrcidTokenResponseDTO getAccessToken(String code) {
        try {
            return orcidClient.getAccessToken((String) code);
        } catch (OrcidClientException ex) {

            if (ex.getMessage() != null && ex.getMessage().contains(INVALID_GRANT_MESSAGE)) {
                throw new IllegalArgumentException("The provided ORCID authorization code is not valid");
            }

            throw ex;

        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return objectToMatch instanceof ResearcherProfile
            && operation.getOp().trim().equalsIgnoreCase(OPERATION_ADD)
            && operation.getPath().trim().toLowerCase().startsWith(OPERATION_ORCID);
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
