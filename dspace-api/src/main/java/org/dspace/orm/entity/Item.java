/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.orm.entity.content.DSpaceObjectType;
import org.dspace.services.AuthorizationService;
import org.dspace.services.auth.Action;
import org.dspace.services.auth.DSpaceAuthorizeConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Entity
@Table(name = "item")
@SequenceGenerator(name="item_gen", sequenceName="item_seq")
@Configurable
public class Item extends DSpaceObject {
	@Autowired AuthorizationService authService;
	
    private Eperson submitter;
    private boolean inArchive;
    private boolean withDrawn;
    private boolean discoverable;
    private Date lastModified;
    private Collection owningCollection;
    private List<Bundle> bundles;
    private List<Collection> collections;
    private List<Collection> templateItemCollections;
    
    @Id
    @Column(name = "item_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="item_gen")
    public int getID() {
        return id;
    }

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "collection2item", joinColumns = { @JoinColumn(name = "item_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "collection_id", nullable = false) })
    public List<Collection> getCollections() {
        return collections;
    }
   
    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id", nullable = true)
    public Eperson getSubmitter() {
        return submitter;
    }

    public void setSubmitter(Eperson submitter) {
        this.submitter = submitter;
    }
    
    @Column(name = "in_archive", nullable = true)
    public boolean getInArchive() {
        return inArchive;
    }

    public void setInArchive(boolean inArchive) {
        this.inArchive = inArchive;
    }

    @Column(name = "withdrawn", nullable = true)
    public boolean getWithDrawn() {
        return withDrawn;
    }

    public void setWithDrawn(boolean withDrawn) {
        this.withDrawn = withDrawn;
    }
    
    @Column(name = "discoverable", nullable = true)
    public boolean getDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
    }
    
    @Column(name = "last_modified", nullable = true)
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owning_collection", nullable = true)
    public Collection getOwningCollection() {
        return owningCollection;
    }

    public void setOwningCollection(Collection owningCollection) {
        this.owningCollection = owningCollection;
    }
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "item2bundle", joinColumns = { @JoinColumn(name = "item_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "bundle_id", nullable = false) })
    public List<Bundle> getBundles() {
        return bundles;
    }
    
    public void setBundles(List<Bundle> bundls) {
        this.bundles = bundls;
    }

	@Override
	@Transient
	public DSpaceObjectType getType() {
		return DSpaceObjectType.ITEM;
	}
	

	@Transient
    public IDSpaceObject getAdminObject(Action action)
    {
        DSpaceObject adminObject = null;
        Collection collection = getOwningCollection();
        Community community = null;
        if (collection != null)
        {
            List<Community> communities = collection.getParents();
            if (communities != null && !communities.isEmpty())
            {
                community = communities.get(0);
            }
        }
        else
        {
            // is a template item?
        	if (this.getTemplateItemCollections() != null && 
        			!this.getTemplateItemCollections().isEmpty())
        		return this.getTemplateItemCollections().get(0);
        }
        
        DSpaceAuthorizeConfiguration config = authService.getConfiguration();
        
        switch (action)
        {
            case ADD:
                // ADD a cc license is less general than add a bitstream but we can't/won't
                // add complex logic here to know if the ADD action on the item is required by a cc or
                // a generic bitstream so simply we ignore it.. UI need to enforce the requirements.
                if (config.canItemAdminPerformBitstreamCreation())
                {
                    adminObject = this;
                }
                else if (config.canCollectionAdminPerformBitstreamCreation())
                {
                    adminObject = collection;
                }
                else if (config.canCommunityAdminPerformBitstreamCreation())
                {
                    adminObject = community;
                }
                break;
            case REMOVE:
                // see comments on ADD action, same things...
                if (config.canItemAdminPerformBitstreamDeletion())
                {
                    adminObject = this;
                }
                else if (config.canCollectionAdminPerformBitstreamDeletion())
                {
                    adminObject = collection;
                }
                else if (config.canCommunityAdminPerformBitstreamDeletion())
                {
                    adminObject = community;
                }
                break;
            case DELETE:
                if (getOwningCollection() != null)
                {
                    if (config.canCollectionAdminPerformItemDeletion())
                    {
                        adminObject = collection;
                    }
                    else if (config.canCommunityAdminPerformItemDeletion())
                    {
                        adminObject = community;
                    }
                }
                else
                {
                    if (config.canCollectionAdminManageTemplateItem())
                    {
                        adminObject = collection;
                    }
                    else if (config.canCommunityAdminManageCollectionTemplateItem())
                    {
                        adminObject = community;
                    }
                }
                break;
            case WRITE:
                // if it is a template item we need to check the
                // collection/community admin configuration
                if (getOwningCollection() == null)
                {
                    if (config.canCollectionAdminManageTemplateItem())
                    {
                        adminObject = collection;
                    }
                    else if (config.canCommunityAdminManageCollectionTemplateItem())
                    {
                        adminObject = community;
                    }
                }
                else
                {
                    adminObject = this;
                }
                break;
            default:
                adminObject = this;
                break;
            }
        return adminObject;
    }
    
    @Transient
    public IDSpaceObject getParentObject()
    {
        Collection ownCollection = getOwningCollection();
        if (ownCollection != null)
        {
            return ownCollection;
        }
        else
        {
            // is a template item?
        	if (this.getTemplateItemCollections() != null && 
        			!this.getTemplateItemCollections().isEmpty())
        		return this.getTemplateItemCollections().get(0);
            return null;
        }
    }

    @OneToMany(fetch=FetchType.LAZY, mappedBy="")
	public List<Collection> getTemplateItemCollections() {
		return templateItemCollections;
	}

	public void setTemplateItemCollections(List<Collection> templateItemCollections) {
		this.templateItemCollections = templateItemCollections;
	}
	
	@Transient
	public List<MetadataValue> getMetadata (String field) { // dc.title
		return super.metadataDao.selectByResourceAndField(this.getType(), this.getID(), field);
	}
	
	@Transient
	public MetadataValue getFirstMetadata (String field) { // dc.title
		List<MetadataValue> l = super.metadataDao.selectByResourceAndField(this.getType(), this.getID(), field);
		if (l.isEmpty()) return null;
		else return l.get(0);
	}
}
