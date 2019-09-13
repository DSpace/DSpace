package org.ssu.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GeneralStatisticsResponse {
    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("total_views")
    private Integer totalViews;

    @JsonProperty("total_downloads")
    private Integer totalDownloads;

    @JsonProperty("current_month_statistics_views")
    private Integer currentMonthStatisticsViews;

    @JsonProperty("current_month_statistics_downloads")
    private Integer currentMonthStatisticsDownloads;

    @JsonProperty("current_year_statistics_views")
    private Integer currentYearStatisticsViews;

    @JsonProperty("current_year_statistics_downloads")
    private Integer currentYearStatisticsDownloads;

    private GeneralStatisticsResponse(Builder builder) {
        totalCount = builder.totalCount;
        totalViews = builder.totalViews;
        totalDownloads = builder.totalDownloads;
        currentMonthStatisticsViews = builder.currentMonthStatisticsViews;
        currentMonthStatisticsDownloads = builder.currentMonthStatisticsDownloads;
        currentYearStatisticsViews = builder.currentYearStatisticsViews;
        currentYearStatisticsDownloads = builder.currentYearStatisticsDownloads;
    }


    public Integer getTotalCount() {
        return totalCount;
    }

    public Integer getTotalViews() {
        return totalViews;
    }

    public Integer getTotalDownloads() {
        return totalDownloads;
    }

    public Integer getCurrentMonthStatisticsViews() {
        return currentMonthStatisticsViews;
    }

    public Integer getCurrentMonthStatisticsDownloads() {
        return currentMonthStatisticsDownloads;
    }

    public Integer getCurrentYearStatisticsViews() {
        return currentYearStatisticsViews;
    }

    public Integer getCurrentYearStatisticsDownloads() {
        return currentYearStatisticsDownloads;
    }


    public static final class Builder {
        private Integer totalCount;
        private Integer totalViews;
        private Integer totalDownloads;
        private Integer currentMonthStatisticsViews;
        private Integer currentMonthStatisticsDownloads;
        private Integer currentYearStatisticsViews;
        private Integer currentYearStatisticsDownloads;

        public Builder() {
        }

        public Builder(GeneralStatisticsResponse copy) {
            this.totalCount = copy.getTotalCount();
            this.totalViews = copy.getTotalViews();
            this.totalDownloads = copy.getTotalDownloads();
            this.currentMonthStatisticsViews = copy.getCurrentMonthStatisticsViews();
            this.currentMonthStatisticsDownloads = copy.getCurrentMonthStatisticsDownloads();
            this.currentYearStatisticsViews = copy.getCurrentYearStatisticsViews();
            this.currentYearStatisticsDownloads = copy.getCurrentYearStatisticsDownloads();
        }

        public Builder withTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder withTotalViews(Integer totalViews) {
            this.totalViews = totalViews;
            return this;
        }

        public Builder withTotalDownloads(Integer totalDownloads) {
            this.totalDownloads = totalDownloads;
            return this;
        }

        public Builder withCurrentMonthStatisticsViews(Integer currentMonthStatisticsViews) {
            this.currentMonthStatisticsViews = currentMonthStatisticsViews;
            return this;
        }

        public Builder withCurrentMonthStatisticsDownloads(Integer currentMonthStatisticsDownloads) {
            this.currentMonthStatisticsDownloads = currentMonthStatisticsDownloads;
            return this;
        }

        public Builder withCurrentYearStatisticsViews(Integer currentYearStatisticsViews) {
            this.currentYearStatisticsViews = currentYearStatisticsViews;
            return this;
        }

        public Builder withCurrentYearStatisticsDownloads(Integer currentYearStatisticsDownloads) {
            this.currentYearStatisticsDownloads = currentYearStatisticsDownloads;
            return this;
        }

        public GeneralStatisticsResponse build() {
            return new GeneralStatisticsResponse(this);
        }
    }
}
