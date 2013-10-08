/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
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
@XmlRootElement(name = "collection")
public class Collection extends DSpaceObject {
    Logger log = Logger.getLogger(Collection.class);

    //Relationships to other objects
    private Integer logoID;

    //Exandable relationships
    private Integer parentCommunityID;
    private List<Integer> parentCommunityIDList = new ArrayList<Integer>();
    private List<Integer> itemIDList = new ArrayList<Integer>();

    @XmlElement(name = "items")
    private List<DSpaceObject> items = new ArrayList<DSpaceObject>();

    private String license;

    //unused-metadata
    //String provenance_description;
    //String short_description;
    //String introductory_text;
    //String copyright_text;
    //String side_bar_text;

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    //Calculated
    private Integer numberItems;

    public Collection(){}

    public Collection(org.dspace.content.Collection collection, String expand, Context context) throws SQLException, WebApplicationException{
        super(collection);
        setup(collection, expand, context);
    }

    private void setup(org.dspace.content.Collection collection, String expand, Context context) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        if(expandFields.contains("parentCommunityIDList") || expandFields.contains("all")) {
            org.dspace.content.Community[] parentCommunities = collection.getCommunities();
            for(org.dspace.content.Community parentCommunity : parentCommunities) {
                this.addParentCommunityIDList(parentCommunity.getID());
            }
        } else {
            this.addExpand("parentCommunityIDList");
        }

        if(expandFields.contains("parentCommunityID") | expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity = (org.dspace.content.Community) collection.getParentObject();
            this.setParentCommunityID(parentCommunity.getID());
        } else {
            this.addExpand("parentCommunityID");
        }

        //TODO: Item paging. limit, offset/page
        if(expandFields.contains("items") || expandFields.contains("all")) {
            ItemIterator childItems = collection.getItems();
            items = new ArrayList<DSpaceObject>();
            while(childItems.hasNext()) {
                org.dspace.content.Item item = childItems.next();
                if(AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
                    items.add(new DSpaceObject(item));
                }
            }
        } else {
            this.addExpand("items");
        }

        if(expandFields.contains("license") || expandFields.contains("all")) {
            setLicense(collection.getLicense());
        } else {
            this.addExpand("license");
        }

        if(!expandFields.contains("all")) {
            this.addExpand("all");
        }

        if(collection.getLogo() != null) {
            this.setLogoID(collection.getLogo().getID());
        }

        this.setNumberItems(collection.countItems());
    }

    public Integer getLogoID() {
        return logoID;
    }

    public void setLogoID(Integer logoID) {
        this.logoID = logoID;
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
}
