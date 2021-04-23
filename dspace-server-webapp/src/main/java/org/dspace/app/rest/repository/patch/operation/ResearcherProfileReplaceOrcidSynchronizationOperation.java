/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.app.profile.OrcidEntitySyncPreference;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.app.profile.OrcidSyncMode;
import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResearcherProfile ORCID synchronization preferences
 * patches.
 *
 * Example:
 * <code> curl -X PATCH http://${dspace.server.url}/api/cris/profiles/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{
 *  "op": "replace",
 *  "path": "/orcid/publications",
 *  "value": "ALL"
 *  }]'
 * </code>
 */
@Component
public class ResearcherProfileReplaceOrcidSynchronizationOperation extends PatchOperation<ResearcherProfile> {

    private static final String OPERATION_ORCID_SYNCH = "/orcid";

    private static final String PUBLICATIONS_PREFERENCES = "/publications";

    private static final String PROJECTS_PREFERENCES = "/projects";

    private static final String PROFILE_PREFERENCES = "/profile";

    private static final String MODE_PREFERENCES = "/mode";

    @Autowired
    private ResearcherProfileService profileService;

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @Override
    public ResearcherProfile perform(Context context, ResearcherProfile profile, Operation operation)
        throws SQLException {

        String path = StringUtils.removeStart(operation.getPath(), OPERATION_ORCID_SYNCH);
        Object valueObject = operation.getValue();
        if (valueObject == null | !(valueObject instanceof String)) {
            throw new UnprocessableEntityException("The /orcid value must be a string");
        }

        String value = (String) valueObject;

        Item profileItem = profile.getItem();

        switch (path) {
            case PUBLICATIONS_PREFERENCES:
                orcidSynchronizationService.setPublicationPreference(context, profileItem, parsePreference(value));
                break;
            case PROJECTS_PREFERENCES:
                orcidSynchronizationService.setProjectPreference(context, profileItem, parsePreference(value));
                break;
            case PROFILE_PREFERENCES:
                orcidSynchronizationService.setProfilePreference(context, profileItem, parseProfilePreferences(value));
                break;
            case MODE_PREFERENCES:
                orcidSynchronizationService.setSynchronizationMode(context, profileItem, parseMode(value));
                break;
            default:
                throw new UnprocessableEntityException("Invalid path starting with " + OPERATION_ORCID_SYNCH);
        }

        try {
            return profileService.findById(context, profile.getId());
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        }
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return objectToMatch instanceof ResearcherProfile
            && operation.getOp().trim().equalsIgnoreCase(OPERATION_REPLACE)
            && operation.getPath().trim().toLowerCase().startsWith(OPERATION_ORCID_SYNCH);
    }

    private List<OrcidProfileSyncPreference> parseProfilePreferences(String value) {
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotEmpty)
            .map(this::parseProfilePreference)
            .collect(Collectors.toList());
    }

    private OrcidProfileSyncPreference parseProfilePreference(String value) {
        try {
            return OrcidProfileSyncPreference.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnprocessableEntityException("Invalid profile's synchronization preference value: " + value, ex);
        }
    }

    private OrcidSyncMode parseMode(String value) {
        try {
            return OrcidSyncMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnprocessableEntityException("Invalid synchronization mode value: " + value, ex);
        }
    }

    private OrcidEntitySyncPreference parsePreference(String value) {
        try {
            return OrcidEntitySyncPreference.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnprocessableEntityException("Invalid synchronization preference value: " + value, ex);
        }
    }

}
