package ua.edu.sumdu.essuir.service;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import ua.edu.sumdu.essuir.statistics.EssuirStatistics;
import ua.edu.sumdu.essuir.statistics.StatisticData;
import ua.edu.sumdu.essuir.statistics.YearStatistics;
import ua.edu.sumdu.essuir.entity.GeneralStatistics;
import ua.edu.sumdu.essuir.repository.GeneralStatisticsRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ivan on 13.05.2015.
 */
public class ScheduledTasks {

    private static Logger log = Logger.getLogger(ScheduledTasks.class);

    @Autowired
    private GeneralStatisticsService generalStatisticsService;

    @Autowired
    private GeneralStatisticsRepository generalStatisticsRepository;

    private Boolean isPreviousMonthStatistics(DateTime now, GeneralStatistics monthStatistics) {
        int previousMonth = now.getMonthOfYear() - 1;
        int previousYear = now.getYear();
        if (now.getMonthOfYear() == 1) {
            previousMonth = 12;
            previousYear = now.getYear() - 1;
        }

        return (monthStatistics.getMonth().equals(previousMonth) && monthStatistics.getYear().equals(previousYear));
    }

    private Boolean isPreviousMonthStatisticsSavedToDatabase() {
        DateTime today = DateTime.now();
        List<GeneralStatistics> statistics = generalStatisticsRepository.findAll();
        for (GeneralStatistics monthStatistics : statistics) {
            if(isPreviousMonthStatistics(today, monthStatistics)) {
                return true;
            }
        }
        return false;
    }

    public Boolean finalizeMonthStatistics() {
        if (!isPreviousMonthStatisticsSavedToDatabase()) {
            addNewEntityByMonth();
            return true;
        }
        return false;
    }

    // Fire at 00:00 on the first day of every month
    @Scheduled(cron = "0 0 0 1 * ?")
    public void addNewEntityByMonth(){
        DateTime dateTime = DateTime.now();
        //System.out.println("start schedule");
        //System.out.println(dateTime.toString());
        StatisticData sd;
        try {
            Context context = new Context();
            sd = EssuirStatistics.getTotalStatistic(context);
            if(dateTime.getMonthOfYear() != 1){
                GeneralStatistics newMonth = new GeneralStatistics(dateTime.getYear(),
                        dateTime.getMonthOfYear() - 1,
                        generalStatisticsService.getCurrentMonthStatisticsViews(sd.getTotalViews()),
                        generalStatisticsService.getCurrentMonthStatisticsDownloads(sd.getTotalDownloads()));
                generalStatisticsRepository.save(newMonth);
                //System.out.println("saved new month");
            }
            else {
                GeneralStatistics newMonth = new GeneralStatistics(dateTime.getYear() - 1,
                        11,
                        generalStatisticsService.getCurrentMonthStatisticsViews(sd.getTotalViews()),
                        generalStatisticsService.getCurrentMonthStatisticsDownloads(sd.getTotalDownloads()));
                generalStatisticsRepository.save(newMonth);
                //System.out.println("saved new month");
                generalStatisticsService.updateListYearsStatistics();
                YearStatistics currentYear = generalStatisticsService.getListYearsStatistics().get(0);
                Integer currentYearStatiscticsViews = 0;
                Integer currentYearStatiscticsDownloads = 0;
                ArrayList<Integer> tmpViews = currentYear.getYearViews();
                ArrayList<Integer> tmpDownloads = currentYear.getYearDownloads();
                for (int i = 0; i < tmpViews.size(); i++) {
                    currentYearStatiscticsViews += tmpViews.get(i);
                    currentYearStatiscticsDownloads += tmpDownloads.get(i);
                }

                GeneralStatistics currentYearResult = generalStatisticsRepository.findCurrentYearTotalStatistics(dateTime.getYear() - 1, -1);
                currentYearResult.setViewsCount(currentYearStatiscticsViews);
                currentYearResult.setDownloadsCount(currentYearStatiscticsDownloads);
                generalStatisticsRepository.save(currentYearResult);
                GeneralStatistics newYearResult = new GeneralStatistics(dateTime.getYear(), -1, 0, 0);
                generalStatisticsRepository.save(newYearResult);
            }
            generalStatisticsService.updateListYearsStatistics();
            context.complete();
            //System.out.println("That's OK");
        } catch (SQLException e){
            log.error(e.getMessage(), e);
            e.printStackTrace();
        }

    }
}
