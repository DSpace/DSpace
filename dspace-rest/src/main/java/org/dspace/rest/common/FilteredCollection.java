/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.rest.filter.ItemFilterSet;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Retrieve items within a collection that match a specific set of Item Filters of interest
 * 
 * @author Terry Brady, Georgetown University
 */
@XmlRootElement(name = "filtered-collection")
public class FilteredCollection extends DSpaceObject {
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    Logger log = Logger.getLogger(FilteredCollection.class);

    //Relationships
    private Community parentCommunity;
    private Community topCommunity;
    private List<Community> parentCommunityList = new ArrayList<Community>();

    private List<Item> items = new ArrayList<Item>();
    
    private List<ItemFilter> itemFilters = new ArrayList<ItemFilter>();
    
    //Calculated
    private Integer numberItems;
    private Integer numberItemsProcessed;

    public FilteredCollection(){}

    /**
     * Evaluate a collection against of set of Item Filters
     * @param collection
     *     DSpace Collection to evaluate
     * @param filters
     *     String representing a list of filters
     * @param expand
     * @param context
     * @param limit
     * @param offset
     * @throws SQLException
     * @throws WebApplicationException
     */
    public FilteredCollection(org.dspace.content.Collection collection, ServletContext servletContext, String filters, String expand, Context context, Integer limit, Integer offset) throws SQLException, WebApplicationException{
        super(collection, servletContext);
        setup(collection, servletContext, expand, context, limit, offset, filters);
    }

    private void setup(org.dspace.content.Collection collection, ServletContext servletContext, String expand, Context context, Integer limit, Integer offset, String filters) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        if(expandFields.contains("parentCommunityList") || expandFields.contains("all")) {
            List<org.dspace.content.Community> parentCommunities = communityService.getAllParents(context, collection);
            List<Community> parentCommunityList = new ArrayList<Community>();
            for(org.dspace.content.Community parentCommunity : parentCommunities) {
                parentCommunityList.add(new Community(parentCommunity, servletContext, null, context));
            }
            this.setParentCommunityList(parentCommunityList);
        } else {
            this.addExpand("parentCommunityList");
        }

        if(expandFields.contains("parentCommunity") | expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity = collection.getCommunities().get(0);
            this.setParentCommunity(new Community(parentCommunity, servletContext, null, context));
        } else {
            this.addExpand("parentCommunity");
        }

        if(expandFields.contains("topCommunity") | expandFields.contains("all")) {
            List<org.dspace.content.Community> parentCommunities = communityService.getAllParents(context, collection);
            if (parentCommunities.size() > 0) {
                org.dspace.content.Community topCommunity = parentCommunities.get(parentCommunities.size()-1);
                this.setTopCommunity(new Community(topCommunity, servletContext, null, context));
            }
        } else {
            this.addExpand("topCommunity");
        }

        
        boolean reportItems = expandFields.contains("items") || expandFields.contains("all");
        ItemFilterSet itemFilterSet = new ItemFilterSet(filters, reportItems);
        this.setItemFilters(itemFilterSet.getItemFilters());
        
        this.setNumberItemsProcessed(0);
        if (itemFilters.size() > 0) {
        	Iterator<org.dspace.content.Item> childItems = itemService.findAllByCollection(context, collection, limit, offset);
            int numProc = itemFilterSet.processSaveItems(context, servletContext, childItems, items, reportItems, expand);
            this.setNumberItemsProcessed(numProc);
        }       
        
        if(!expandFields.contains("all")) {
            this.addExpand("all");
        }
        this.setNumberItems(itemService.countAllItems(context, collection));
    }

    public Integer getNumberItems() {
        return numberItems;
    }

    public void setNumberItems(Integer numberItems) {
        this.numberItems = numberItems;
    }
    public Integer getNumberItemsProcessed() {
        return numberItemsProcessed;
    }

    public void setNumberItemsProcessed(Integer numberItemsProcessed) {
        this.numberItemsProcessed = numberItemsProcessed;
    }

    public Community getParentCommunity() {
        return parentCommunity;
    }

    public void setParentCommunity(Community parentCommunity) {
        this.parentCommunity = parentCommunity;
    }

    public Community getTopCommunity() {
        return topCommunity;
    }

    public void setTopCommunity(Community topCommunity) {
        this.topCommunity = topCommunity;
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

	public List<ItemFilter> getItemFilters() {
	    return itemFilters;
	}
	
	public void setItemFilters(List<ItemFilter> itemFilters) {
	    this.itemFilters = itemFilters;
	}
}
