package ua.edu.sumdu.essuir.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Publication {
    @JsonProperty("title")
    private String title;
    @JsonProperty("citation")
    private String citation;
    @JsonProperty("authors")
    private String authors;
    @JsonProperty("type")
    private String type;

    public Publication() {}

    private Publication(Builder builder) {
        title = builder.title;
        citation = builder.citation;
        authors = builder.authors;
        type = builder.type;
    }

    public String getTitle() {
        return title;
    }

    public String getCitation() {
        return citation;
    }

    public String getAuthors() {
        return authors;
    }

    public String getType() {
        return type;
    }

    public static final class Builder {
        private String title;
        private String citation;
        private String authors;
        private String type;

        public Builder() {
        }

        public Builder(Publication copy) {
            this.title = copy.getTitle();
            this.citation = copy.getCitation();
            this.authors = copy.getAuthors();
            this.type = copy.getType();
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withCitation(String citation) {
            this.citation = citation;
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

        public Publication build() {
            return new Publication(this);
        }
    }
}
