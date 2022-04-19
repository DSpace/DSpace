/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.dspace.content.comparator.NameAscendingComparator;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.hibernate.proxy.HibernateProxyHelper;

/**
 * Class representing an item in DSpace.
 * <P>
 * This class holds in memory the item Dublin Core metadata, the bundles in the
 * item, and the bitstreams in those bundles. When modifying the item, if you
 * modify the Dublin Core or the "in archive" flag, you must call
 * <code>update</code> for the changes to be written to the database.
 * Creating, adding or removing bundles or bitstreams has immediate effect in
 * the database.
 *
 * @author Robert Tansley
 * @author Martin Hald
 */
@Entity
@Table(name = "item")
public class Item extends DSpaceObject implements DSpaceObjectLegacySupport {
    /**
     * Wild card for Dublin Core metadata qualifiers/languages
     */
    public static final String ANY = "*";

    @Column(name = "item_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name = "in_archive")
    private boolean inArchive = false;

    @Column(name = "discoverable")
    private boolean discoverable = false;

    @Column(name = "withdrawn")
    private boolean withdrawn = false;

    @Column(name = "last_modified", columnDefinition = "timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified = new Date();

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "owning_collection")
    private Collection owningCollection;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "template")
    private Collection templateItemOf;

    /**
     * The e-person who submitted this item
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id")
    private EPerson submitter = null;


    /**
     * The bundles in this item - kept in sync with DB
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinTable(
        name = "collection2item",
        joinColumns = {@JoinColumn(name = "item_id")},
        inverseJoinColumns = {@JoinColumn(name = "collection_id")}
    )
    private final Set<Collection> collections = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "items")
    private final List<Bundle> bundles = new ArrayList<>();

    @Transient
    private transient ItemService itemService;

    /**
     * True if anything else was changed since last metadata retrieval()
     * (to drive metadata cache)
     */
    @Transient
    private boolean modifiedMetadataCache = true;

    @Transient
    private List<MetadataValue> cachedMetadata = new ArrayList<>();

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.ItemService#create(Context, WorkspaceItem)}
     */
    protected Item() {

    }

    /**
     * Takes a pre-determined UUID to be passed to the object to allow for the
     * restoration of previously defined UUID's.
     *
     * @param uuid Takes a uuid to be passed to the Pre-Defined UUID Generator
     */
    protected Item(UUID uuid) {
        this.predefinedUUID = uuid;
    }

    /**
     * Find out if the item is part of the main archive
     *
     * @return true if the item is in the main archive
     */
    public boolean isArchived() {
        return inArchive;
    }

    /**
     * Find out if the item has been withdrawn
     *
     * @return true if the item has been withdrawn
     */
    public boolean isWithdrawn() {
        return withdrawn;
    }


    /**
     * Set an item to be withdrawn, do NOT make this method public, use itemService().withdraw() to withdraw an item
     *
     * @param withdrawn
     */
    void setWithdrawn(boolean withdrawn) {
        this.withdrawn = withdrawn;
    }

    /**
     * Find out if the item is discoverable
     *
     * @return true if the item is discoverable
     */
    public boolean isDiscoverable() {
        return discoverable;
    }

    /**
     * Get the date the item was last modified, or the current date if
     * last_modified is null
     *
     * @return the date the item was last modified, or the current date if the
     * column is null.
     */
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Set the "is_archived" flag. This is public and only
     * <code>WorkflowItem.archive()</code> should set this.
     *
     * @param isArchived new value for the flag
     */
    public void setArchived(boolean isArchived) {
        this.inArchive = isArchived;
        setModified();
    }

    /**
     * Set the "discoverable" flag. This is public and only
     *
     * @param discoverable new value for the flag
     */
    public void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
        setModified();
    }

    /**
     * Set the owning Collection for the item
     *
     * @param c Collection
     */
    public void setOwningCollection(Collection c) {
        this.owningCollection = c;
        setModified();
    }

    /**
     * Get the owning Collection for the item
     *
     * @return Collection that is the owner of the item
     */
    public Collection getOwningCollection() {
        return owningCollection;
    }

    /**
     * Get the e-person that originally submitted this item
     *
     * @return the submitter
     */
    public EPerson getSubmitter() {
        return submitter;
    }

    /**
     * Set the e-person that originally submitted this item. This is a public
     * method since it is handled by the WorkspaceItem class in the ingest
     * package. <code>update</code> must be called to write the change to the
     * database.
     *
     * @param sub the submitter
     */
    public void setSubmitter(EPerson sub) {
        this.submitter = sub;
        setModified();
    }

    /**
     * Get the collections this item is in. The order is sorted ascending by collection name.
     *
     * @return the collections this item is in, if any.
     */
    public List<Collection> getCollections() {
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Collection[] output = collections.toArray(new Collection[] {});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
    }

    void addCollection(Collection collection) {
        collections.add(collection);
    }

    void removeCollection(Collection collection) {
        collections.remove(collection);
    }

    public void clearCollections() {
        collections.clear();
    }

    public Collection getTemplateItemOf() {
        return templateItemOf;
    }


    void setTemplateItemOf(Collection templateItemOf) {
        this.templateItemOf = templateItemOf;
    }

    /**
     * Get the bundles in this item.
     *
     * @return the bundles in an unordered array
     */
    public List<Bundle> getBundles() {
        return bundles;
    }

    /**
     * Get the bundles matching a bundle name (name corresponds roughly to type)
     *
     * @param name
     *            name of bundle (ORIGINAL/TEXT/THUMBNAIL)
     *
     * @return the bundles in an unordered array
     */
    public List<Bundle> getBundles(String name) {
        List<Bundle> matchingBundles = new ArrayList<>();
         // now only keep bundles with matching names
        List<Bundle> bunds = getBundles();
        for (Bundle bundle : bunds) {
            if (name.equals(bundle.getName())) {
                matchingBundles.add(bundle);
            }
        }
        return matchingBundles;
    }

    /**
     * Add a bundle to the item, should not be made public since we don't want to skip business logic
     *
     * @param bundle the bundle to be added
     */
    void addBundle(Bundle bundle) {
        bundles.add(bundle);
    }

    /**
     * Remove a bundle from item, should not be made public since we don't want to skip business logic
     *
     * @param bundle the bundle to be removed
     */
    void removeBundle(Bundle bundle) {
        bundles.remove(bundle);
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Item as
     * this object, <code>false</code> otherwise.
     *
     * @param obj object to compare to
     * @return <code>true</code> if object passed in represents the same item
     * as this object
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Item)) {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (this.getClass() != objClass) {
            return false;
        }
        final Item otherItem = (Item) obj;
        return this.getID().equals(otherItem.getID());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash += 71 * hash + getType();
        hash += 71 * hash + getID().hashCode();
        return hash;
    }

    /**
     * return type found in Constants
     *
     * @return int Constants.ITEM
     */
    @Override
    public int getType() {
        return Constants.ITEM;
    }

    @Override
    public String getName() {
        return getItemService().getMetadataFirstValue(this, MetadataSchemaEnum.DC.getName(), "title", null, Item.ANY);
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    public ItemService getItemService() {
        if (itemService == null) {
            itemService = ContentServiceFactory.getInstance().getItemService();
        }
        return itemService;
    }

    @Override
    protected void setMetadataModified() {
        super.setMetadataModified();
        modifiedMetadataCache = true;
    }

    public boolean isModifiedMetadataCache() {
        return modifiedMetadataCache;
    }

    protected List<MetadataValue> getCachedMetadata() {
        return cachedMetadata;
    }

    protected void setCachedMetadata(List<MetadataValue> cachedMetadata) {
        this.cachedMetadata = cachedMetadata;
        modifiedMetadataCache = false;
    }
}
