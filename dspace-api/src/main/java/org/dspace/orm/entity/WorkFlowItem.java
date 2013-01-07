/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

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
import javax.persistence.Transient;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */

@Entity
@Table(name = "workflowitem")
public class WorkFlowItem extends DSpaceObject {
    private int id;
    private Item item;
    private Collection collection;
    private Integer state;
    private Eperson owner;
    private boolean multipleTitles;
    private boolean publishedBefore;
    private boolean multipleFiles;
    private List<Eperson> epersons;
    
    @Id
    @Column(name = "workflow_id")
    @GeneratedValue
    public int getID() {
        return id;
    }
    
    public int setID(int id) {
        return this.id= id;
    }
    
    @Override
    @Transient
    public DSpaceObjectType getType()
    {
    	return DSpaceObjectType.WORKFLOW_ITEM;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = true)
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = true)
	public Collection getCollection() {
		return collection;
	}
	

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner", nullable = true)
	public Eperson getOwner() {
		return owner;
	}

	public void setOwner(Eperson owner) {
		this.owner = owner;
	}

	@Column(name = "state", nullable = true)
	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}

	@Column(name = "published_before", nullable = true)
	public boolean isPublishedBefore() {
		return publishedBefore;
	}

	public void setPublishedBefore(boolean publishedBefore) {
		this.publishedBefore = publishedBefore;
	}

	@Column(name = "multiple_titles", nullable = true)
	public boolean isMultipleTitles() {
		return multipleTitles;
	}

	public void setMultipleTitles(boolean multipleTitles) {
		this.multipleTitles = multipleTitles;
	}

	@Column(name = "multiple_files", nullable = true)
	public boolean isMultipleFiles() {
		return multipleFiles;
	}

	public void setMultipleFiles(boolean multipleFiles) {
		this.multipleFiles = multipleFiles;
	}

	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "tasklistitem", joinColumns = { @JoinColumn(name = "workflow_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "eperson_id", nullable = false) })
	public List<Eperson> getEpersons() {
		return epersons;
	}

	public void setEpersons(List<Eperson> epersons) {
		this.epersons = epersons;
	}

	

}
