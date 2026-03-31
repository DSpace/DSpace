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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.dspace.app.audit.MetadataEvent;
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


    // accumulate information to be stored in the Audit system
    // data is stored in a structured way, so no need to concatenate
    @Transient
    private Set<MetadataEvent> metadataEventDetails;

    /**
     * Using a Set instead of a List avoids Hibernate "bag" semantics, where any change
     * to a List (without @OrderColumn) triggers a DELETE-all + INSERT-all of every row.
     * With a Set, Hibernate can track individual element additions and removals.
     *
     * The same order should be applied inside this comparator
     * {@link MetadataValueComparators#defaultComparator} to preserve
     * ordering while the list has been modified and not yet persisted
     * and reloaded.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dSpaceObject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MetadataValue> metadata = new LinkedHashSet<>();

    /**
     * Using a Set avoids Hibernate bag recreation on every change.
     * The oldest handle (lowest id) is the preferred handle; sorting is done in {@link #getHandle()}.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dso")
    private Set<Handle> handles = new LinkedHashSet<>();

    /**
     * Using a Set avoids Hibernate bag recreation on every change.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dSpaceObject", cascade = CascadeType.ALL)
    private final Set<ResourcePolicy> resourcePolicies = new LinkedHashSet<>();

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
     * Deprecated: Use {@link #addMetadataEventDetails(MetadataEvent event)} instead.
     * Add a string to the cache of event details.  Automatically
     * separates entries with a comma.
     * Subclass can just start calling addDetails, since it creates
     * the cache if it needs to.
     *
     * @param d detail string to add.
     */
    @Deprecated
    protected void addDetails(String d) {
        if (eventDetails == null) {
            eventDetails = new StringBuffer(d);
        } else {
            eventDetails.append(", ").append(d);
        }
    }

    /**
     * Deprecated: Use {@link #getMetadataEventDetails()} instead.
     *
     * @return summary of event details, or null if there are none.
     */
    @Deprecated
    public String getDetails() {
        return eventDetails == null ? null : eventDetails.toString();
    }

    /**
     * Add a MetadataEvent event in the list of metadata event details.
     * so that is stored in the audit system.
     *
     * @param event detail object to add.
     */
    public void addMetadataEventDetails(MetadataEvent event) {
        if (metadataEventDetails == null) {
            metadataEventDetails = new HashSet<>();
        }
        metadataEventDetails.add(event);
    }

    /**
     * @return list of metadata event details, or empty list if there is none.
     */
    public List<MetadataEvent> getMetadataEventDetails() {
        return metadataEventDetails == null ? List.of() : metadataEventDetails.stream().toList();
    }

    /**
     * Clear the list of metadata event details to avoid duplication in audit logs.
     */
    public void clearMetadataEventDetails() {
        if (metadataEventDetails != null) {
            metadataEventDetails.clear();
        }
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
        return handles.stream()
            .min(Comparator.comparingInt(Handle::getID))
            .map(Handle::getHandle)
            .orElse(null);
    }

    void setHandle(Set<Handle> handle) {
        this.handles = handle;
    }

    /**
     * Append to this object's collection of Handles.
     *
     * @param handle the new Handle to be added.
     */
    public void addHandle(Handle handle) {
        this.handles.add(handle);
    }

    /**
     * Remove a Handle from this object's collection.
     *
     * @param handle the Handle to be removed.
     */
    public void removeHandle(Handle handle) {
        this.handles.remove(handle);
    }

    /**
     * Get all Handles associated with this object.
     * Returns a defensive copy sorted by id ascending (oldest handle first).
     *
     * @return sorted list of handles
     */
    public List<Handle> getHandles() {
        List<Handle> sorted = new ArrayList<>(handles);
        sorted.sort(Comparator.comparingInt(Handle::getID));
        return sorted;
    }

    /**
     * Get the metadata values for this object.
     * Returns a defensive copy sorted by metadata field and place,
     * matching the previous @OrderBy("metadataField, place") behavior.
     *
     * @return sorted list of metadata values
     */
    public List<MetadataValue> getMetadata() {
        List<MetadataValue> sorted = new ArrayList<>(metadata);
        sorted.sort(MetadataValueComparators.defaultComparator);
        return sorted;
    }

    /**
     * Replace the entire metadata set.
     *
     * @param metadata the new metadata values
     */
    public void setMetadata(List<MetadataValue> metadata) {
        this.metadata = new LinkedHashSet<>(metadata);
    }

    protected void removeMetadata(MetadataValue metadataValue) {
        setMetadataModified();
        this.metadata.remove(metadataValue);
    }

    protected void removeMetadata(List<MetadataValue> metadataValues) {
        setMetadataModified();
        this.metadata.removeAll(metadataValues);
    }

    protected void addMetadata(MetadataValue metadataValue) {
        setMetadataModified();
        this.metadata.add(metadataValue);
        addMetadataEventDetails(new MetadataEvent(metadataValue, MetadataEvent.ADD));
    }

    /**
     * Get the resource policies for this object.
     * Returns a defensive copy.
     *
     * @return list of resource policies
     */
    public List<ResourcePolicy> getResourcePolicies() {
        return new ArrayList<>(resourcePolicies);
    }

    /**
     * Remove a single resource policy from this object.
     *
     * @param policy the resource policy to remove
     */
    public void removeResourcePolicy(ResourcePolicy policy) {
        this.resourcePolicies.remove(policy);
    }

    /**
     * Remove multiple resource policies from this object.
     *
     * @param policies the resource policies to remove
     */
    public void removeResourcePolicies(Collection<ResourcePolicy> policies) {
        this.resourcePolicies.removeAll(policies);
    }

    /**
     * Remove all resource policies from this object.
     */
    public void clearResourcePolicies() {
        this.resourcePolicies.clear();
    }

    /**
     * Get the raw backing collection of handles, without creating a defensive copy.
     * Use this only for Hibernate initialization checks ({@code Hibernate.isInitialized()}).
     * For normal iteration, use {@link #getHandles()} instead.
     *
     * @return the raw handles collection (may be a Hibernate-managed proxy)
     */
    public java.util.Collection<Handle> getHandlesInternal() {
        return handles;
    }

    /**
     * Get the raw backing collection of resource policies, without creating a defensive copy.
     * Use this only for Hibernate initialization checks ({@code Hibernate.isInitialized()}).
     * For normal iteration, use {@link #getResourcePolicies()} instead.
     *
     * @return the raw resource policies collection (may be a Hibernate-managed proxy)
     */
    public java.util.Collection<ResourcePolicy> getResourcePoliciesInternal() {
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
