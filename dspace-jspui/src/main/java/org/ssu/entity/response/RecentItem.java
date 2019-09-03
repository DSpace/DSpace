package org.ssu.entity.response;

import org.dspace.content.Item;

public class RecentItem {
    private String title;
    private String type;
    private String handle;

    private RecentItem(Builder builder) {
        title = builder.title;
        type = builder.type;
        handle = builder.handle;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getHandle() {
        return handle;
    }


    public static final class Builder {
        private String title;
        private String type;
        private String handle;

        public Builder() {
        }

        public Builder(RecentItem copy) {
            this.title = copy.getTitle();
            this.type = copy.getType();
            this.handle = copy.getHandle();
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withHandle(String handle) {
            this.handle = handle;
            return this;
        }

        public RecentItem build() {
            return new RecentItem(this);
        }
    }
}
