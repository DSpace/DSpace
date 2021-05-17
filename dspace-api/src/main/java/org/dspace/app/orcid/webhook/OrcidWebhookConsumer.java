/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.webhook;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.app.orcid.webhook.OrcidWebhookMode.ALL;
import static org.dspace.app.orcid.webhook.OrcidWebhookMode.DISABLED;

import java.util.HashSet;
import java.util.Set;

import org.dspace.app.orcid.factory.OrcidServiceFactory;
import org.dspace.app.orcid.service.OrcidWebhookService;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Implementation of {@link Consumer} that perform registrations and
 * unregistration from ORCID webhook.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidWebhookConsumer implements Consumer {

    private Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ItemService itemService;

    private OrcidWebhookService orcidWebhookService;

    private ConfigurationService configurationService;

    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.orcidWebhookService = OrcidServiceFactory.getInstance().getOrcidWebhookService();
    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        OrcidWebhookMode webhookConfiguration = orcidWebhookService.getOrcidWebhookMode();
        if (webhookConfiguration == DISABLED) {
            return;
        }

        Item item = (Item) event.getSubject(context);
        if (item == null || itemsAlreadyProcessed.contains(item) || !item.isArchived()) {
            return;
        }

        itemsAlreadyProcessed.add(item);

        if (isNotProfile(item)) {
            return;
        }

        boolean isWebhookAlreadyRegistered = orcidWebhookService.isProfileRegistered(item);
        boolean hasRequiredOrcidMetadata = hasRequiredOrcidMetadata(item, webhookConfiguration);

        if (!isWebhookAlreadyRegistered && hasRequiredOrcidMetadata) {
            orcidWebhookService.register(context, item);
        }

    }

    private boolean isNotProfile(Item item) {
        return !getProfileType().equals(itemService.getEntityType(item));
    }

    private boolean hasRequiredOrcidMetadata(Item item, OrcidWebhookMode webhookConfiguration) {
        boolean hasOrcidId = isNotBlank(getMetadataFirstValue(item, "person.identifier.orcid"));
        boolean hasAccessToken = isNotBlank(getMetadataFirstValue(item, "cris.orcid.access-token"));
        return webhookConfiguration == ALL ? hasOrcidId : hasOrcidId && hasAccessToken;
    }

    private String getProfileType() {
        return configurationService.getProperty("researcher-profile.type", "Person");
    }

    private String getMetadataFirstValue(Item item, String metadataField) {
        return itemService.getMetadataFirstValue(item, new MetadataFieldName(metadataField), Item.ANY);
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
    }

}
