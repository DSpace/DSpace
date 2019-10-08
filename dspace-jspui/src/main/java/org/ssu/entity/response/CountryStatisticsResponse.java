package org.ssu.entity.response;

public class CountryStatisticsResponse {
    private String countryCode;
    private String countryName;
    private Integer count;

    private CountryStatisticsResponse(Builder builder) {
        countryCode = builder.countryCode;
        countryName = builder.countryName;
        count = builder.count;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public Integer getCount() {
        return count;
    }

    public static final class Builder {
        private String countryCode;
        private String countryName;
        private Integer count;

        public Builder() {
        }

        public Builder(CountryStatisticsResponse copy) {
            this.countryCode = copy.getCountryCode();
            this.countryName = copy.getCountryName();
            this.count = copy.getCount();
        }

        public Builder withCountryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder withCountryName(String countryName) {
            this.countryName = countryName;
            return this;
        }

        public Builder withCount(Integer count) {
            this.count = count;
            return this;
        }

        public CountryStatisticsResponse build() {
            return new CountryStatisticsResponse(this);
        }
    }
}
