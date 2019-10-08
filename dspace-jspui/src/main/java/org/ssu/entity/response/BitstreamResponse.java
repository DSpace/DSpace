package org.ssu.entity.response;

public class BitstreamResponse {
    private String filename;
    private String handle;
    private String size;
    private Integer downloadCount;
    private String link;
    private String format;

    private BitstreamResponse(Builder builder) {
        filename = builder.filename;
        handle = builder.handle;
        size = builder.size;
        downloadCount = builder.downloadCount;
        link = builder.link;
        format = builder.format;
    }

    public String getFilename() {
        return filename;
    }

    public String getHandle() {
        return handle;
    }

    public String getSize() {
        return size;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public String getLink() {
        return link;
    }

    public String getFormat() {
        return format;
    }


    public static final class Builder {
        private String filename;
        private String handle;
        private String size;
        private Integer downloadCount;
        private String link;
        private String format;

        public Builder() {
        }

        public Builder(BitstreamResponse copy) {
            this.filename = copy.getFilename();
            this.handle = copy.getHandle();
            this.size = copy.getSize();
            this.downloadCount = copy.getDownloadCount();
            this.link = copy.getLink();
            this.format = copy.getFormat();
        }

        public Builder withFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder withHandle(String handle) {
            this.handle = handle;
            return this;
        }

        public Builder withSize(String size) {
            this.size = size;
            return this;
        }

        public Builder withDownloadCount(Integer downloadCount) {
            this.downloadCount = downloadCount;
            return this;
        }

        public Builder withLink(String link) {
            this.link = link;
            return this;
        }

        public Builder withFormat(String format) {
            this.format = format;
            return this;
        }

        public BitstreamResponse build() {
            return new BitstreamResponse(this);
        }
    }
}
