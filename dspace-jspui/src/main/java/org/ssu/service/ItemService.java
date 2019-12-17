package org.ssu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.springframework.stereotype.Service;
import org.ssu.entity.AuthorLocalization;
import org.ssu.entity.response.ItemResponse;
import org.ssu.repository.MetadatavalueRepository;
import org.ssu.service.localization.TypeLocalization;
import org.ssu.service.statistics.EssuirStatistics;

import javax.annotation.Resource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ItemService {
    transient private final org.dspace.content.service.ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    transient private final MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    transient private final MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
    @Resource
    private AuthorsService authorsService;
    @Resource
    private EssuirStatistics essuirStatistics;
    @Resource
    private TypeLocalization typeLocalization;
    @Resource
    private MetadatavalueRepository metadatavalueRepository;

    public Iterator<Item> findAll(Context context) throws SQLException {
        return itemService.findAll(context);
    }

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

    public String getSpecialityForItem(Item item) {
        String speciality = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "speciality", "id", Item.ANY)
                .stream()
                .map(MetadataValue::getValue)
                .findFirst()
                .orElse("");

        try {
            ArrayList<JsonNode> jsonNodes = Lists.newArrayList(new ObjectMapper().readTree(speciality).elements());
            if(jsonNodes.size() < 3) return "";
            return jsonNodes.stream()
                    .map(it -> it.get("name").asText())
                    .collect(Collectors.joining("//"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
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
                .map(author -> authorsService.getAuthorLocalization(author))
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

    public Map<UUID, LocalDate> getAllDatesAvailable(Context context) throws SQLException, IOException {
        MetadataField dateAvailableField = metadataFieldService.findByElement(context, MetadataSchema.DC_SCHEMA, "date", "available");
        return metadatavalueRepository.selectMetadataByFieldId(dateAvailableField.getID())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        item -> LocalDate.parse(item.getValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
                        (a, b) -> a));
    }

    public LocalDate getDateAvailableForItem(Item item) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "date", "available", Item.ANY)
                .stream()
                .sorted(Comparator.comparingInt(MetadataValue::getPlace))
                .map(MetadataValue::getValue)
                .map(date -> LocalDate.parse(date.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .findFirst()
                .orElse(LocalDate.MIN);
    }

    public String getItemTypeLocalized(Item item, Locale locale) {
        return itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "type", "*", Item.ANY)
                .stream()
                .findFirst()
                .map(MetadataValue::getValue)
                .map(type -> typeLocalization.getTypeLocalized(type, locale))
                .orElse("");
    }

    public ItemResponse fetchItemresponseDataForItem(Item item, Locale locale) {
        Function<Item, String> extractAuthorListForItem = (currentItem) ->
                extractAuthorListForItem(currentItem)
                        .stream()
                        .map(author -> author.getFormattedAuthorData("%s, %s", locale))
                        .map(author -> String.format("<a href=\"/browse?type=author&value=%s\">%s</a>", author, author))
                        .collect(Collectors.joining("; "));

        return new ItemResponse.Builder()
                .withTitle(item.getName())
                .withYear(extractIssuedYearForItem(item))
                .withHandle(item.getHandle())
                .withAuthors(extractAuthorListForItem.apply(item))
                .withType(getItemTypeLocalized(item, locale))
                .withViews(essuirStatistics.getViewsForItem(item.getLegacyId()))
                .withDownloads(essuirStatistics.getDownloadsForItem(item.getLegacyId()))
                .withDateAvailable(getDateAvailableForItem(item))
                .withSubmitter(item.getSubmitter())
                .build();
    }

    public Map<UUID, String> fetchMastersAndBachelorsPapers() {
        return metadatavalueRepository.selectMetadataByFieldId(133);
    }

    public Map<UUID, String> fetchItemType() {
        return metadatavalueRepository.selectMetadataByFieldId(66);
    }
}
