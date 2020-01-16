package org.ssu.service.localization;

import org.dspace.app.util.DCInputsReaderException;
import org.springframework.stereotype.Service;
import org.ssu.entity.response.ItemTypeResponse;
import org.ssu.service.statistics.EssuirStatistics;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TypeLocalization {
    @Resource
    private EssuirStatistics essuirStatistics;

    private Map<Locale, LocalizedTypeStorage> types = new HashMap<>();

    class LocalizedTypeStorage{
        private Map<String, String> typesTable = new HashMap<>();

        public LocalizedTypeStorage(Map<String, String> typesTable) {
            this.typesTable = typesTable;
        }

        public String getTypeLocalized(String type) {
            return typesTable.getOrDefault(type, type);
        }
    }

    @PostConstruct
    public void init() {
        types.put(Locale.ENGLISH, updateTypeLocalizationTable(Locale.ENGLISH));
        types.put(Locale.forLanguageTag("uk"), updateTypeLocalizationTable(Locale.forLanguageTag("uk")));
        types.put(Locale.forLanguageTag("ru"), updateTypeLocalizationTable(Locale.forLanguageTag("ru")));
    }

    private LocalizedTypeStorage updateTypeLocalizationTable(Locale locale) {
        Map<String, String> typesTable = new HashMap<>();
        List<String> typesList = null;
        try {
            typesList = new LocalizedInputsReader().getInputsReader(locale.getLanguage()).getPairs("common_types");
        } catch (DCInputsReaderException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < typesList.size(); i += 2)
            typesTable.put(typesList.get(i + 1), typesList.get(i));
        return new LocalizedTypeStorage(typesTable);
    }

    public String getTypeLocalized(String type, Locale locale) {
        return types.get(locale).getTypeLocalized(type);
    }

    public List<ItemTypeResponse> getSubmissionStatisticsByType(Locale locale) {
        return essuirStatistics.getStatisticsByType().entrySet()
                .stream()
                .map(item -> new ItemTypeResponse.Builder().withTitle(getTypeLocalized(item.getKey(), locale)).withCount(item.getValue()).withSearchQuery(item.getKey()).build())
                .collect(Collectors.toList());

    }
}
