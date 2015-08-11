package org.dspace.rest.filter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.rest.common.Item;
import org.dspace.rest.common.ItemFilter;

public class ItemFilterSet {
    private List<ItemFilter> itemFilters;
    private ItemFilter allFiltersFilter; 

    public ItemFilterSet(String filterList, boolean reportItems) {
        itemFilters = ItemFilter.getItemFilters(filterList, reportItems);
        allFiltersFilter = ItemFilter.getAllFiltersFilter(itemFilters);
    }
    
    public ItemFilter getAllFiltersFilter() {
        return allFiltersFilter;
    }

    public void testItem(Context context, org.dspace.content.Item item, Item restItem) {
        boolean bAllTrue = true;
        for(ItemFilter itemFilter: itemFilters) {
            if (itemFilter.hasItemTest()) {
                bAllTrue &= itemFilter.testItem(context, item, restItem);                            
            }
        }
        if (bAllTrue && allFiltersFilter != null) {
            allFiltersFilter.addItem(restItem);
        }
    }
    
    public List<ItemFilter> getItemFilters() {
        return itemFilters;
    }
    
    public void processItems(Context context, ItemIterator childItems) throws WebApplicationException, SQLException {
        processSaveItems(context, childItems, false, null);
    }

    public List<Item> processSaveItems(Context context, ItemIterator childItems, boolean save, String expand) throws WebApplicationException, SQLException {
        List<Item> items = new ArrayList<Item>();
        while(childItems.hasNext()) {
            org.dspace.content.Item item = childItems.next();
            if(AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
                Item restItem = new Item(item, expand, context); 
                if(save) {
                    items.add(restItem);
                }
               testItem(context, item, restItem);
            }
        }
        return items;
    }
}
