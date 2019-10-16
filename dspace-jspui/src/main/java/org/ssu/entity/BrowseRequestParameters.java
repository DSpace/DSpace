package org.ssu.entity;

import java.util.Optional;

public class BrowseRequestParameters {
    private Integer sortBy;
    private String sortOrder;
    private Optional<String> startsWith;
    private Integer page;
    private Integer itemsPerPage;
    private Boolean isExtendedTable;

    private BrowseRequestParameters(Builder builder) {
        sortBy = builder.sortBy;
        sortOrder = builder.sortOrder;
        startsWith = builder.startsWith;
        page = builder.page;
        itemsPerPage = builder.itemsPerPage;
        isExtendedTable = builder.isExtendedTable;
    }


    public Integer getSortBy() {
        return sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public Optional<String> getStartsWith() {
        return startsWith;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    public Boolean getExtendedTable() {
        return isExtendedTable;
    }


    public static final class Builder {
        private Integer sortBy;
        private String sortOrder;
        private Optional<String> startsWith;
        private Integer page;
        private Integer itemsPerPage;
        private Boolean isExtendedTable;

        public Builder() {
        }

        public Builder(BrowseRequestParameters copy) {
            this.sortBy = copy.getSortBy();
            this.sortOrder = copy.getSortOrder();
            this.startsWith = copy.getStartsWith();
            this.page = copy.getPage();
            this.itemsPerPage = copy.getItemsPerPage();
            this.isExtendedTable = copy.getExtendedTable();
        }

        public Builder withSortBy(Integer sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder withSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder withStartsWith(Optional<String> startsWith) {
            this.startsWith = startsWith;
            return this;
        }

        public Builder withPage(Integer page) {
            this.page = page;
            return this;
        }

        public Builder withItemsPerPage(Integer itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
            return this;
        }

        public Builder withIsExtendedTable(Boolean isExtendedTable) {
            this.isExtendedTable = isExtendedTable;
            return this;
        }

        public BrowseRequestParameters build() {
            return new BrowseRequestParameters(this);
        }
    }
}
