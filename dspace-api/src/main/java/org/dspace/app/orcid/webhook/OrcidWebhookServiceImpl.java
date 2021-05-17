/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.webhook;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.app.orcid.webhook.OrcidWebhookMode.DISABLED;

import java.sql.SQLException;

import org.apache.commons.lang3.EnumUtils;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.service.OrcidWebhookService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidWebhookService}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidWebhookServiceImpl implements OrcidWebhookService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public OrcidWebhookMode getOrcidWebhookMode() {
        String value = configurationService.getProperty("orcid.webhook.registration-mode", "disabled").toUpperCase();
        if (!EnumUtils.isValidEnum(OrcidWebhookMode.class, value)) {
            return DISABLED;
        }
        return OrcidWebhookMode.valueOf(value);
    }

    @Override
    public boolean isProfileRegistered(Item profile) {
        return isNotBlank(itemService.getMetadataFirstValue(profile, "cris", "orcid", "webhook", Item.ANY));
    }

    @Override
    public void register(Context context, Item profile) {

        String accessToken = getAccessToken();
        String orcid = getOrcid(profile);
        String webhookUrl = getWebhookUrl(orcid);
        orcidClient.registerWebhook(accessToken, orcid, webhookUrl);

        String currentDate = ISO_DATE_TIME.format(now());
        try {
            itemService.setMetadataSingleValue(context, profile, "cris", "orcid", "webhook", null, currentDate);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

    }

    @Override
    public void unregister(Context context, Item profile) {

        unregister(context, getOrcid(profile));

        try {
            itemService.clearMetadata(context, profile, "cris", "orcid", "webhook", Item.ANY);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

    }

    @Override
    public void unregister(Context context, String orcid) {
        String accessToken = getAccessToken();
        String webhookUrl = getWebhookUrl(orcid);
        orcidClient.unregisterWebhook(accessToken, orcid, webhookUrl);
    }

    private String getOrcid(Item profile) {
        String orcid = itemService.getMetadataFirstValue(profile, "person", "identifier", "orcid", Item.ANY);
        if (isBlank(orcid)) {
            throw new IllegalArgumentException("The given profile " + profile.getID() + " does not have an ORCID id. ");
        }
        return orcid;
    }

    private String getAccessToken() {
        return orcidClient.getWebhookAccessToken().getAccessToken();
    }

    private String getWebhookUrl(String orcid) {
        String server = configurationService.getProperty("dspace.server.url");
        String token = configurationService.getProperty("orcid.webhook.registration-token");
        return server + "/api/cris/orcid/" + orcid + "/webhook/" + token;
    }

    public OrcidClient getOrcidClient() {
        return orcidClient;
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
