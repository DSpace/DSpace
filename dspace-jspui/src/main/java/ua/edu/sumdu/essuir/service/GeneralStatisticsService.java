package ua.edu.sumdu.essuir.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.edu.sumdu.essuir.statistics.YearStatistics;
import ua.edu.sumdu.essuir.entity.GeneralStatistics;
import ua.edu.sumdu.essuir.repository.GeneralStatisticsRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeneralStatisticsService {

    private List<YearStatistics> cacheListYearsStatistics;

    @Autowired
    private GeneralStatisticsRepository generalStatisticsRepository;

    public  List<YearStatistics> getListYearsStatistics(){
        if(cacheListYearsStatistics == null) {
            updateListYearsStatistics();
        }
        return cacheListYearsStatistics;
    }

    public void updateListYearsStatistics(){
        List<YearStatistics> listYearsStatistics = new ArrayList<YearStatistics>();
        List<GeneralStatistics> tmpYearsStatistics = (ArrayList<GeneralStatistics>) generalStatisticsRepository.findAllYearsStatistics();
        for (GeneralStatistics entity : tmpYearsStatistics) {
            YearStatistics yearStatistics = new YearStatistics();
            yearStatistics.setYear(entity.getYear());
            yearStatistics.setTotalYearViews(entity.getViewsCount());
            yearStatistics.setTotalYearDownloads(entity.getDownloadsCount());
            ArrayList<Integer> tmpYearViews = (ArrayList<Integer>) generalStatisticsRepository.findAllMonthsViewsStatisticsByYear(entity.getYear());
            ArrayList<Integer> tmpYearDownloads = (ArrayList<Integer>) generalStatisticsRepository.findAllMonthsDownloadsStatisticsByYear(entity.getYear());
            if(tmpYearViews.size() < 12){
                yearStatistics.setCurrentMonth(tmpYearViews.size());
                for (int i = tmpYearViews.size(); i < 12; i++) {
                    tmpYearViews.add(0);
                    tmpYearDownloads.add(0);
                }
            }
            yearStatistics.setYearViews(tmpYearViews);
            yearStatistics.setYearDownloads(tmpYearDownloads);
            listYearsStatistics.add(yearStatistics);
        }
        cacheListYearsStatistics = listYearsStatistics;
    }


    public Integer getCurrentMonthStatisticsViews(long totalViews){
        Integer res = getCurrentYearStatisticsViews(totalViews);
        ArrayList<Integer> currentYearStatisticsViews = getListYearsStatistics().get(0).getYearViews();
        for (int i = 0; i < currentYearStatisticsViews.size(); i++) {
            res -= currentYearStatisticsViews.get(i);
        }
        return res;
    }

    public Integer getCurrentMonthStatisticsDownloads(long totalDownloads){
        Integer res = getCurrentYearStatisticsDownloads(totalDownloads);
        ArrayList<Integer> currentYearStatisticsDownloads = getListYearsStatistics().get(0).getYearDownloads();
        for (int i = 0; i < currentYearStatisticsDownloads.size(); i++) {
            res -= currentYearStatisticsDownloads.get(i);
        }
        return res;
    }

    public Integer getCurrentYearStatisticsViews(long totalViews){
        Integer res = Long.valueOf(totalViews).intValue();
        for (int i = 0; i < getListYearsStatistics().size(); i++) {
            res -= getListYearsStatistics().get(i).getTotalYearViews();
        }
        return res;
    }

    public Integer getCurrentYearStatisticsDownloads(long totalDownloads){
        Integer res = Long.valueOf(totalDownloads).intValue();
        for (int i = 0; i < getListYearsStatistics().size(); i++) {
            res -= getListYearsStatistics().get(i).getTotalYearDownloads();
        }
        return res;
    }

}
