package ua.edu.sumdu.essuir.entity;

import java.io.Serializable;

class GeneralStatisticsId implements Serializable {
    Integer year;
    Integer month;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }


}
