package org.dspace.rest.common;

import org.dspace.content.Site;
import org.dspace.core.Context;

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

    //Internal value
    private Integer communityID;

    private static final String type = "community";

    //Exandable relationships
    private Integer parentCommunityID;
    private List<Integer> childCollectionIDList = new ArrayList<Integer>();
    private List<Integer> childCommunityIDList = new ArrayList<Integer>();


    private List<String> expand = new ArrayList<String>();

    //Metadata
    private String name;
    private String handle;

    private String copyrightText, introductoryText, shortDescription, sidebarText;
    private Integer countItems;

    //@XmlElement(name = "communities")
    private List<Community> subCommunities = new ArrayList<Community>();

    //@XmlElement(name = "collections")
    private List<Collection> collections = new ArrayList<Collection>();



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
            //TODO Handle exceptions
            //throw e;

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

            org.dspace.content.Collection[] collectionArray = community.getCollections();
            for(org.dspace.content.Collection collection : collectionArray) {
                collections.add(new Collection(collection, expand));
            }

            org.dspace.content.Community[] communityArray = community.getSubcommunities();
            for(org.dspace.content.Community subCommunity : communityArray) {
                subCommunities.add(new Community(subCommunity, expand));
            }

            if(expandFields.contains("parentCommunityID")) {
                org.dspace.content.Community parentCommunity = community.getParentCommunity();
                if(parentCommunity != null) {
                    this.setParentCommunityID(parentCommunity.getID());
                } else {
                    this.setParentCommunityID(Site.SITE_ID);
                }

            } else {
                this.addExpand("parentCommunityID");
            }

            if(expandFields.contains("subCollections")) {
                org.dspace.content.Collection[] collections = community.getCollections();
                this.childCollectionIDList = new ArrayList<Integer>();
                for(org.dspace.content.Collection collection : collections) {
                    this.childCollectionIDList.add(collection.getID());
                }
            } else {
                this.addExpand("subCollections");
            }

            if(expandFields.contains("subCommunities")) {
                org.dspace.content.Community[] subCommunities = community.getSubcommunities();
                this.childCommunityIDList = new ArrayList<Integer>();
                for(org.dspace.content.Community subComm : subCommunities) {
                    this.childCommunityIDList.add(subComm.getID());
                }
            } else {
                this.addExpand("subCommunities");
            }

            //collection.getMetadata()
        } catch (Exception e) {

        }

    }


    public Integer getParentCommunityID() {
        return parentCommunityID;
    }

    public void setParentCommunityID(Integer parentCommunityID) {
        this.parentCommunityID = parentCommunityID;
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

    public List<Integer> getChildCollectionIDList() {
        return childCollectionIDList;
    }

    public void setChildCollectionIDList(List<Integer> childCollectionIDList) {
        this.childCollectionIDList = childCollectionIDList;
    }

    public List<Integer> getChildCommunityIDList() {
        return childCommunityIDList;
    }

    public void setChildCommunityIDList(List<Integer> childCommunityIDList) {
        this.childCommunityIDList = childCommunityIDList;
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

    public static String getType() {
        return type;
    }

    public List<Collection> getCollections() {
        return collections;
    }

    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }

    public List<Community> getSubCommunities() {
        return subCommunities;
    }

    public void setSubCommunities(List<Community> subCommunities) {
        this.subCommunities = subCommunities;
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
}
