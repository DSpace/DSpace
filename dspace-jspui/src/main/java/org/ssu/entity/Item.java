package org.ssu.entity;

import java.util.UUID;

public class Item {
    private UUID itemId;
    private String name;
    private Long viewCount;
    private Long downloadCount;
    private String link;

    private Item(Builder builder) {
        itemId = builder.itemId;
        name = builder.name;
        viewCount = builder.viewCount;
        downloadCount = builder.downloadCount;
        link = builder.link;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public String getLink() {
        return link;
    }


    public static final class Builder {
        private UUID itemId;
        private String name;
        private Long viewCount;
        private Long downloadCount;
        private String link;

        public Builder() {
        }

        public Builder(Item copy) {
            this.itemId = copy.getItemId();
            this.name = copy.getName();
            this.viewCount = copy.getViewCount();
            this.downloadCount = copy.getDownloadCount();
            this.link = copy.getLink();
        }

        public Builder withItemId(UUID itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withViewCount(Long viewCount) {
            this.viewCount = viewCount;
            return this;
        }

        public Builder withDownloadCount(Long downloadCount) {
            this.downloadCount = downloadCount;
            return this;
        }

        public Builder withLink(String link) {
            this.link = link;
            return this;
        }

        public Item build() {
            return new Item(this);
        }
    }
}
