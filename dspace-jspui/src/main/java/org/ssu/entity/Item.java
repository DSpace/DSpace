package org.ssu.entity;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Where(clause = "in_archive = true")
public class Item {
    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submitter_id", referencedColumnName = "eperson_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private EPerson submitter;

    @Column(name = "in_archive")
    private Boolean inArchive;

    @Column(name = "owning_collection")
    private Integer owningCollection;

    @Column(name = "withdrawn")
    private Boolean withdrawn;



    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "discoverable")
    private Boolean discoverable;

    public Item() {
    }

    private Item(Builder builder) {
        itemId = builder.itemId;
        submitter = builder.submitter;
        inArchive = builder.inArchive;
        owningCollection = builder.owningCollection;
        withdrawn = builder.withdrawn;
        lastModified = builder.lastModified;
        discoverable = builder.discoverable;
        specialityName = builder.specialityName;
        presentationDate = builder.presentationDate;
        title = builder.title;
        link = builder.link;
        dateAvailable = builder.dateAvailable;
    }

    public EPerson getSubmitter() {
        return submitter;
    }

    public Integer getItemId() {
        return itemId;
    }

    public Boolean getInArchive() {
        return inArchive;
    }

    public Integer getOwningCollection() {
        return owningCollection;
    }

    public Boolean getWithdrawn() {
        return withdrawn;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public Boolean getDiscoverable() {
        return discoverable;
    }

    @Transient
    private String specialityName;

    @Transient
    private String presentationDate;

    @Transient
    private String title;

    @Transient
    private String link;

    @Transient
    private String dateAvailable;

    public String getSpecialityName() {
        return specialityName;
    }

    public String getPresentationDate() {
        return presentationDate;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public LocalDate getDateAvailable() {
        return LocalDateTime.parse(dateAvailable.isEmpty() ? "2001-01-01T01:01:01Z": dateAvailable, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")).toLocalDate();
    }


    public static final class Builder {
        private Integer itemId;
        private EPerson submitter;
        private Boolean inArchive;
        private Integer owningCollection;
        private Boolean withdrawn;
        private LocalDateTime lastModified;
        private Boolean discoverable;
        private String specialityName;
        private String presentationDate;
        private String title;
        private String link;
        private String dateAvailable;

        public Builder() {
        }

        public Builder(Item copy) {
            this.itemId = copy.getItemId();
            this.submitter = copy.getSubmitter();
            this.inArchive = copy.getInArchive();
            this.owningCollection = copy.getOwningCollection();
            this.withdrawn = copy.getWithdrawn();
            this.lastModified = copy.getLastModified();
            this.discoverable = copy.getDiscoverable();
            this.specialityName = copy.getSpecialityName();
            this.presentationDate = copy.getPresentationDate();
            this.title = copy.getTitle();
            this.link = copy.getLink();
            this.dateAvailable = copy.getDateAvailable().toString();
        }

        public Builder withItemId(Integer itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder withSubmitter(EPerson submitter) {
            this.submitter = submitter;
            return this;
        }

        public Builder withInArchive(Boolean inArchive) {
            this.inArchive = inArchive;
            return this;
        }

        public Builder withOwningCollection(Integer owningCollection) {
            this.owningCollection = owningCollection;
            return this;
        }

        public Builder withWithdrawn(Boolean withdrawn) {
            this.withdrawn = withdrawn;
            return this;
        }

        public Builder withLastModified(LocalDateTime lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public Builder withDiscoverable(Boolean discoverable) {
            this.discoverable = discoverable;
            return this;
        }

        public Builder withSpecialityName(String specialityName) {
            this.specialityName = specialityName;
            return this;
        }

        public Builder withPresentationDate(String presentationDate) {
            this.presentationDate = presentationDate;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withLink(String link) {
            this.link = link;
            return this;
        }

        public Builder withDateAvailable(String dateAvailable) {
            this.dateAvailable = dateAvailable;
            return this;
        }

        public Item build() {
            return new Item(this);
        }
    }
}