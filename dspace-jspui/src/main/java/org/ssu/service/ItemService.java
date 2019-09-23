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

    public List<AuthorLocalization> extractAuthorListForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "contributor", "*", Item.ANY)
                .stream()
                .map(MetadataValue::getValue)
                .map(author -> authorsCache.getAuthorLocalization(author))
                .distinct()
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
