/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specialization of ItemRest dedicated to the Filtered Items report.
 * This class adds the owning collection property required to properly
 * display search results without compromising the expected behaviour
 * of standard ItemRest instances, in all other contexts, especially
 * when it comes to embedded contents, a criterion that is widely checked
 * against in several integration tests.
 *
 * @author Jean-François Morin (jean-francois.morin@bibl.ulaval.ca)
 */
public class FilteredItemRest {

    public static final String NAME = "filtered-item";
    public static final String CATEGORY = RestAddressableModel.CONTENT_REPORT;

    public static final String OWNING_COLLECTION = "owningCollection";

    private String uuid;
    private String name;
    private String handle;
    MetadataRest metadata = new MetadataRest();
    private boolean inArchive = false;
    private boolean discoverable = false;
    private boolean withdrawn = false;
    private Date lastModified = new Date();
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String entityType = null;
    private CollectionRest owningCollection;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public MetadataRest getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataRest metadata) {
        this.metadata = metadata;
    }

    public boolean getInArchive() {
        return inArchive;
    }

    public void setInArchive(boolean inArchive) {
        this.inArchive = inArchive;
    }

    public boolean getDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
    }

    public boolean getWithdrawn() {
        return withdrawn;
    }

    public void setWithdrawn(boolean withdrawn) {
        this.withdrawn = withdrawn;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public CollectionRest getOwningCollection() {
        return owningCollection;
    }

    public void setOwningCollection(CollectionRest owningCollection) {
        this.owningCollection = owningCollection;
    }

}
