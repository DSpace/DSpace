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
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.rest.filter.ItemFilterDefs;
import org.dspace.rest.filter.ItemFilterList;
import org.dspace.rest.filter.ItemFilterTest;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * Use Case Item Filters that match a specific set of criteria.
 * @author Terry Brady, Georgetown University
 */
@XmlRootElement(name = "item-filter")
public class ItemFilter {
    static Logger log = Logger.getLogger(ItemFilter.class);

    private ItemFilterTest itemFilterTest = null;
    private String filterName = "";
    private String title;
    private String description;
    private String category;
    private String queryAnnotation;
    private List<org.dspace.rest.common.Item> items = new ArrayList<org.dspace.rest.common.Item>();
    private List<ItemFilterQuery> itemFilterQueries = new ArrayList<ItemFilterQuery>();
    private List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
    private Integer itemCount;
    private Integer unfilteredItemCount;
    private boolean saveItems = false;

    public ItemFilter(){}

    public static final String ALL_FILTERS = "all_filters";
    public static final String ALL = "all";
    
    public static List<ItemFilter> getItemFilters(String filters, boolean saveItems) {
    	LinkedHashMap<String,ItemFilterTest> availableTests = new LinkedHashMap<String,ItemFilterTest>();
    	for(ItemFilterList plugobj: (ItemFilterList[]) CoreServiceFactory.getInstance().getPluginService().getPluginSequence(ItemFilterList.class)) {
			for(ItemFilterTest defFilter: plugobj.getFilters()) {
				availableTests.put(defFilter.getName(), defFilter);
			}
    	}
        List<ItemFilter> itemFilters = new ArrayList<ItemFilter>();
        ItemFilter allFilters = new ItemFilter(ItemFilter.ALL_FILTERS, "Matches all specified filters", 
        		"This filter includes all items that matched ALL specified filters", ItemFilterDefs.CAT_ITEM, saveItems);

        if (filters.equals(ALL)) {
            for(ItemFilterTest itemFilterDef: availableTests.values()) {
                itemFilters.add(new ItemFilter(itemFilterDef, saveItems));
            }                
            itemFilters.add(allFilters);
        } else {
            for(String filter: Arrays.asList(filters.split(","))) {
                if (filter.equals(ItemFilter.ALL_FILTERS)) {
                    continue;
                }
                
                ItemFilterTest itemFilterDef;
                itemFilterDef = availableTests.get(filter);
                if (itemFilterDef == null) {
                	continue;
                }
                itemFilters.add(new ItemFilter(itemFilterDef, saveItems));
            }
            itemFilters.add(allFilters);
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
        setup(itemFilterTest.getName(), itemFilterTest.getTitle(), itemFilterTest.getDescription(), itemFilterTest.getCategory());
    }
    
    public ItemFilter(String name, String title, String description, String category, boolean saveItems) throws WebApplicationException{
        this.saveItems = saveItems;
        setup(name, title, description, category);
    }

    private void setup(String name, String title, String description, String category) {
        this.setFilterName(name);
        this.setTitle(title);
        this.setDescription(description);
        this.setCategory(category);
    }

    private void initCount() {
        if (itemCount == null) {
            itemCount = 0;
        }        
        if (unfilteredItemCount == null) {
            unfilteredItemCount = 0;
        }    
    }    
    
    public boolean hasItemTest() {
        return itemFilterTest != null;
    }
    
    public void addItem(org.dspace.rest.common.Item restItem) {
        initCount();
        if (saveItems){
            items.add(restItem);            
        }
        itemCount++;
    }
    
    public boolean testItem(Context context, org.dspace.content.Item item, org.dspace.rest.common.Item restItem) {
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

    @XmlAttribute(name="category")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public void annotateQuery(List<String> query_field, List<String> query_op, List<String> query_val) throws SQLException {
        int index = Math.min(query_field.size(), Math.min(query_op.size(), query_val.size()));
        StringBuilder sb = new StringBuilder();

        for(int i=0; i<index; i++) {
        	if (!sb.toString().isEmpty()) {
        		sb.append(" and ");
        	}
        	sb.append("(");
        	sb.append(query_field.get(i));
        	sb.append(" ");
        	sb.append(query_op.get(i));
        	sb.append(" ");
        	sb.append(query_val.get(i));
        	sb.append(")");
        }
        setQueryAnnotation(sb.toString());
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

    @XmlAttribute(name="unfiltered-item-count")
    public Integer getUnfilteredItemCount() {
        return unfilteredItemCount;
    }

    public void setUnfilteredItemCount(Integer unfilteredItemCount) {
        this.unfilteredItemCount = unfilteredItemCount;
    }

    public List<org.dspace.rest.common.Item> getItems() {
        return items;
    }

	public void setItems(List<org.dspace.rest.common.Item> items) {
		this.items = items;
	}

    public List<ItemFilterQuery> getItemFilterQueries() {
        return itemFilterQueries;
    }

    public void setItemFilterQueries(List<ItemFilterQuery> itemFilterQueries) {
        this.itemFilterQueries = itemFilterQueries;
    }

    public void initMetadataList(List<String> show_fields) {
        if (show_fields != null) {
            List<MetadataEntry> returnFields = new ArrayList<MetadataEntry>();
            for(String field: show_fields) {
                returnFields.add(new MetadataEntry(field, null, null));
            }
            setMetadata(returnFields);                    
        }
    }
    
    public List<MetadataEntry> getMetadata() {
        return metadata;
    }

    @XmlElement(required = true)
    public void setMetadata(List<MetadataEntry> metadata) {
        this.metadata = metadata;
    }
}
