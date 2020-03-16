package org.ssu.service;

import org.springframework.stereotype.Service;
import org.ssu.entity.statistics.GeneralStatistics;
import org.ssu.entity.statistics.YearStatistics;
import org.ssu.repository.GeneralStatisticsRepository;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GeneralStatisticsService {
    @Resource
    private GeneralStatisticsRepository generalStatisticsRepository;

    public List<YearStatistics> getListYearsStatistics() {
        Map<Integer, List<GeneralStatistics>> collect = generalStatisticsRepository.findAll()
                .stream()
                .filter(item -> item.getMonth() != -1)
                .collect(Collectors.groupingBy(GeneralStatistics::getYear));

        Function<List<GeneralStatistics>, List<Integer>> getViewByMonth = (data) -> data.stream().sorted(Comparator.comparing(GeneralStatistics::getMonth)).filter(item -> item.getMonth() != -1).map(GeneralStatistics::getViewsCount).collect(Collectors.toList());
        Function<List<GeneralStatistics>, List<Integer>> getDownloadsByMonth = (data) -> data.stream().sorted(Comparator.comparing(GeneralStatistics::getMonth)).filter(item -> item.getMonth() != -1).map(GeneralStatistics::getDownloadsCount).collect(Collectors.toList());

        return collect.entrySet()
                .stream()
                .map(item ->
                        new YearStatistics.Builder()
                                .withYearViews(getViewByMonth.apply(item.getValue()))
                                .withYearDownloads(getDownloadsByMonth.apply(item.getValue()))
                                .withTotalYearDownloads(getDownloadsByMonth.apply(item.getValue()).stream().mapToInt(Integer::valueOf).sum())
                                .withTotalYearViews(getViewByMonth.apply(item.getValue()).stream().mapToInt(Integer::valueOf).sum())
                                .withYear(item.getKey())
                                .build()
                )
                .sorted(Comparator.comparing(YearStatistics::getYear).reversed())
                .collect(Collectors.toList());
    }
}