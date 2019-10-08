package org.ssu.service;

import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.springframework.stereotype.Service;
import org.ssu.entity.AuthorLocalization;
import org.ssu.service.localization.AuthorsCache;
import org.ssu.service.localization.TypeLocalization;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Resource
    private AuthorsCache authorsCache;

    @Resource
    private TypeLocalization typeLocalization;

    transient private final org.dspace.content.service.ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public Integer extractIssuedYearForItem(Item item) {
        List<MetadataValue> dateIssuedMetadata = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);

        return dateIssuedMetadata.stream()
                .findFirst()
                .map(MetadataValue::getValue)
                .map(DCDate::new)
                .map(DCDate::getYear)
                .orElse(null);
    }

    public String getCitationForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "identifier", "citation", Item.ANY)
                .stream()
                .map(MetadataValue::getValue)
                .findFirst()
                .orElse("");
    }

    public String getPublisherForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "publisher", null, Item.ANY)
                .stream()
                .map(MetadataValue::getValue)
                .findFirst()
                .orElse("");
    }

    public String getURIForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "identifier", "uri", Item.ANY)
                .stream()
                .map(MetadataValue::getValue)
                .findFirst()
                .orElse("");
    }

    public String getAlternativeTitleForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "title", "alternative", Item.ANY)
                .stream()
                .map(MetadataValue::getValue)
                .findFirst()
                .orElse("");
    }

    public List<AuthorLocalization> extractAuthorListForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "contributor", "*", Item.ANY)
                .stream()
                .map(MetadataValue::getValue)
                .map(author -> authorsCache.getAuthorLocalization(author))
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getKeywordsForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "subject", null, Item.ANY)
                .stream()
                .sorted(Comparator.comparingInt(MetadataValue::getPlace))
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
    }

    public List<String> getAbstractsForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "description", "abstract", Item.ANY)
                .stream()
                .sorted(Comparator.comparingInt(MetadataValue::getPlace))
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
    }

    public String getItemTypeLocalized(Item item, Locale locale) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "type", "*", Item.ANY)
                .stream()
                .findFirst()
                .map(MetadataValue::getValue)
                .map(type -> typeLocalization.getTypeLocalized(type, locale))
                .get();
    }

}
