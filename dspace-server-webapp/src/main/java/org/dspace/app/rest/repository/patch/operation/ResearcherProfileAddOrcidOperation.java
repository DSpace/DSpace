/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.profile.ResearcherProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of the PATCH operation used to establish the ORCID connection.
 *
 * Example: <code><br/>
 * curl -X PATCH http://${dspace.server.url}/api/eperson/profiles/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "add", "path": "/orcid", value: "code" }]'
 * </code> <br/>
 * The value to be provided is the authorization code for an ORCID iD and
 * 3-legged access token.
 */
@Component
public class ResearcherProfileAddOrcidOperation extends PatchOperation<ResearcherProfile> {

    private static final String OPERATION_ORCID = "/orcid";

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

            if (ex.isInvalidGrantException()) {
                throw new UnprocessableEntityException("The provided ORCID authorization code is not valid", ex);
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
