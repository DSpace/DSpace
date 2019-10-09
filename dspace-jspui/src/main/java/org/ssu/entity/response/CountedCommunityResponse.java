package org.ssu.entity.response;

public class CountedCommunityResponse {
    private String title;
    private String handle;
    private Integer itemCount;

    private CountedCommunityResponse(Builder builder) {
        title = builder.title;
        handle = builder.handle;
        itemCount = builder.itemCount;
    }

    public String getTitle() {
        return title;
    }

    public String getHandle() {
        return handle;
    }

    public Integer getItemCount() {
        return itemCount;
    }


    public static final class Builder {
        private String title;
        private String handle;
        private Integer itemCount;

        public Builder() {
        }

        public Builder(CountedCommunityResponse copy) {
            this.title = copy.getTitle();
            this.handle = copy.getHandle();
            this.itemCount = copy.getItemCount();
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withHandle(String handle) {
            this.handle = handle;
            return this;
        }

        public Builder withItemCount(Integer itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public CountedCommunityResponse build() {
            return new CountedCommunityResponse(this);
        }
    }
}
