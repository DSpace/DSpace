package ua.edu.sumdu.essuir.entity;

import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Item {
    @Id
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "submitter_id")
    private Integer submitterId;

    @Column(name = "in_archive")
    private Boolean inArchive;

    @Column(name = "owning_collection")
    private Integer owningCollection;

    @Column(name = "withdrawn")
    private Boolean withdrawn;

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @Where(clause = "metadata_field_id = 133")
    private List<Metadatavalue> metadataFieldsForSpeciality = new ArrayList<>();

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @Where(clause = "metadata_field_id = 134")
    private List<Metadatavalue> metadataFieldsForPresentationDate = new ArrayList<>();

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @Where(clause = "metadata_field_id = 25")
    private List<Metadatavalue> metadataFieldsForLink = new ArrayList<>();

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @Where(clause = "metadata_field_id = 64")
    private List<Metadatavalue> metadataFieldsForTitle = new ArrayList<>();

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @Where(clause = "metadata_field_id = 12")
    private List<Metadatavalue> metadataFieldsForDateAvailable = new ArrayList<>();

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "discoverable")
    private Boolean discoverable;

    public Item() {
    }

    private Item(Builder builder) {
        itemId = builder.itemId;
        submitterId = builder.submitterId;
        inArchive = builder.inArchive;
        owningCollection = builder.owningCollection;
        withdrawn = builder.withdrawn;
        lastModified = builder.lastModified;
        discoverable = builder.discoverable;
    }

    public Integer getItemId() {
        return itemId;
    }

    public Integer getSubmitterId() {
        return submitterId;
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

    private String getMetadataFieldValue(List<Metadatavalue> values) {
        return values.stream()
                .filter(item -> item.getPlace() == 1 && item.getResourceTypeId() == 2)
                .findAny()
                .map(Metadatavalue::getTextValue)
                .orElse("");
    }

    public String getSpecialityName() {
        return getMetadataFieldValue(metadataFieldsForSpeciality);
    }

    public String getPresentationDate() {
        return getMetadataFieldValue(metadataFieldsForPresentationDate);
    }

    public String getTitle() {
        return getMetadataFieldValue(metadataFieldsForTitle);
    }

    public String getLink() {
        return getMetadataFieldValue(metadataFieldsForLink);
    }

    public String getDateAvailable() {
        return getMetadataFieldValue(metadataFieldsForDateAvailable);
    }

    public static final class Builder {
        private Integer itemId;
        private Integer submitterId;
        private Boolean inArchive;
        private Integer owningCollection;
        private Boolean withdrawn;
        private LocalDateTime lastModified;
        private Boolean discoverable;

        public Builder() {
        }

        public Builder(Item copy) {
            this.itemId = copy.getItemId();
            this.submitterId = copy.getSubmitterId();
            this.inArchive = copy.getInArchive();
            this.owningCollection = copy.getOwningCollection();
            this.withdrawn = copy.getWithdrawn();
            this.lastModified = copy.getLastModified();
            this.discoverable = copy.getDiscoverable();
        }

        public Builder withItemId(Integer itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder withSubmitterId(Integer submitterId) {
            this.submitterId = submitterId;
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

        public Item build() {
            return new Item(this);
        }
    }
}
