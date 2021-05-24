/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.webhook;

import static java.lang.String.format;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.exception.OrcidClientException;
import org.dspace.app.orcid.service.OrcidWebhookService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Implementation of {@link OrcidWebhookAction} that check if the ORCID access
 * token related to the given profile, if any, is still valid.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CheckOrcidAuthorization implements OrcidWebhookAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckOrcidAuthorization.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private OrcidWebhookService orcidWebhookService;

    @Override
    public void perform(Context context, Item profile, String orcid) {
        String accessToken = getAccessToken(profile);
        if (StringUtils.isBlank(accessToken)) {
            return;
        }

        try {

            if (isAccessTokenExpired(accessToken, orcid)) {

                removeAccessToken(context, profile);

                if (isWebhookConfiguredForOnlyLinkedProfiles() && orcidWebhookService.isProfileRegistered(profile)) {
                    orcidWebhookService.unregister(context, profile);
                }

            }

        } catch (Exception ex) {
            LOGGER.error(format("An error occurs checking the access token %s related "
                + "to the orcid %s (profile id %s)", accessToken, orcid, profile.getID()));
        }

    }

    private String getAccessToken(Item profile) {
        return itemService.getMetadataFirstValue(profile, "cris", "orcid", "access-token", Item.ANY);
    }

    private void removeAccessToken(Context context, Item profile) throws SQLException {
        itemService.clearMetadata(context, profile, "cris", "orcid", "access-token", Item.ANY);
        itemService.clearMetadata(context, profile, "cris", "orcid", "authenticated", Item.ANY);
    }

    private boolean isAccessTokenExpired(String accessToken, String orcid) {
        try {
            return orcidClient.getPerson(accessToken, orcid) == null;
        } catch (OrcidClientException ex) {
            if (ex.getStatus() != HttpStatus.SC_UNAUTHORIZED) {
                throw ex;
            }
            return true;
        }
    }

    private boolean isWebhookConfiguredForOnlyLinkedProfiles() {
        return orcidWebhookService.getOrcidWebhookMode() == OrcidWebhookMode.ONLY_LINKED;
    }

    public OrcidClient getOrcidClient() {
        return orcidClient;
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
