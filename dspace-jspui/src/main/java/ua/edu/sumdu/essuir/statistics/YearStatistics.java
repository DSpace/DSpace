package ua.edu.sumdu.essuir.statistics;

import java.util.ArrayList;

public class YearStatistics {

    private Integer year;
    private Integer totalYearViews;
    private Integer totalYearDownloads;
    private ArrayList<Integer> yearViews;
    private ArrayList<Integer> yearDownloads;
    private Integer currentMonth;

    public YearStatistics(){
        this.currentMonth = -1;
    }

    public YearStatistics(int year, int totalYearViews, int totalYearDownloads, ArrayList<Integer> yearViews, ArrayList<Integer> yearDownloads, Integer currentMonth) {
        this.year = year;
        this.totalYearViews = totalYearViews;
        this.totalYearDownloads = totalYearDownloads;
        this.yearViews = yearViews;
        this.yearDownloads = yearDownloads;
        this.currentMonth = currentMonth;
    }

    public Integer getCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(Integer currentMonth) {
        this.currentMonth = currentMonth;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getTotalYearViews() {
        return totalYearViews;
    }

    public void setTotalYearViews(Integer totalYearViews) {
        this.totalYearViews = totalYearViews;
    }

    public Integer getTotalYearDownloads() {
        return totalYearDownloads;
    }

    public void setTotalYearDownloads(Integer totalYearDownloads) {
        this.totalYearDownloads = totalYearDownloads;
    }

    public ArrayList<Integer> getYearViews() {
        return yearViews;
    }

    public void setYearViews(ArrayList<Integer> yearViews) {
        this.yearViews = yearViews;
    }

    public ArrayList<Integer> getYearDownloads() {
        return yearDownloads;
    }

    public void setYearDownloads(ArrayList<Integer> yearDownloads) {
        this.yearDownloads = yearDownloads;
    }
}
