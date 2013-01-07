/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

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
@Table(name = "collection")
@Configurable
public class Collection extends DSpaceObject {
	@Autowired AuthorizationService authService;
	
    private int id;
    private String name;
    private String shortDescription;
    private String introductoryText;
    private Bitstream logo;
    private Item templateItem;
    private String provenanceDescription;
    private String license;
    private String copyrightText;
    private String sideBarText;
    private EpersonGroup workflowStep1;
    private EpersonGroup workflowStep2;
    private EpersonGroup workflowStep3;
    private EpersonGroup submitter;
    private EpersonGroup admin;
    private List<Community> parents;
    private List<Eperson> epersons;
    private Integer itemCount;
    private List<Item> items;

    @Id
    @Column(name = "collection_id")
    @GeneratedValue
    public int getID() {
        return id;
    }

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "community2collection", joinColumns = { @JoinColumn(name = "collection_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "community_id", nullable = false) })
    public List<Community> getParents() {
        return parents;
    }
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "collection2item", joinColumns = { @JoinColumn(name = "collection_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "item_id", nullable = false) })
    public List<Item> getItems() {
        return items;
    }
   
    public void setItems(List<Item> items) {
        this.items = items;
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

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "template_item_id", nullable = true)
    public Item getTemplateItem() {
        return templateItem;
    }

    @Column(name = "provenance_description", nullable = true)
    public String getProvenanceDescription() {
        return provenanceDescription;
    }

    @Column(name = "license", nullable = true)
    public String getLicense() {
        return license;
    }

    @Column(name = "copyright_text", nullable = true)
    public String getCopyrightText() {
        return copyrightText;
    }

    @Column(name = "side_bar_text", nullable = true)
    public String getSideBarText() {
        return sideBarText;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "workflow_step_1", nullable = true)
    public EpersonGroup getWorkflowStep1() {
        return workflowStep1;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "workflow_step_2", nullable = true)
    public EpersonGroup getWorkflowStep2() {
        return workflowStep2;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "workflow_step_3", nullable = true)
    public EpersonGroup getWorkflowStep3() {
        return workflowStep3;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "submitter", nullable = true)
    public EpersonGroup getSubmitter() {
        return submitter;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "admin", nullable = true)
    public EpersonGroup getAdmin() {
        return admin;
    }

    @Transient
    public List<Community> getHierarchyList() {
        List<Community> parents = new ArrayList<Community>();
        Community tmp = this.getParent();
        while (tmp != null) {
            parents.add(tmp);
            if (tmp.hasParents()) {
                tmp = tmp.getParent();
            } else
                tmp = null;
        }
        Collections.reverse(parents);
        return parents;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setParents(List<Community> coms) {
        this.parents = coms;
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

    public void setTemplateItem(Item templateItem) {
        this.templateItem = templateItem;
    }

    public void setProvenanceDescription(String provenanceDescription) {
        this.provenanceDescription = provenanceDescription;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public void setSideBarText(String sideBarText) {
        this.sideBarText = sideBarText;
    }

    public void setWorkflowStep1(EpersonGroup workflowStep1) {
        this.workflowStep1 = workflowStep1;
    }

    public void setWorkflowStep2(EpersonGroup workflowStep2) {
        this.workflowStep2 = workflowStep2;
    }

    public void setWorkflowStep3(EpersonGroup workflowStep3) {
        this.workflowStep3 = workflowStep3;
    }

    public void setSubmitter(EpersonGroup submitter) {
        this.submitter = submitter;
    }

    public void setAdmin(EpersonGroup admin) {
        this.admin = admin;
    }

    @Override
    @Transient
    public DSpaceObjectType getType() {
        return DSpaceObjectType.COLLECTION;
    }
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "subscription", joinColumns = { @JoinColumn(name = "eperson_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "collection_id", nullable = false) })
    public List<Eperson> getEpersons() {
        return epersons;
    }
    
    public void setEpersons(List<Eperson> epersons) {
        this.epersons = epersons;
    }

    @Column(name = "item_count", nullable = true)
   	public Integer getItemCount() {
   		return itemCount;
   	}

   	public void setItemCount(Integer itemCount) {
   		this.itemCount = itemCount;
   	}
   	

    public IDSpaceObject getAdminObject(Action action)
    {
        DSpaceObject adminObject = null;
        Community community = null;
        List<Community> communities = this.getParents();
        if (communities != null && !communities.isEmpty())
        {
            community = communities.get(0);
        }

        switch (action)
        {
        case REMOVE:
            if (authService.getConfiguration().canCollectionAdminPerformItemDeletion())
            {
                adminObject = this;
            }
            else if (authService.getConfiguration().canCommunityAdminPerformItemDeletion())
            {
                adminObject = community;
            }
            break;

        case DELETE:
            if (authService.getConfiguration().canCommunityAdminPerformSubelementDeletion())
            {
                adminObject = community;
            }
            break;
        default:
            adminObject = this;
            break;
        }
        return adminObject;
    }
    
    @Override
    public IDSpaceObject getParentObject()
    {
        List<Community> communities = this.getParents();
        if (communities != null && (!communities.isEmpty() && communities.get(0) != null))
        {
            return communities.get(0);
        }
        else
        {
            return null;
        }
    }

    
}
