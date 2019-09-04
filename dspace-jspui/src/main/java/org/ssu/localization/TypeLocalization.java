package org.ssu.localization;

import org.dspace.app.util.DCInputsReaderException;
import org.springframework.stereotype.Service;
import org.ssu.entity.response.ItemTypeResponse;
import org.ssu.statistics.EssuirStatistics;

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
    private Map<String, String> typesTable = new HashMap<>();

    private void updateTypeLocalizationTable(Locale locale) {
        typesTable.clear();
        List<String> typesList = null;
        try {
            typesList = new LocalizedInputsReader().getInputsReader(locale.getLanguage()).getPairs("common_types");
        } catch (DCInputsReaderException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < typesList.size(); i += 2)
            typesTable.put(typesList.get(i + 1), typesList.get(i));
    }

    public String getTypeLocalized(String type, Locale locale) {
        updateTypeLocalizationTable(locale);
        return typesTable.getOrDefault(type, type);
    }

    public List<ItemTypeResponse> getSubmissionStatisticsByType(Locale locale) {

        return essuirStatistics.getStatisticsByType().entrySet()
                .stream()
                .map(item -> new ItemTypeResponse.Builder().withTitle(getTypeLocalized(item.getKey(), locale)).withCount(item.getValue()).withSearchQuery(item.getKey()).build())
                .collect(Collectors.toList());

    }
}
