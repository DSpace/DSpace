/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.rest.filter.ItemFilterDefs;
import org.dspace.rest.filter.ItemFilterTest;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Filtered list of items that match a specific set of criteria.
 */
@XmlRootElement(name = "item-filter")
public class ItemFilter {
    Logger log = Logger.getLogger(ItemFilter.class);

    private ItemFilterTest itemFilterTest = null;
    private String filterName = "";
    private String title;
    private String description;
    private String queryAnnotation;
    private List<Item> items = new ArrayList<Item>();
    private List<ItemFilterQuery> itemFilterQueries = new ArrayList<ItemFilterQuery>();
    private List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
    private Integer itemCount;
    private boolean saveItems = false;

    public ItemFilter(){}

    public static final String ALL_FILTERS = "all_filters";
    public static final String ALL = "all";
    
    public static List<ItemFilter> getItemFilters(String filters, boolean saveItems) {
        List<ItemFilter> itemFilters = new ArrayList<ItemFilter>();
        ItemFilter allFilters = new ItemFilter(ItemFilter.ALL_FILTERS, "Matches all specified filters", "This filter includes all items that matched ALL specified filters", saveItems);

        if (filters.equals(ALL)) {
            for(ItemFilterDefs itemFilterDef: ItemFilterDefs.values()) {
                itemFilters.add(new ItemFilter(itemFilterDef, saveItems));
            }                
            itemFilters.add(allFilters);
        } else {
            for(String filter: Arrays.asList(filters.split(","))) {
                if (filter.equals(ItemFilter.ALL_FILTERS)) {
                    itemFilters.add(allFilters);
                    continue;
                }
                
                ItemFilterDefs itemFilterDef;
                try {
                    itemFilterDef = ItemFilterDefs.valueOf(filter);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                itemFilters.add(new ItemFilter(itemFilterDef, saveItems));
            }
        }
        return itemFilters;
    }
    
    public static ItemFilter getAllFiltersFilter(List<ItemFilter> itemFilters) {
        for(ItemFilter itemFilter: itemFilters) {
            if (itemFilter.getFilterName().equals(ALL_FILTERS)) {
                itemFilter.initCount();
                return itemFilter;
            }
        }
        return null;
    }
    
    public ItemFilter(ItemFilterTest itemFilterTest, boolean saveItems) throws WebApplicationException{
        this.itemFilterTest = itemFilterTest;
        this.saveItems = saveItems;
        setup(itemFilterTest.getName(), itemFilterTest.getTitle(), itemFilterTest.getDescription());
    }
    
    public ItemFilter(String name, String title, String description, boolean saveItems) throws WebApplicationException{
        this.saveItems = saveItems;
        setup(name, title, description);
    }

    private void setup(String name, String title, String description) {
        this.setFilterName(name);
        this.setTitle(title);
        this.setDescription(description);
    }

    private void initCount() {
        if (itemCount == null) {
            itemCount = 0;
        }        
    }    
    
    public boolean hasItemTest() {
        return itemFilterTest != null;
    }
    
    public void addItem(Item restItem) {
        initCount();
        if (saveItems){
            items.add(restItem);            
        }
        itemCount++;
    }
    
    public boolean testItem(Context context, org.dspace.content.Item item, Item restItem) {
        initCount();
        if (itemFilterTest == null) {
            return false;
        }
        if (itemFilterTest.testItem(context, item)) {
            addItem(restItem);
            return true;
        }
        return false;
    }


    @XmlAttribute(name="filter-name")
    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String name) {
        this.filterName = name;
    }
    
    @XmlAttribute(name="title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @XmlAttribute(name="description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlAttribute(name="query-annotation")
    public String getQueryAnnotation() {
        return queryAnnotation;
    }

    public void setQueryAnnotation(String queryAnnotation) {
        this.queryAnnotation = queryAnnotation;
    }

    @XmlAttribute(name="item-count")
    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public List<Item> getItems() {
        return items;
    }

	public void setItems(List<Item> items) {
		this.items = items;
	}

    public List<ItemFilterQuery> getItemFilterQueries() {
        return itemFilterQueries;
    }

    public void setItemFilterQueries(List<ItemFilterQuery> itemFilterQueries) {
        this.itemFilterQueries = itemFilterQueries;
    }

    public List<MetadataEntry> getMetadata() {
        return metadata;
    }

    @XmlElement(required = true)
    public void setMetadata(List<MetadataEntry> metadata) {
        this.metadata = metadata;
    }
}
