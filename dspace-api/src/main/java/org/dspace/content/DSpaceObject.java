/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.ReloadableEntity;
import org.dspace.handle.Handle;
import org.hibernate.annotations.GenericGenerator;

/**
 * Abstract base class for DSpace objects
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "dspaceobject")
public abstract class DSpaceObject implements Serializable, ReloadableEntity<java.util.UUID> {
    @Id
    @GeneratedValue(generator = "predefined-uuid")
    @GenericGenerator(name = "predefined-uuid", strategy = "org.dspace.content.PredefinedUUIDGenerator")
    @Column(name = "uuid", unique = true, nullable = false, insertable = true, updatable = false)
    protected java.util.UUID id;

    // accumulate information to add to "detail" element of content Event,
    // e.g. to document metadata fields touched, etc.
    @Transient
    private StringBuffer eventDetails = null;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dSpaceObject", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("metadataField, place")
    private List<MetadataValue> metadata = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dso")
    // OrderBy is here to ensure that the oldest handle is retrieved first.
    // Multiple handles are assigned to the latest version of an item.
    // The original handle will have the lowest identifier.  This handle is the
    // preferred handle.
    @OrderBy("id ASC")
    private List<Handle> handles = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dSpaceObject", cascade = CascadeType.ALL)
    private final List<ResourcePolicy> resourcePolicies = new ArrayList<>();

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    @Transient
    private boolean modifiedMetadata = false;

    /**
     * Flag set when data is modified, for events
     */
    @Transient
    private boolean modified = false;

    /**
     * This will read our predefinedUUID property to pass it along to the UUID generator
     */
    @Transient
    protected UUID predefinedUUID;
    public UUID getPredefinedUUID() {
        return predefinedUUID;
    }

    protected DSpaceObject() {

    }

    /**
     * Reset the cache of event details.
     */
    public void clearDetails() {
        eventDetails = null;
    }

    /**
     * Add a string to the cache of event details.  Automatically
     * separates entries with a comma.
     * Subclass can just start calling addDetails, since it creates
     * the cache if it needs to.
     *
     * @param d detail string to add.
     */
    protected void addDetails(String d) {
        if (eventDetails == null) {
            eventDetails = new StringBuffer(d);
        } else {
            eventDetails.append(", ").append(d);
        }
    }

    /**
     * @return summary of event details, or null if there are none.
     */
    public String getDetails() {
        return (eventDetails == null ? null : eventDetails.toString());
    }

    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    public abstract int getType();

    /**
     * Get the internal ID (database primary key) of this object
     *
     * @return internal ID of object
     */
    @Override
    public UUID getID() {
        return id;
    }

    public abstract String getName();

    /**
     * Get the Handle of the object. This may return <code>null</code>
     *
     * @return Handle of the object, or <code>null</code> if it doesn't have
     * one
     */
    public String getHandle() {
        return (CollectionUtils.isNotEmpty(handles) ? handles.get(0).getHandle() : null);
    }

    void setHandle(List<Handle> handle) {
        this.handles = handle;
    }

    /**
     * Append to this object's list of Handles.
     * @param handle the new Handle to be added.
     */
    public void addHandle(Handle handle) {
        this.handles.add(handle);
    }

    public List<Handle> getHandles() {
        return handles;
    }

    public List<MetadataValue> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetadataValue> metadata) {
        this.metadata = metadata;
    }

    protected void removeMetadata(MetadataValue metadataValue) {
        setMetadataModified();
        getMetadata().remove(metadataValue);
    }

    protected void removeMetadata(List<MetadataValue> metadataValues) {
        setMetadataModified();
        getMetadata().removeAll(metadataValues);
    }


    protected void addMetadata(MetadataValue metadataValue) {
        setMetadataModified();
        getMetadata().add(metadataValue);
        addDetails(metadataValue.getMetadataField().toString());
    }

    public List<ResourcePolicy> getResourcePolicies() {
        return resourcePolicies;
    }

    public boolean isMetadataModified() {
        return modifiedMetadata;
    }

    protected void setMetadataModified() {
        this.modifiedMetadata = true;
    }

    public boolean isModified() {
        return modified;
    }

    public void clearModified() {
        this.modified = false;
    }

    protected void setModified() {
        this.modified = true;
    }

}
