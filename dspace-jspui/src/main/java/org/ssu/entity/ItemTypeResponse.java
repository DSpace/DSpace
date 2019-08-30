package org.ssu.entity;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ItemTypeResponse implements Serializable {
    @JsonProperty("count")
    private Integer count;
    @JsonProperty("title")
    private String title;
    @JsonProperty("search_query")
    private String searchQuery;

    private ItemTypeResponse(Builder builder) {
        count = builder.count;
        title = builder.title;
        searchQuery = builder.searchQuery;
    }

    public Integer getCount() {
        return count;
    }

    public String getTitle() {
        return title;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public static final class Builder {
        private Integer count;
        private String title;
        private String searchQuery;

        public Builder() {
        }

        public Builder(ItemTypeResponse copy) {
            this.count = copy.getCount();
            this.title = copy.getTitle();
            this.searchQuery = copy.getSearchQuery();
        }

        public Builder withCount(Integer count) {
            this.count = count;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withSearchQuery(String searchQuery) {
            this.searchQuery = searchQuery;
            return this;
        }

        public ItemTypeResponse build() {
            return new ItemTypeResponse(this);
        }
    }
}
