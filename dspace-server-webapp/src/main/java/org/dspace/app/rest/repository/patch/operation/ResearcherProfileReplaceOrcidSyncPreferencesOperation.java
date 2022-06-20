/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import static org.dspace.orcid.model.OrcidEntityType.FUNDING;
import static org.dspace.orcid.model.OrcidEntityType.PUBLICATION;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.service.OrcidQueueService;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.profile.OrcidEntitySyncPreference;
import org.dspace.profile.OrcidProfileSyncPreference;
import org.dspace.profile.OrcidSynchronizationMode;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation for ResearcherProfile ORCID synchronization preferences
 * patches.
 *
 * Example:
 * <code> curl -X PATCH http://${dspace.server.url}/api/eperson/profiles/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{
 *  "op": "replace",
 *  "path": "/orcid/publications",
 *  "value": "ALL"
 *  }]'
 * </code>
 */
@Component
public class ResearcherProfileReplaceOrcidSyncPreferencesOperation extends PatchOperation<ResearcherProfile> {

    private static final String OPERATION_ORCID_SYNCH = "/orcid";

    private static final String PUBLICATIONS_PREFERENCES = "/publications";

    private static final String FUNDINGS_PREFERENCES = "/fundings";

    private static final String PROFILE_PREFERENCES = "/profile";

    private static final String MODE_PREFERENCES = "/mode";

    @Autowired
    private ResearcherProfileService profileService;

    @Autowired
    private OrcidSynchronizationService synchronizationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Override
    public ResearcherProfile perform(Context context, ResearcherProfile profile, Operation operation)
        throws SQLException {

        String path = StringUtils.removeStart(operation.getPath(), OPERATION_ORCID_SYNCH);
        String value = getNewValueFromOperation(operation);

        Item profileItem = profile.getItem();

        // Permission check already performed on patch endpoint invocation
        context.turnOffAuthorisationSystem();

        try {

            boolean updated = updatePreferences(context, path, value, profileItem);

            if (updated) {
                reloadOrcidQueue(context, path, value, profileItem);
            }

            return profileService.findById(context, profile.getId());

        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    private String getNewValueFromOperation(Operation operation) {
        Object valueObject = operation.getValue();
        if (valueObject == null | !(valueObject instanceof String)) {
            throw new UnprocessableEntityException("The /orcid value must be a string");
        }
        return (String) valueObject;
    }

    /**
     * Update the ORCID synchronization preference related to the given path.
     * Returns true if the value has actually been updated, false if the value to be
     * set is the same as the one already configured.
     */
    private boolean updatePreferences(Context context, String path, String value, Item profileItem)
        throws SQLException {
        switch (path) {
            case PUBLICATIONS_PREFERENCES:
                OrcidEntitySyncPreference preference = parsePreference(value);
                return synchronizationService.setEntityPreference(context, profileItem, PUBLICATION, preference);
            case FUNDINGS_PREFERENCES:
                OrcidEntitySyncPreference fundingPreference = parsePreference(value);
                return synchronizationService.setEntityPreference(context, profileItem, FUNDING, fundingPreference);
            case PROFILE_PREFERENCES:
                List<OrcidProfileSyncPreference> profilePreferences = parseProfilePreferences(value);
                return synchronizationService.setProfilePreference(context, profileItem, profilePreferences);
            case MODE_PREFERENCES:
                return synchronizationService.setSynchronizationMode(context, profileItem, parseMode(value));
            default:
                throw new UnprocessableEntityException("Invalid path starting with " + OPERATION_ORCID_SYNCH);
        }
    }

    private void reloadOrcidQueue(Context context, String path, String value, Item profileItem)
        throws SQLException, AuthorizeException {

        if (path.equals(PUBLICATIONS_PREFERENCES) || path.equals(FUNDINGS_PREFERENCES)) {
            OrcidEntitySyncPreference preference = parsePreference(value);
            OrcidEntityType entityType = path.equals(PUBLICATIONS_PREFERENCES) ? PUBLICATION : FUNDING;
            orcidQueueService.recalculateOrcidQueue(context, profileItem, entityType, preference);
        }

        itemService.update(context, profileItem);
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

    private OrcidSynchronizationMode parseMode(String value) {
        try {
            return OrcidSynchronizationMode.valueOf(value.toUpperCase());
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
