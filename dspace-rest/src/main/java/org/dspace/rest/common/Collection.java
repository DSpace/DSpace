package org.dspace.rest.common;

import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
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
@XmlRootElement
public class Collection {
    //Internal value
    private Integer collectionID;

    //Relationships to other objects
    private Integer logoID;

    //Exandable relationships
    private Integer parentCommunityID;
    private List<Integer> parentCommunityIDList = new ArrayList<Integer>();
    private List<Integer> itemIDList = new ArrayList<Integer>();


    private List<String> expand = new ArrayList<String>();

    //Internal metadata
    private String name;
    private String handle;
    private String license;

    //Calculated
    private Integer numberItems;

    private static Context context;

    public Collection(){}

    public Collection(org.dspace.content.Collection collection, String expand) {
       setup(collection, expand);
    }

    public Collection(Integer collectionID, String expand) {
        try {
            if(context == null || !context.isValid() ) {
                context = new Context();
            }

            org.dspace.content.Collection collection = org.dspace.content.Collection.find(context, collectionID);
            setup(collection, expand);

        } catch (Exception e) {
            this.setName(e.getMessage());

        }
    }

    private void setup(org.dspace.content.Collection collection, String expand) {
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }


        try {
            this.setCollectionID(collection.getID());
            this.setName(collection.getName());
            this.setHandle(collection.getHandle());

            if(expandFields.contains("parentCommunityIDList")) {
                Community[] parentCommunities = collection.getCommunities();
                for(Community parentCommunity : parentCommunities) {
                    this.addParentCommunityIDList(parentCommunity.getID());
                }
            } else {
                this.addExpand("parentCommunityIDList");
            }

            if(expandFields.contains("parentCommunityID")) {
                Community parentCommunity = (Community) collection.getParentObject();
                this.setParentCommunityID(parentCommunity.getID());
            } else {
                this.addExpand("parentCommunityID");
            }

            if(expandFields.contains("itemIDList")) {
                ItemIterator childItems = collection.getItems();
                while(childItems.hasNext()) {
                    Item item = childItems.next();
                    this.addItemIDToList(item.getID());
                }
            } else {
                this.addExpand("itemIDList");
            }

            if(collection.getLogo() != null) {
                this.setLogoID(collection.getLogo().getID());
            }

            this.setLicense(collection.getLicense());
            this.setNumberItems(collection.countItems());
            //collection.getMetadata()
        } catch (Exception e) {

        }

    }

    //metadata                                                                         s
    //String provenance_description;
    //String short_description;
    //String introductory_text;
    //String copyright_text;
    //String side_bar_text;

    public Integer getCollectionID() {
        return collectionID;
    }

    public void setCollectionID(Integer id) {
        this.collectionID = id;
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

    public Integer getLogoID() {
        return logoID;
    }

    public void setLogoID(Integer logoID) {
        this.logoID = logoID;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public Integer getNumberItems() {
        return numberItems;
    }

    public void setNumberItems(Integer numberItems) {
        this.numberItems = numberItems;
    }

    public Integer getParentCommunityID() {
        return parentCommunityID;
    }

    public void setParentCommunityID(Integer parentCommunityID) {
        this.parentCommunityID = parentCommunityID;
    }

    public List<Integer> getParentCommunityIDList() {
        return parentCommunityIDList;
    }

    public void setParentCommunityIDList(List<Integer> parentCommunityIDList) {
        this.parentCommunityIDList = parentCommunityIDList;
    }

    public void addParentCommunityIDList(Integer communityParentID) {
        this.parentCommunityIDList.add(communityParentID);
    }


    public List<Integer> getItemIDList() {
        return itemIDList;
    }

    public void setItemIDList(List<Integer> itemIDList) {
        this.itemIDList = itemIDList;
    }

    public void addItemIDToList(Integer itemID) {
        this.itemIDList.add(itemID);
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
}
