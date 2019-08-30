package org.ssu.statistics;

public class StatisticsData {
    private long totalCount = 0;
    private String lastUpdate = "";
    private long totalViews = 0;
    private long totalDownloads = 0;

    private StatisticsData(Builder builder) {
        totalCount = builder.totalCount;
        lastUpdate = builder.lastUpdate;
        totalViews = builder.totalViews;
        totalDownloads = builder.totalDownloads;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public long getTotalDownloads() {
        return totalDownloads;
    }


    public static final class Builder {
        private long totalCount;
        private String lastUpdate;
        private long totalViews;
        private long totalDownloads;

        public Builder() {
        }

        public Builder(StatisticsData copy) {
            this.totalCount = copy.getTotalCount();
            this.lastUpdate = copy.getLastUpdate();
            this.totalViews = copy.getTotalViews();
            this.totalDownloads = copy.getTotalDownloads();
        }

        public Builder withTotalCount(long totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder withLastUpdate(String lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }

        public Builder withTotalViews(long totalViews) {
            this.totalViews = totalViews;
            return this;
        }

        public Builder withTotalDownloads(long totalDownloads) {
            this.totalDownloads = totalDownloads;
            return this;
        }

        public StatisticsData build() {
            return new StatisticsData(this);
        }
    }
}
