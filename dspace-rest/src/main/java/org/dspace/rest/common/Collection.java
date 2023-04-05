/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import static org.dspace.content.service.DSpaceObjectService.MD_COPYRIGHT_TEXT;
import static org.dspace.content.service.DSpaceObjectService.MD_INTRODUCTORY_TEXT;
import static org.dspace.content.service.DSpaceObjectService.MD_SHORT_DESCRIPTION;
import static org.dspace.content.service.DSpaceObjectService.MD_SIDEBAR_TEXT;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 5/22/13
 * Time: 9:41 AM
 */
@XmlRootElement(name = "collection")
public class Collection extends DSpaceObject {
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    Logger log = org.apache.logging.log4j.LogManager.getLogger(Collection.class);

    //Relationships
    private Bitstream logo;
    private Community parentCommunity;
    private List<Community> parentCommunityList = new ArrayList<>();

    private List<Item> items = new ArrayList<>();

    //Collection-Metadata
    private String license;
    private String copyrightText;
    private String introductoryText;
    private String shortDescription;
    private String sidebarText;

    //Calculated
    private Integer numberItems;

    public Collection() {
    }

    public Collection(org.dspace.content.Collection collection, ServletContext servletContext, String expand,
                      Context context, Integer limit, Integer offset)
        throws SQLException, WebApplicationException {
        super(collection, servletContext);
        setup(collection, servletContext, expand, context, limit, offset);
    }

    private void setup(org.dspace.content.Collection collection, ServletContext servletContext, String expand,
                       Context context, Integer limit, Integer offset)
        throws SQLException {
        List<String> expandFields = new ArrayList<>();
        if (expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        this.setCopyrightText(collectionService.getMetadataFirstValue(collection,
                MD_COPYRIGHT_TEXT, org.dspace.content.Item.ANY));
        this.setIntroductoryText(collectionService.getMetadataFirstValue(collection,
                MD_INTRODUCTORY_TEXT, org.dspace.content.Item.ANY));
        this.setShortDescription(collectionService.getMetadataFirstValue(collection,
                MD_SHORT_DESCRIPTION, org.dspace.content.Item.ANY));
        this.setSidebarText(collectionService.getMetadataFirstValue(collection,
                MD_SIDEBAR_TEXT, org.dspace.content.Item.ANY));

        if (expandFields.contains("parentCommunityList") || expandFields.contains("all")) {
            List<org.dspace.content.Community> parentCommunities = communityService.getAllParents(context, collection);
            for (org.dspace.content.Community parentCommunity : parentCommunities) {
                this.addParentCommunityList(new Community(parentCommunity, servletContext, null, context));
            }
        } else {
            this.addExpand("parentCommunityList");
        }

        if (expandFields.contains("parentCommunity") | expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity =
                (org.dspace.content.Community) collectionService
                    .getParentObject(context, collection);
            this.setParentCommunity(new Community(
                parentCommunity, servletContext, null, context));
        } else {
            this.addExpand("parentCommunity");
        }

        //TODO: Item paging. limit, offset/page
        if (expandFields.contains("items") || expandFields.contains("all")) {
            Iterator<org.dspace.content.Item> childItems =
                itemService.findByCollection(context, collection, limit, offset);

            items = new ArrayList<>();
            while (childItems.hasNext()) {
                org.dspace.content.Item item = childItems.next();

                if (itemService.isItemListedForUser(context, item)) {
                    items.add(new Item(item, servletContext, null, context));
                }
            }
        } else {
            this.addExpand("items");
        }

        if (expandFields.contains("license") || expandFields.contains("all")) {
            setLicense(collectionService.getLicense(collection));
        } else {
            this.addExpand("license");
        }

        if (expandFields.contains("logo") || expandFields.contains("all")) {
            if (collection.getLogo() != null) {
                this.logo = new Bitstream(collection.getLogo(), servletContext, null, context);
            }
        } else {
            this.addExpand("logo");
        }

        if (!expandFields.contains("all")) {
            this.addExpand("all");
        }

        this.setNumberItems(itemService.countItems(context, collection));
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

    public String getCopyrightText() {
        return copyrightText;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public String getIntroductoryText() {
        return introductoryText;
    }

    public void setIntroductoryText(String introductoryText) {
        this.introductoryText = introductoryText;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getSidebarText() {
        return sidebarText;
    }

    public void setSidebarText(String sidebarText) {
        this.sidebarText = sidebarText;
    }
}
