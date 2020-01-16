package org.ssu.service.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ssu.entity.statistics.GeneralStatistics;
import org.ssu.entity.statistics.StatisticsData;
import org.ssu.repository.GeneralStatisticsRepository;
import org.ssu.service.GeneralStatisticsService;

import javax.annotation.Resource;
import java.time.LocalDate;

@Service
public class ScheduledTasks {
    @Resource
    private EssuirStatistics essuirStatistics;

    @Autowired
    private GeneralStatisticsService generalStatisticsService;

    @Autowired
    private GeneralStatisticsRepository generalStatisticsRepository;

    @Scheduled(cron = "0 0 0/3 1-2 * ? *")
    public void finalizeMonthStatistics() {
        LocalDate previousMonth = LocalDate.now().minusDays(25);
        StatisticsData statisticsData = essuirStatistics.getTotalStatistic(null);
        GeneralStatistics generalStatistics = new GeneralStatistics.Builder()
                .withMonth(previousMonth.getMonthValue() - 1)
                .withYear(previousMonth.getYear())
                .withViewsCount(generalStatisticsService.getCurrentMonthStatisticsViews(statisticsData))
                .withDownloadsCount(generalStatisticsService.getCurrentMonthStatisticsDownloads(statisticsData))
                .build();
        generalStatisticsRepository.save(generalStatistics);
    }
}