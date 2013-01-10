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
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.orm.dao.api.ICollectionDao;
import org.dspace.services.AuthorizationService;
import org.dspace.services.auth.DSpaceAuthorizeConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Entity
@Table(name = "epersongroup")
@SequenceGenerator(name="epersongroup_gen", sequenceName="epersongroup_seq")
@Configurable
public class EpersonGroup extends DSpaceObject {
	@Autowired AuthorizationService authService;
	@Autowired ICollectionDao collectionDao;
	
    private String name;
    private List<Eperson> epersons;
    private List<WorkSpaceItem> workSpaceItems;
    private List<EpersonGroup> parents;
    private List<EpersonGroup> childs;
    private List<EpersonGroup> allChilds;
    private List<EpersonGroup> allParents;
    private List<Collection> ownerCollections;
    private List<Community> ownedCommunities;
    private List<Collection> submittedCollections;
    private List<Collection> workflowstep1Collections;
    private List<Collection> workflowstep2Collections;
    private List<Collection> workflowstep3Collections;
    
    @Id
    @Column(name = "eperson_group_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="epersongroup_gen")
    public int getID() {
        return id;
    }
    
    @Column(name = "name", nullable = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "group2group", joinColumns = { @JoinColumn(name = "parent_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "child_id", nullable = false) })
    public List<EpersonGroup> getChilds() {
        return childs;
    }
    
    public void setChilds(List<EpersonGroup> epersonGroups) {
        this.childs = epersonGroups;
    }
    

    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "group2groupcache", joinColumns = { @JoinColumn(name = "parent_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "child_id", nullable = false) })
    public List<EpersonGroup> getAllChilds() {
        return allChilds;
    }
    
    public void setAllChilds(List<EpersonGroup> epersonGroups) {
        this.allChilds = epersonGroups;
    }
    

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "group2group", joinColumns = { @JoinColumn(name = "child_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "parent_id", nullable = false) })
    public List<EpersonGroup> getParents() {
        return parents;
    }
    
    public void setParents(List<EpersonGroup> epersonGroups) {
        this.parents = epersonGroups;
    }
    

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "group2groupcache", joinColumns = { @JoinColumn(name = "child_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "parent_id", nullable = false) })
    public List<EpersonGroup> getAllParents() {
        return allParents;
    }
    
    public void setAllParents(List<EpersonGroup> epersonGroups) {
        this.allParents = epersonGroups;
    }
    
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "epersongroup2eperson", joinColumns = { @JoinColumn(name = "eperson_group_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "eperson_id", nullable = false) })
    public List<Eperson> getEpersons() {
        return epersons;
    }
    
    public void setEpersons(List<Eperson> epersons) {
        this.epersons = epersons;
    }
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "epersongroup2workspaceitem", joinColumns = { @JoinColumn(name = "eperson_group_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "workspace_item_id", nullable = false) })
	public List<WorkSpaceItem> getWorkSpaceItems() {
		return workSpaceItems;
	}

	public void setWorkSpaceItems(List<WorkSpaceItem> workSpaceItems) {
		this.workSpaceItems = workSpaceItems;
	}

	@Override
	@Transient
	public DSpaceObjectType getType() {
		return DSpaceObjectType.GROUP;
	}
	
	@Transient
	public IDSpaceObject getParentObject()
    {
		DSpaceAuthorizeConfiguration config = authService.getConfiguration();
        // could a collection/community administrator manage related groups?
        // check before the configuration options could give a performance gain
        // if all group management are disallowed
        if (config.canCollectionAdminManageAdminGroup()
                || config.canCollectionAdminManageSubmitters()
                || config.canCollectionAdminManageWorkflows()
                || config.canCommunityAdminManageAdminGroup()
                || config
                        .canCommunityAdminManageCollectionAdminGroup()
                || config
                        .canCommunityAdminManageCollectionSubmitters()
                || config
                        .canCommunityAdminManageCollectionWorkflows())
        {
            // is this a collection related group?
        	Collection c = null;

        	if (this.getWorkflowstep1Collections() != null && !this.getWorkflowstep1Collections().isEmpty())
        		c = this.getWorkflowstep1Collections().get(0);
        	
        	if (c == null && this.getWorkflowstep2Collections() != null && !this.getWorkflowstep2Collections().isEmpty())
        		c = this.getWorkflowstep2Collections().get(0);
        	
        	if (c == null && this.getWorkflowstep3Collections() != null && !this.getWorkflowstep3Collections().isEmpty())
        		c = this.getWorkflowstep3Collections().get(0);
        	
        	if (c != null && config.canCollectionAdminManageWorkflows()) 
        		return c;
        	else if (c != null && config.canCommunityAdminManageCollectionWorkflows()) 
        		return c.getParentObject();
        	
        	c = null;
        	if (this.getSubmittedCollections() != null && !this.getSubmittedCollections().isEmpty())
    			c = this.getSubmittedCollections().get(0);
        	if (c != null && config.canCollectionAdminManageSubmitters())
        		return c;
        	else if (c != null && config.canCommunityAdminManageCollectionSubmitters())
        		return c.getParentObject();
        	
        	c = null;
        	if (this.getOwnedCollections() != null && !this.getOwnedCollections().isEmpty())
    			c = this.getOwnedCollections().get(0);
        	if (c != null && config.canCollectionAdminManageAdminGroup())
        		return c;
        	else if (c != null && config.canCommunityAdminManageCollectionAdminGroup())
        		return c.getParentObject();
        	
        	Community com = null;
        	if (this.getOwnedCommunities() != null && !this.getOwnedCommunities().isEmpty())
        		com = this.getOwnedCommunities().get(0);
        	
        	if (com != null && config.canCommunityAdminManageAdminGroup())
        		return com;
        }
        return null;
    }

	@OneToMany(fetch=FetchType.LAZY, mappedBy="admin")
	public List<Collection> getOwnedCollections() {
		return ownerCollections;
	}

	public void setOwnedCollections(List<Collection> ownerCollections) {
		this.ownerCollections = ownerCollections;
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="workflowStep1")
	public List<Collection> getWorkflowstep1Collections() {
		return workflowstep1Collections;
	}

	public void setWorkflowstep1Collections(List<Collection> workflowstep1Collections) {
		this.workflowstep1Collections = workflowstep1Collections;
	}
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="submitter")
	public List<Collection> getSubmittedCollections() {
		return submittedCollections;
	}

	public void setSubmittedCollections(List<Collection> submittedCollections) {
		this.submittedCollections = submittedCollections;
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="workflowStep2")
	public List<Collection> getWorkflowstep2Collections() {
		return workflowstep2Collections;
	}

	public void setWorkflowstep2Collections(List<Collection> workflowstep2Collections) {
		this.workflowstep2Collections = workflowstep2Collections;
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="workflowStep3")
	public List<Collection> getWorkflowstep3Collections() {
		return workflowstep3Collections;
	}

	public void setWorkflowstep3Collections(List<Collection> workflowstep3Collections) {
		this.workflowstep3Collections = workflowstep3Collections;
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="admin")
	public List<Community> getOwnedCommunities() {
		return ownedCommunities;
	}

	public void setOwnedCommunities(List<Community> ownedCommunities) {
		this.ownedCommunities = ownedCommunities;
	}
}
