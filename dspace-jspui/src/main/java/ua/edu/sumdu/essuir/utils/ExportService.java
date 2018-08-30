package ua.edu.sumdu.essuir.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseItem;
import ua.edu.sumdu.essuir.cache.AuthorCache;
import ua.edu.sumdu.essuir.entity.Publication;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExportService {
    public ExportService() {

    }

    public String publicationList(BrowseInfo browseInfo) {
        List<BrowseItem> items = Arrays.asList(browseInfo.getBrowseItemResults());
        return parsePublications(items);
    }

    private String parsePublications(List<BrowseItem> items) {
        try {
            return new ObjectMapper()
                    .writeValueAsString(items.stream()
                            .map(this::parseData)
                            .collect(Collectors.toList()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Publication parseData(BrowseItem item) {
        List<String> authors = Arrays.stream(item.getMetadata("dc", "contributor", "author", null)).map(it -> it.value).collect(Collectors.toList());
        List<String> localizedAuthors = AuthorCache.getLocalizedAuthors(authors, "uk");
        String itemType = Stream.of(item.getMetadata("dc", "type", null, null))
                .map(type -> EssuirUtils.getTypeLocalized(type.value, "uk"))
                .findFirst().orElse("");

        String citation = Stream.of(item.getMetadata("dc", "identifier", "citation", null))
                .findFirst()
                .map(citationText -> citationText.value)
                .orElse("");

        return new Publication.Builder()
                .withAuthors(localizedAuthors.stream().map(author -> author.replaceAll(",", "")).collect(Collectors.joining(";\r\n")))
                .withCitation(citation)
                .withTitle(item.getMetadata("dc", "title", null, null)[0].value)
                .withType(itemType)
                .build();
    }
}
