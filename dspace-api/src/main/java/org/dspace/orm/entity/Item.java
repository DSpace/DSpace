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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.dspace.core.Constants;

@Entity
@Table(name = "item")
public class Item implements IDSpaceObject {
    private int id;
    private Eperson submitter;
    private boolean inArchive;
    private boolean withDrawn;
    private boolean discoverable;
    private Date lastModified;
    private Collection owningCollection;
    private List<Bundle> bundles;
    List<Collection> collections;
    
    @Id
    @Column(name = "item_id")
    @GeneratedValue
    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
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
	public int getType() {
		return Constants.ITEM;
	}
}
