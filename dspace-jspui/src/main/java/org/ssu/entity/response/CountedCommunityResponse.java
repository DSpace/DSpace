package org.ssu.entity.response;

import java.util.UUID;

public class CountedCommunityResponse {
    private UUID id;
    private String title;
    private String handle;
    private Integer itemCount;

    private CountedCommunityResponse(Builder builder) {
        id = builder.id;
        title = builder.title;
        handle = builder.handle;
        itemCount = builder.itemCount;
    }

    public UUID getId() {
        return id;
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
        private UUID id;
        private String title;
        private String handle;
        private Integer itemCount;

        public Builder() {
        }

        public Builder(CountedCommunityResponse copy) {
            this.id = copy.getId();
            this.title = copy.getTitle();
            this.handle = copy.getHandle();
            this.itemCount = copy.getItemCount();
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
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
