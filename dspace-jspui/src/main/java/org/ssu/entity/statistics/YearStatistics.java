package org.ssu.entity.statistics;

import java.util.List;

public class YearStatistics {

    private Integer year;
    private Integer totalYearViews;
    private Integer totalYearDownloads;
    private List<Integer> yearViews;
    private List<Integer> yearDownloads;

    private YearStatistics(Builder builder) {
        year = builder.year;
        totalYearViews = builder.totalYearViews;
        totalYearDownloads = builder.totalYearDownloads;
        yearViews = builder.yearViews;
        yearDownloads = builder.yearDownloads;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getTotalYearViews() {
        return totalYearViews;
    }

    public Integer getTotalYearDownloads() {
        return totalYearDownloads;
    }

    public List<Integer> getYearViews() {
        return yearViews;
    }

    public List<Integer> getYearDownloads() {
        return yearDownloads;
    }

    public static final class Builder {
        private Integer year;
        private Integer totalYearViews;
        private Integer totalYearDownloads;
        private List<Integer> yearViews;
        private List<Integer> yearDownloads;

        public Builder() {
        }

        public Builder(YearStatistics copy) {
            this.year = copy.getYear();
            this.totalYearViews = copy.getTotalYearViews();
            this.totalYearDownloads = copy.getTotalYearDownloads();
            this.yearViews = copy.getYearViews();
            this.yearDownloads = copy.getYearDownloads();
        }

        public Builder withYear(Integer year) {
            this.year = year;
            return this;
        }

        public Builder withTotalYearViews(Integer totalYearViews) {
            this.totalYearViews = totalYearViews;
            return this;
        }

        public Builder withTotalYearDownloads(Integer totalYearDownloads) {
            this.totalYearDownloads = totalYearDownloads;
            return this;
        }

        public Builder withYearViews(List<Integer> yearViews) {
            this.yearViews = yearViews;
            while(this.yearViews.size() < 12)
                this.yearViews.add(0);
            return this;
        }

        public Builder withYearDownloads(List<Integer> yearDownloads) {
            this.yearDownloads = yearDownloads;
            while(this.yearDownloads.size() < 12)
                this.yearDownloads.add(0);
            return this;
        }

        public YearStatistics build() {
            return new YearStatistics(this);
        }
    }
}