package org.ssu.entity.response;

import org.dspace.eperson.EPerson;

import java.time.LocalDate;

public class ItemResponse {
    private String handle;
    private String title;
    private Integer views;
    private Integer downloads;
    private Integer year;
    private String authors;
    private String type;
    private LocalDate dateAvailable;
    private EPerson submitter;

    private ItemResponse(Builder builder) {
        handle = builder.handle;
        title = builder.title;
        views = builder.views;
        downloads = builder.downloads;
        year = builder.year;
        authors = builder.authors;
        type = builder.type;
        dateAvailable = builder.dateAvailable;
        submitter = builder.submitter;
    }

    public String getHandle() {
        return handle;
    }

    public String getTitle() {
        return title;
    }

    public Integer getViews() {
        return views;
    }

    public Integer getDownloads() {
        return downloads;
    }

    public Integer getYear() {
        return year;
    }

    public String getAuthors() {
        return authors;
    }

    public String getType() {
        return type;
    }

    public LocalDate getDateAvailable() {
        return dateAvailable;
    }

    public EPerson getSubmitter() {
        return submitter;
    }

    public static final class Builder {
        private String handle;
        private String title;
        private Integer views;
        private Integer downloads;
        private Integer year;
        private String authors;
        private String type;
        private LocalDate dateAvailable;
        private EPerson submitter;

        public Builder() {
        }

        public Builder(ItemResponse copy) {
            this.handle = copy.getHandle();
            this.title = copy.getTitle();
            this.views = copy.getViews();
            this.downloads = copy.getDownloads();
            this.year = copy.getYear();
            this.authors = copy.getAuthors();
            this.type = copy.getType();
            this.dateAvailable = copy.getDateAvailable();
            this.submitter = copy.getSubmitter();
        }

        public Builder withHandle(String handle) {
            this.handle = handle;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withViews(Integer views) {
            this.views = views;
            return this;
        }

        public Builder withDownloads(Integer downloads) {
            this.downloads = downloads;
            return this;
        }

        public Builder withYear(Integer year) {
            this.year = year;
            return this;
        }

        public Builder withAuthors(String authors) {
            this.authors = authors;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withDateAvailable(LocalDate dateAvailable) {
            this.dateAvailable = dateAvailable;
            return this;
        }

        public Builder withSubmitter(EPerson submitter) {
            this.submitter = submitter;
            return this;
        }

        public ItemResponse build() {
            return new ItemResponse(this);
        }
    }
}
