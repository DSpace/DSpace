package ua.edu.sumdu.essuir.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@IdClass(GeneralStatisticsId.class)
@Table(name = "general_statistics")
public class GeneralStatistics implements Serializable {

    @Id
    @Column(name = "year")
    private Integer year;

    @Id
    @Column(name = "month")
    private Integer month;

    @Column(name = "count_views")
    private Integer viewsCount;

    @Column(name = "count_downloads")
    private Integer downloadsCount;

    public GeneralStatistics(){}

    public GeneralStatistics(Integer year, Integer month,Integer viewsCount, Integer downloadsCount) {
        this.year = year;
        this.month = month;
        this.viewsCount = viewsCount;
        this.downloadsCount = downloadsCount;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Integer getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Integer viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Integer getDownloadsCount() {
        return downloadsCount;
    }

    public void setDownloadsCount(Integer downloadsCount) {
        this.downloadsCount = downloadsCount;
    }
}
