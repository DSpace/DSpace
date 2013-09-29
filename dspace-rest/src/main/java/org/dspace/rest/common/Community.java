package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 5/22/13
 * Time: 9:41 AM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "community")
public class Community {
    private static Logger log = Logger.getLogger(Community.class);

    //Internal value
    private Integer communityID;

    @XmlElement(name = "type", required = true)
    final String type = "community";

    //Exandable relationships
    @XmlElement(name = "parentCommunity")
    private LiteCommunity parentCommunity;


    private List<String> expand = new ArrayList<String>();

    //Metadata
    private String name;
    private String handle;

    private String copyrightText, introductoryText, shortDescription, sidebarText;
    private Integer countItems;

    @XmlElement(name = "subcommunities", required = true)
    private List<LiteCommunity> subCommunities = new ArrayList<LiteCommunity>();

    @XmlElement(name = "collections")
    private List<LiteCollection> collections = new ArrayList<LiteCollection>();



    private static Context context;

    public Community(){}

    public Community(org.dspace.content.Community community, String expand) {
        setup(community, expand);
    }

    public Community(Integer communityID, String expand) {
        try {
            if(context == null || !context.isValid() ) {
                context = new Context();
            }

            org.dspace.content.Community community = org.dspace.content.Community.find(context, communityID);
            setup(community, expand);

        } catch (Exception e) {
            log.error(e.getMessage());

        }
    }

    private void setup(org.dspace.content.Community community, String expand) {
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        try {
            this.setCommunityID(community.getID());
            this.setName(community.getName());
            this.setHandle(community.getHandle());
            this.setCopyrightText(community.getMetadata("copyrightText"));
            this.setIntroductoryText(community.getMetadata("shortDescription"));
            this.setSidebarText(community.getMetadata("sidebarText"));
            this.setCountItems(community.countItems());

            if(expandFields.contains("parentCommunityID") || expandFields.contains("all")) {
                org.dspace.content.Community parentCommunity = community.getParentCommunity();
                if(parentCommunity != null) {
                    setParentCommunity(new LiteCommunity(parentCommunity));
                }
            } else {
                this.addExpand("parentCommunityID");
            }

            if(expandFields.contains("subCollections") || expandFields.contains("all")) {
                org.dspace.content.Collection[] collectionArray = community.getCollections();
                collections = new ArrayList<LiteCollection>();
                for(org.dspace.content.Collection collection : collectionArray) {
                    collections.add(new LiteCollection(collection));
                }
            } else {
                this.addExpand("subCollections");
            }

            if(expandFields.contains("subCommunities") || expandFields.contains("all")) {
                org.dspace.content.Community[] communityArray = community.getSubcommunities();
                subCommunities = new ArrayList<LiteCommunity>();
                for(org.dspace.content.Community subCommunity : communityArray) {
                    subCommunities.add(new LiteCommunity(subCommunity));
                }
            } else {
                this.addExpand("subCommunities");
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    public List<String> getExpand() {
        return expand;
    }

    public void setExpand(List<String> expand) {
        this.expand = expand;
    }

    public void addExpand(String expandableAttribute) {
        this.expand.add(expandableAttribute);
    }

    public Integer getCommunityID() {
        return communityID;
    }

    public void setCommunityID(Integer communityID) {
        this.communityID = communityID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public List<LiteCollection> getCollections() {
        return collections;
    }

    public void setCollections(List<LiteCollection> collections) {
        this.collections = collections;
    }

    public Integer getCountItems() {
        return countItems;
    }

    public void setCountItems(Integer countItems) {
        this.countItems = countItems;
    }

    public String getSidebarText() {
        return sidebarText;
    }

    public void setSidebarText(String sidebarText) {
        this.sidebarText = sidebarText;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getIntroductoryText() {
        return introductoryText;
    }

    public void setIntroductoryText(String introductoryText) {
        this.introductoryText = introductoryText;
    }

    public String getCopyrightText() {
        return copyrightText;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public String getType() {
        return type;
    }

    public LiteCommunity getParentCommunity() {
        return parentCommunity;
    }

    public void setParentCommunity(LiteCommunity parentCommunity) {
        this.parentCommunity = parentCommunity;
    }
}
