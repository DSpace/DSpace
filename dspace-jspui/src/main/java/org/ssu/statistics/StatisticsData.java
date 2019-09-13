package org.ssu.statistics;

public class StatisticsData {
    private Integer totalCount = 0;
    private String lastUpdate = "";
    private Integer totalViews = 0;
    private Integer totalDownloads = 0;

    private StatisticsData(Builder builder) {
        totalCount = builder.totalCount;
        lastUpdate = builder.lastUpdate;
        totalViews = builder.totalViews;
        totalDownloads = builder.totalDownloads;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public Integer getTotalViews() {
        return totalViews;
    }

    public Integer getTotalDownloads() {
        return totalDownloads;
    }


    public static final class Builder {
        private Integer totalCount;
        private String lastUpdate;
        private Integer totalViews;
        private Integer totalDownloads;

        public Builder() {
        }

        public Builder(StatisticsData copy) {
            this.totalCount = copy.getTotalCount();
            this.lastUpdate = copy.getLastUpdate();
            this.totalViews = copy.getTotalViews();
            this.totalDownloads = copy.getTotalDownloads();
        }

        public Builder withTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder withLastUpdate(String lastUpdate) {
            this.lastUpdate = lastUpdate;
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

        public StatisticsData build() {
            return new StatisticsData(this);
        }
    }
}
