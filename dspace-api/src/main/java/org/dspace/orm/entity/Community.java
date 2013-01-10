/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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

import org.dspace.services.AuthorizationService;
import org.dspace.services.auth.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Entity
@Table(name = "community")
@Configurable
public class Community extends DSpaceObject implements Serializable {
    private static final long serialVersionUID = 6681512980782299861L;
    
    @Autowired AuthorizationService authService;
    private int id;
    private String name;
    private String shortDescription;
    private String introductoryText;
    private Bitstream logo;
    private String copyrightText;
    private EpersonGroup admin;
    private List<Community> parents;
    private List<Community> childs;
    private List<Collection> collections;
    private List<Item> items;
    private boolean istop;
    private Integer itemCount;

    @Id
    @GeneratedValue
    @Column(name = "community_id", unique = true, nullable = false)
    public int getID() {
        return id;
    }

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "community2community", joinColumns = { @JoinColumn(name = "child_comm_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "parent_comm_id", nullable = false) })
    public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "community2community", joinColumns = { @JoinColumn(name = "child_comm_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "parent_comm_id", nullable = false) })
    public List<Community> getParents() {
        return parents;
    }

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "community2collection", joinColumns = { @JoinColumn(name = "community_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "collection_id", nullable = false) })
    public List<Collection> getCollections() {
        return collections;
    }

    @Transient
    public boolean hasCollections() {
        return (this.getCollections() != null && !this.getCollections()
                .isEmpty());
    }

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "community2community", joinColumns = { @JoinColumn(name = "parent_comm_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "child_comm_id", nullable = false) })
    public List<Community> getChilds() {
        return childs;
    }

    @Transient
    public boolean hasChilds() {
        return (this.getChilds() != null && !this.getChilds().isEmpty());
    }

    @Transient
    public boolean hasParents() {
        return (this.getParents() != null && !this.getParents().isEmpty());
    }

    @Transient
    public Community getParent() {
        if (this.hasParents())
            return this.getParents().get(0);
        else
            return null;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    @Column(name = "short_description", nullable = true)
    public String getShortDescription() {
        return shortDescription;
    }

    @Column(name = "introductory_text", nullable = true)
    public String getIntroductoryText() {
        return introductoryText;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_bitstream_id", nullable = true)
    public Bitstream getLogo() {
        return logo;
    }

    @Column(name = "copyright_text", nullable = true)
    public String getCopyrightText() {
        return copyrightText;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="admin", nullable = false)
    public EpersonGroup getAdmin() {
        return admin;
    }

    @Transient
    public List<Community> getHierarchyList() {
        List<Community> parents = new ArrayList<Community>();
        Community tmp = this;
        while (tmp != null) {
            if (tmp.hasParents()) {
                tmp = tmp.getParent();
                if (tmp != null)
                    parents.add(tmp);
            } else
                tmp = null;
        }
        Collections.reverse(parents);
        return parents;
    }

    public void setChilds(List<Community> comms) {
        this.childs = comms;
    }

    public void setCollections(List<Collection> colls) {
        this.collections = colls;
    }

    public void setParents(List<Community> comms) {
        this.parents = comms;
        this.setTop((comms == null || !comms.isEmpty()));
    }

    public void setID(int communityId) {
        this.id = communityId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public void setIntroductoryText(String introductoryText) {
        this.introductoryText = introductoryText;
    }

    public void setLogo(Bitstream logo) {
        this.logo = logo;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public void setAdmin(EpersonGroup admin) {
        this.admin = admin;
    }

    @Override
    @Transient
    public DSpaceObjectType getType() {
        return DSpaceObjectType.COMMUNITY;
    }

    @Column(name="istop")
	public boolean isTop() {
		return istop;
	}

	public void setTop(boolean istop) {
		this.istop = istop;
	}

    @Column(name = "item_count", nullable = true)
	public Integer getItemCount() {
		return itemCount;
	}

	public void setItemCount(Integer itemCount) {
		this.itemCount = itemCount;
	}

	@Transient
	public IDSpaceObject getAdminObject(Action action)
    {
        IDSpaceObject adminObject = null;
        switch (action)
        {
        case REMOVE:
            if (authService.getConfiguration().canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = this;
            }
            break;

        case DELETE:
            if (authService.getConfiguration().canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = this.getParent();
            }
            break;
        case ADD:
            if (authService.getConfiguration().canCommunityAdminPerformSubelementCreation())
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
        return this.getParent();      
    }
}
