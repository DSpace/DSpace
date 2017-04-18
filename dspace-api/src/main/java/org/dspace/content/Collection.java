/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.comparator.NameAscendingComparator;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.*;
import org.dspace.eperson.Group;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.sql.SQLException;
import java.util.*;
import org.dspace.authorize.AuthorizeException;

/**
 * Class representing a collection.
 * <P>
 * The collection's metadata (name, introductory text etc), workflow groups, and
 * default group of submitters are loaded into memory. Changes to metadata are
 * not written to the database until <code>update</code> is called. If you
 * create or remove a workflow group, the change is only reflected in the
 * database after calling <code>update</code>. The default group of
 * submitters is slightly different - creating or removing this has instant
 * effect.
 *
 * @author Robert Tansley
 * @version $Revision$
 */
@Entity
@Table(name="collection")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class Collection extends DSpaceObject implements DSpaceObjectLegacySupport
{

    @Column(name="collection_id", insertable = false, updatable = false)
    private Integer legacyId;

    /** The logo bitstream */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_bitstream_id")
    private Bitstream logo;

    /** The item template */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_item_id")
    private Item template;

    /**
     * Groups corresponding to workflow steps - NOTE these start from one, so
     * workflowGroups[0] corresponds to workflow_step_1.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_1")
    private Group workflowStep1;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_2")
    private Group workflowStep2;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_3")
    private Group workflowStep3;


    @OneToOne
    @JoinColumn(name = "submitter")
    /** The default group of administrators */
    private Group submitters;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin")
    /** The default group of administrators */
    private Group admins;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinTable(
            name = "community2collection",
            joinColumns = {@JoinColumn(name = "collection_id") },
            inverseJoinColumns = {@JoinColumn(name = "community_id") }
    )
    private Set<Community> communities = new HashSet<>();

    @Transient
    private transient CollectionService collectionService;

    // Keys for accessing Collection metadata
    @Transient
    public static final String COPYRIGHT_TEXT = "copyright_text";
    @Transient
    public static final String INTRODUCTORY_TEXT = "introductory_text";
    @Transient
    public static final String SHORT_DESCRIPTION = "short_description";
    @Transient
    public static final String SIDEBAR_TEXT = "side_bar_text";
    @Transient
    public static final String PROVENANCE_TEXT = "provenance_description";

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.CollectionService#create(Context, Community)}
     * or
     * {@link org.dspace.content.service.CollectionService#create(Context, Community, String)}
     *
     */
    protected Collection()
    {

    }

    @Override
    public String getName()
    {
        String value = getCollectionService().getMetadataFirstValue(this, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        return value == null ? "" : value;
    }

    /**
     * Get the logo for the collection. <code>null</code> is returned if the
     * collection does not have a logo.
     *
     * @return the logo of the collection, or <code>null</code>
     */
    public Bitstream getLogo()
    {
        return logo;
    }

    protected void setLogo(Bitstream logo) {
        this.logo = logo;
        setModified();
    }


    /**
     * Get the default group of submitters, if there is one. Note that the
     * authorization system may allow others to submit to the collection, so
     * this is not necessarily a definitive list of potential submitters.
     * <P>
     * The default group of submitters for collection 100 is the one called
     * <code>collection_100_submit</code>.
     *
     * @return the default group of submitters, or <code>null</code> if there
     *         is no default group.
     */
    public Group getSubmitters()
    {
        return submitters;
    }

    /**
     * Set the default group of submitters
     *
     * Package protected in order to preven unauthorized calls to this method
     *
     * @param submitters the group of submitters
     */
    void setSubmitters(Group submitters) {
        this.submitters = submitters;
        setModified();
    }


    /**
     * Get the default group of administrators, if there is one. Note that the
     * authorization system may allow others to be administrators for the
     * collection.
     * <P>
     * The default group of administrators for collection 100 is the one called
     * <code>collection_100_admin</code>.
     *
     * @return group of administrators, or <code>null</code> if there is no
     *         default group.
     */
    public Group getAdministrators()
    {
        return admins;
    }

    void setAdmins(Group admins) {
        this.admins = admins;
        setModified();
    }

    public Group getWorkflowStep1() {
        return workflowStep1;
    }

    public Group getWorkflowStep2() {
        return workflowStep2;
    }

    public Group getWorkflowStep3() {
        return workflowStep3;
    }

    void setWorkflowStep1(Group workflowStep1) {
        this.workflowStep1 = workflowStep1;
        setModified();
    }

    void setWorkflowStep2(Group workflowStep2) {
        this.workflowStep2 = workflowStep2;
        setModified();
    }

    void setWorkflowStep3(Group workflowStep3) {
        this.workflowStep3 = workflowStep3;
        setModified();
    }

    /**
     * Get the license that users must grant before submitting to this
     * collection.
     *
     * @return the license for this collection
     */
    public String getLicenseCollection()
    {
        return getCollectionService().getMetadata(this, "license");
    }

    /**
     * Set the license for this collection. Passing in <code>null</code> means
     * that the site-wide default will be used.
     *
     * @param context context
     * @param license the license, or <code>null</code>
     * @throws SQLException if database error
     */
    public void setLicense(Context context, String license) throws SQLException {
        getCollectionService().setMetadata(context, this, "license", license);
    }

    /**
     * Get the template item for this collection. <code>null</code> is
     * returned if the collection does not have a template. Submission
     * mechanisms may copy this template to provide a convenient starting point
     * for a submission.
     *
     * @return the item template, or <code>null</code>
     * @throws SQLException if database error
     */
    public Item getTemplateItem() throws SQLException
    {
        return template;
    }

    void setTemplateItem(Item template) {
        this.template = template;
        setModified();
    }

    /**
     * Get the communities this collection appears in
     *
     * @return array of <code>Community</code> objects
     * @throws SQLException if database error
     */
    public List<Community> getCommunities() throws SQLException
    {
        // We return a copy because we do not want people to add elements to this collection directly.
        // We return a list to maintain backwards compatibility
        Community[] output = communities.toArray(new Community[]{});
        Arrays.sort(output, new NameAscendingComparator());
        return Arrays.asList(output);
    }

    void addCommunity(Community community) {
        this.communities.add(community);
        setModified();
    }

    void removeCommunity(Community community) {
        this.communities.remove(community);
        setModified();
    }


    /**
     * Return <code>true</code> if <code>other</code> is the same Collection
     * as this object, <code>false</code> otherwise
     *
     * @param other
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         collection as this object
     */
     @Override
     public boolean equals(Object other)
     {
         if (other == null)
         {
             return false;
         }
         Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(other);
         if (this.getClass() != objClass)
         {
             return false;
         }
         final Collection otherCollection = (Collection) other;
         if (!this.getID().equals(otherCollection.getID() ))
         {
             return false;
         }

         return true;
     }

     @Override
     public int hashCode()
     {
         int hash = 5;
         hash += 71 * hash + getType();
         hash += 71 * hash + getID().hashCode();
         return hash;
     }

    /**
     * return type found in Constants
     *
     * @return int Constants.COLLECTION
     */
    @Override
    public int getType()
    {
        return Constants.COLLECTION;
    }

    public void setWorkflowGroup(Context context, int step, Group g)
            throws SQLException, AuthorizeException 
    {
        getCollectionService().setWorkflowGroup(context, this, step, g);
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    private CollectionService getCollectionService() {
        if(collectionService == null)
        {
            collectionService = ContentServiceFactory.getInstance().getCollectionService();
        }
        return collectionService;
    }
}
