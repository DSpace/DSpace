package org.ssu.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ItemDepositorResponse {
    @JsonProperty("name")
    private String name;
    @JsonProperty("count")
    private Integer count;
    @JsonProperty("data")
    private List<ItemDepositorResponse> depositors;

    private ItemDepositorResponse(Builder builder) {
        name = builder.name;
        count = builder.count;
        depositors = builder.depositors;
    }

    public String getName() {
        return name;
    }

    public Integer getCount() {
        return count;
    }

    public List<ItemDepositorResponse> getDepositors() {
        return depositors;
    }


    public static final class Builder {
        private String name;
        private Integer count;
        private List<ItemDepositorResponse> depositors;

        public Builder() {
        }

        public Builder(ItemDepositorResponse copy) {
            this.name = copy.getName();
            this.count = copy.getCount();
            this.depositors = copy.getDepositors();
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withCount(Integer count) {
            this.count = count;
            return this;
        }

        public Builder withDepositors(List<ItemDepositorResponse> depositors) {
            this.depositors = depositors;
            return this;
        }

        public ItemDepositorResponse build() {
            return new ItemDepositorResponse(this);
        }
    }
}
