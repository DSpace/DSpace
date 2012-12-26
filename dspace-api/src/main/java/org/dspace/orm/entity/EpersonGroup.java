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
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.core.Constants;

@Entity
@Table(name = "epersongroup")
public class EpersonGroup extends DSpaceObject {
    private int id;
    private String name;
    private List<Eperson> epersons;
    private List<WorkSpaceItem> workSpaceItems;

    private List<EpersonGroup> epersonGroups;
    
    @Id
    @Column(name = "eperson_group_id")
    @GeneratedValue
    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
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
    public List<EpersonGroup> getGroups() {
        return epersonGroups;
    }
    
    public void setGroups(List<EpersonGroup> epersonGroups) {
        this.epersonGroups = epersonGroups;
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
	public int getType() {
		return Constants.EPERSONGROUP;
	}
}
