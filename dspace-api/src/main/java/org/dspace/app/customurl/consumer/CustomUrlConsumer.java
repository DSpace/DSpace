/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.customurl.consumer;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.commons.lang3.StringUtils.stripAccents;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.dspace.app.customurl.CustomUrlService;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link Consumer} to set a default value for the item's
 * custom url.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrlConsumer implements Consumer {

    private final Set<Item> itemsAlreadyProcessed = new HashSet<Item>();

    private ItemService itemService;

    private CustomUrlService customUrlService;

    private ConfigurationService configurationService;

    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.customUrlService = new DSpace().getSingletonService(CustomUrlService.class);
    }

    @Override
    public void finish(Context ctx) throws Exception {

    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        Item item = (Item) event.getSubject(context);
        if (item == null || itemsAlreadyProcessed.contains(item) || !item.isArchived()) {
            return;
        }

        context.turnOffAuthorisationSystem();
        try {
            consume(context, item);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void consume(Context context, Item item) {
        if (isEntityTypeNotSupported(item) || hasAlreadyCustomUrl(item)) {
            return;
        }

        generateCustomUrl(item).ifPresent(customUrl -> addCustomUrl(context, item, customUrl));
    }

    private boolean isEntityTypeNotSupported(Item item) {
        String entityType = itemService.getEntityTypeLabel(item);
        return entityType == null || !contains(getSupportedEntities(), entityType);
    }

    private boolean hasAlreadyCustomUrl(Item item) {
        return customUrlService.getCustomUrl(item).isPresent();
    }

    private Optional<String> generateCustomUrl(Item item) {
        String title = itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
        if (isBlank(title)) {
            return Optional.empty();
        }

        title = removeInvalidCharacters(title);
        if (isBlank(title)) {
            return Optional.empty();
        }

        return Optional.of(title);
    }

    private void addCustomUrl(Context context, Item item, String customUrl) {
        customUrlService.replaceCustomUrl(context, item, customUrl);
    }

    private String removeInvalidCharacters(String title) {
        title = stripAccents(title).toLowerCase().replaceAll("[^a-z0-9 ,]", "");
        title = title.replaceAll(",", " ");
        return normalizeSpace(title).replaceAll(" ", "-");
    }

    private String[] getSupportedEntities() {
        return configurationService.getArrayProperty("cris.custom-url.consumer.supported-entities", new String[] {});
    }

    @Override
    public void end(Context ctx) throws Exception {
        itemsAlreadyProcessed.clear();
    }

}
