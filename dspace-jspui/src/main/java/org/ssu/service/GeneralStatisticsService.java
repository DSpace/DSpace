package org.ssu.service;

import org.springframework.stereotype.Service;
import org.ssu.entity.statistics.GeneralStatistics;
import org.ssu.entity.statistics.YearStatistics;
import org.ssu.entity.response.GeneralStatisticsResponse;
import org.ssu.repository.GeneralStatisticsRepository;
import org.ssu.service.statistics.EssuirStatistics;
import org.ssu.entity.statistics.StatisticsData;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class GeneralStatisticsService {

    private List<YearStatistics> cacheListYearsStatistics = new ArrayList<>();

    @Resource
    private GeneralStatisticsRepository generalStatisticsRepository;

    @Resource
    private EssuirStatistics essuirStatistics;

    private Predicate<YearStatistics> isCurrentYear = (entry) -> entry.getYear().equals(LocalDate.now().getYear());

    @PostConstruct
    public void updateListYearsStatistics() {
        Map<Integer, List<GeneralStatistics>> collect = generalStatisticsRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(GeneralStatistics::getYear));

        Function<List<GeneralStatistics>, List<Integer>> getViewByMonth = (data) -> data.stream().sorted(Comparator.comparing(GeneralStatistics::getMonth)).filter(item -> item.getMonth() != -1).map(GeneralStatistics::getViewsCount).collect(Collectors.toList());
        Function<List<GeneralStatistics>, List<Integer>> getDownloadsByMonth = (data) -> data.stream().sorted(Comparator.comparing(GeneralStatistics::getMonth)).filter(item -> item.getMonth() != -1).map(GeneralStatistics::getDownloadsCount).collect(Collectors.toList());

        cacheListYearsStatistics = collect.entrySet()
                .stream()
                .map(item ->
                        new YearStatistics.Builder()
                                .withYearViews(getViewByMonth.apply(item.getValue()))
                                .withYearDownloads(getDownloadsByMonth.apply(item.getValue()))
                                .withTotalYearDownloads(getDownloadsByMonth.apply(item.getValue()).stream().mapToInt(Integer::valueOf).sum())
                                .withTotalYearViews(getViewByMonth.apply(item.getValue()).stream().mapToInt(Integer::valueOf).sum())
                                .withYear(item.getKey())
                                .withCurrentMonth(LocalDate.now().getMonthValue() - 1)
                                .build()
                )
                .sorted(Comparator.comparing(YearStatistics::getYear).reversed())
                .collect(Collectors.toList());
    }


    public GeneralStatisticsResponse collectGeneralStatistics() {
        StatisticsData statisticsData = essuirStatistics.getTotalStatistic();
        return new GeneralStatisticsResponse.Builder()
                .withTotalCount(statisticsData.getTotalCount())
                .withTotalViews(statisticsData.getTotalViews())
                .withTotalDownloads(statisticsData.getTotalDownloads())
                .withCurrentMonthStatisticsViews(getCurrentMonthStatisticsViews(statisticsData))
                .withCurrentMonthStatisticsDownloads(getCurrentMonthStatisticsDownloads(statisticsData))
                .withCurrentYearStatisticsDownloads(getCurrentYearStatisticsDownloads(statisticsData))
                .withCurrentYearStatisticsViews(getCurrentYearStatisticsViews(statisticsData))
                .build();
    }

    public List<YearStatistics> getListYearsStatistics() {
        return cacheListYearsStatistics;
    }

    public Integer getCurrentMonthStatisticsViews(StatisticsData statisticsData) {
        return statisticsData.getTotalViews()
                - getCumulativeStatisticsByMonthForYear(YearStatistics::getYearViews, isCurrentYear)
                - getCumulativeStatisticsByMonthForYear(YearStatistics::getYearViews, isCurrentYear.negate());
    }

    public Integer getCurrentMonthStatisticsDownloads(StatisticsData statisticsData) {
        return statisticsData.getTotalDownloads()
                - getCumulativeStatisticsByMonthForYear(YearStatistics::getYearDownloads, isCurrentYear)
                - getCumulativeStatisticsByMonthForYear(YearStatistics::getYearDownloads, isCurrentYear.negate());
    }

    private Integer getCurrentYearStatisticsViews(StatisticsData statisticsData) {
        return statisticsData.getTotalViews()
                - getCumulativeStatisticsByMonthForYear(YearStatistics::getYearViews, isCurrentYear.negate());
    }

    private Integer getCurrentYearStatisticsDownloads(StatisticsData statisticsData) {
        return statisticsData.getTotalDownloads()
                - getCumulativeStatisticsByMonthForYear(YearStatistics::getYearDownloads, isCurrentYear.negate());
    }

    private Integer getCumulativeStatisticsByMonthForYear(Function<YearStatistics, List<Integer>> dataTransform, Predicate<YearStatistics> isCurrentYear) {
        return cacheListYearsStatistics
                .stream()
                .filter(isCurrentYear)
                .flatMapToInt(item -> dataTransform.apply(item).stream().mapToInt(t -> t))
                .sum();
    }
}