package org.ssu.service.localization;

import org.springframework.stereotype.Service;
import org.ssu.entity.response.ItemTypeResponse;
import org.ssu.service.statistics.EssuirStatistics;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TypeLocalization {
    private LocalizedPairsStorage storage;

    @Resource
    private EssuirStatistics essuirStatistics;

    @PostConstruct
    public void init() {
        storage = new LocalizedPairsStorage("common_types");
    }

    public String getTypeLocalized(String type, Locale locale) {
        return storage.getItem(type, locale);
    }

    public List<ItemTypeResponse> getSubmissionStatisticsByType(Locale locale) {
        return essuirStatistics.getStatisticsByType().entrySet()
                .stream()
                .map(item -> new ItemTypeResponse.Builder().withTitle(getTypeLocalized(item.getKey(), locale)).withCount(item.getValue()).withSearchQuery(item.getKey()).build())
                .collect(Collectors.toList());

    }
}
