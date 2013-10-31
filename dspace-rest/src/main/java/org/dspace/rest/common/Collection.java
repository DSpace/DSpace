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

    //Relationships
    private Bitstream logo;
    private Community parentCommunity;
    private List<Community> parentCommunityList = new ArrayList<Community>();

    private List<Item> items = new ArrayList<Item>();

    //Collection-Metadata
    private String license;
    //String provenance_description;
    //String short_description;
    //String introductory_text;
    //String copyright_text;
    //String side_bar_text;

    //Calculated
    private Integer numberItems;

    public Collection(){}

    public Collection(org.dspace.content.Collection collection, String expand, Context context, Integer limit, Integer offset) throws SQLException, WebApplicationException{
        super(collection);
        setup(collection, expand, context, limit, offset);
    }

    private void setup(org.dspace.content.Collection collection, String expand, Context context, Integer limit, Integer offset) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        if(expandFields.contains("parentCommunityList") || expandFields.contains("all")) {
            org.dspace.content.Community[] parentCommunities = collection.getCommunities();
            for(org.dspace.content.Community parentCommunity : parentCommunities) {
                this.addParentCommunityList(new Community(parentCommunity, null, context));
            }
        } else {
            this.addExpand("parentCommunityList");
        }

        if(expandFields.contains("parentCommunity") | expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity = (org.dspace.content.Community) collection.getParentObject();
            this.setParentCommunity(new Community(parentCommunity, null, context));
        } else {
            this.addExpand("parentCommunity");
        }

        //TODO: Item paging. limit, offset/page
        if(expandFields.contains("items") || expandFields.contains("all")) {
            ItemIterator childItems;
            if(limit != null && limit >= 0 && offset != null && offset >= 0) {
                childItems = collection.getItems(limit, offset);
            } else {
                childItems = collection.getItems();
            }

            items = new ArrayList<Item>();
            while(childItems.hasNext()) {
                org.dspace.content.Item item = childItems.next();
                if(AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
                    items.add(new Item(item, null, context));
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

        if(expandFields.contains("logo") || expandFields.contains("all")) {
            if(collection.getLogo() != null) {
                this.logo = new Bitstream(collection.getLogo(), null);
            }
        }

        if(!expandFields.contains("all")) {
            this.addExpand("all");
        }

        this.setNumberItems(collection.countItems());
    }

    public Bitstream getLogo() {
        return logo;
    }

    public Integer getNumberItems() {
        return numberItems;
    }

    public void setNumberItems(Integer numberItems) {
        this.numberItems = numberItems;
    }

    public Community getParentCommunity() {
        return parentCommunity;
    }

    public void setParentCommunity(Community parentCommunity) {
        this.parentCommunity = parentCommunity;
    }

    public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public void setParentCommunityList(List<Community> parentCommunityList) {
		this.parentCommunityList = parentCommunityList;
	}

	public List<Community> getParentCommunityList() {
        return parentCommunityList;
    }

    public void addParentCommunityList(Community parentCommunity) {
        this.parentCommunityList.add(parentCommunity);
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }
}
