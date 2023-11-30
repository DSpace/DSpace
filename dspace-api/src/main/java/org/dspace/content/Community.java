/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.browse.ItemCountException;
import org.dspace.content.comparator.NameAscendingComparator;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

/**
 * Class representing a community
 * <P>
 * The community's metadata (name, introductory text etc.) is loaded into'
 * memory. Changes to this metadata are only reflected in the database after
 * <code>update</code> is called.
 *
 * @author Robert Tansley
 */
@Entity
@Table(name = "community")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class Community extends DSpaceObject implements DSpaceObjectLegacySupport {
    @Column(name = "community_id", insertable = false, updatable = false)
    private Integer legacyId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "community2community",
        joinColumns = {@JoinColumn(name = "parent_comm_id")},
        inverseJoinColumns = {@JoinColumn(name = "child_comm_id")}
    )
    private final Set<Community> subCommunities = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "subCommunities")
    private final Set<Community> parentCommunities = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "communities", cascade = {CascadeType.PERSIST})
    private final Set<Collection> collections = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "admin")
    /** The default group of administrators */
    private Group admins;

    /**
     * The logo bitstream
     */
    @OneToOne
    @JoinColumn(name = "logo_bitstream_id")
    private Bitstream logo = null;

    @Transient
    protected transient CommunityService communityService;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.CommunityService#create(Community, Context)}
     * or
     * {@link org.dspace.content.service.CommunityService#create(Community, Context, String)}
     */
    protected Community() {

    }

    /**
     * Takes a pre-determined UUID to be passed to the object to allow for the
     * restoration of previously defined UUID's.
     *
     * @param uuid Takes a uuid to be passed to the Pre-Defined UUID Generator
     */
    protected Community(UUID uuid) {
        this.predefinedUUID = uuid;
    }

    void addSubCommunity(Community subCommunity) {
        subCommunities.add(subCommunity);
        setModified();
    }

    public void removeSubCommunity(Community subCommunity) {
        subCommunities.remove(subCommunity);
        setModified();
    }

    /**
     * Get the logo for the community. <code>null</code> is return if the
     * community does not have a logo.
     *
     * @return the logo of the community, or <code>null</code>
     */
    public Bitstream getLogo() {
        return logo;
    }

    void setLogo(Bitstream logo) {
        this.logo = logo;
        setModified();
    }

    /**
     * Get the default group of administrators, if there is one. Note that the
     * authorization system may allow others to be administrators for the
     * community.
     * <P>
     * The default group of administrators for community 100 is the one called
     * <code>community_100_admin</code>.
     *
     * @return group of administrators, or <code>null</code> if there is no
     * default group.
     */
    public Group getAdministrators() {
        return admins;
    }

    void setAdmins(Group admins) {
        this.admins = admins;
        setModified();
    }

    /**
     * Get the collections in this community. Throws an SQLException because
     * creating a community object won't load in all collections.
     *
     * @return array of Collection objects
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

    /**
     * Get the immediate sub-communities of this community. Throws an
     * SQLException because creating a community object won't load in all
     * collections.
     *
     * @return array of Community objects
     */
    public List<Community> getSubcommunities() {
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Community[] output = subCommunities.toArray(new Community[] {});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
    }

    /**
     * Return the parent community of this community, or null if the community
     * is top-level
     *
     * @return the immediate parent community, or null if top-level
     */
    public List<Community> getParentCommunities() {
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Community[] output = parentCommunities.toArray(new Community[] {});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
    }

    void addParentCommunity(Community parentCommunity) {
        parentCommunities.add(parentCommunity);
    }

    void clearParentCommunities() {
        parentCommunities.clear();
    }

    public void removeParentCommunity(Community parentCommunity) {
        parentCommunities.remove(parentCommunity);
        setModified();
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Community
     * as this object, <code>false</code> otherwise
     *
     * @param other object to compare to
     * @return <code>true</code> if object passed in represents the same
     * community as this object
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Community)) {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(other);
        if (this.getClass() != objClass) {
            return false;
        }
        final Community otherCommunity = (Community) other;
        return this.getID().equals(otherCommunity.getID());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getID()).toHashCode();
    }

    /**
     * return type found in Constants
     *
     * @return Community type
     */
    @Override
    public int getType() {
        return Constants.COMMUNITY;
    }

    @Override
    public String getName() {
        String value = getCommunityService()
            .getMetadataFirstValue(this, MetadataSchemaEnum.DC.getName(), "title", null, Item.ANY);
        return value == null ? "" : value;
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    private CommunityService getCommunityService() {
        if (communityService == null) {
            communityService = ContentServiceFactory.getInstance().getCommunityService();
        }
        return communityService;
    }

    /**
     * return count of the community items
     *
     * @return int
     */
    public int countArchivedItems() {
        try {
            return communityService.countArchivedItems(this);
        } catch (ItemCountException e) {
            throw new RuntimeException(e);
        }
    }
}
