/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.filter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.rest.common.Item;
import org.dspace.rest.common.ItemFilter;

/**
 * The set of Item Filter Use Cases to apply to a collection of items.
 * 
 * @author Terry Brady, Georgetown University
 * 
 */
public class ItemFilterSet {
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    static Logger log = Logger.getLogger(ItemFilterSet.class);

    private List<ItemFilter> itemFilters;
    private ItemFilter allFiltersFilter; 

    /**
     * Construct a set of Item Filters identified by a list string.
     * 
     * @param filterList
     *     Comma separated list of filter names to include.
     *     Use {@link org.dspace.rest.common.ItemFilter#ALL} to retrieve all filters.
     * @param reportItems
     *     If true, return item details.  If false, return only counts of items.
     */
    public ItemFilterSet(String filterList, boolean reportItems) {
    	log.debug(String.format("Create ItemFilterSet: %s", filterList));
        itemFilters = ItemFilter.getItemFilters(filterList, reportItems);
        allFiltersFilter = ItemFilter.getAllFiltersFilter(itemFilters);
    }
    
    /**
     * Get the special filter that represents the intersection of all items in the Item Filter Set.
     * @return the special Item Filter that contains items that satisfied every other Item Filter in the Item Filter Set
     */
    public ItemFilter getAllFiltersFilter() {
        return allFiltersFilter;
    }

    /**
     * Evaluate an item against the use cases in the Item Filter Set.
     * 
     * If an item satisfies all items in the Item Filter Set, it should also ve added to the special all items filter.
     * 
     * @param context 
     *     Active DSpace Context
     * @param item 
     *     DSpace Object to evaluate
     * @param restItem
     *     REST representation of the DSpace Object being evaluated
     */
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
    
    /**
     * Get all of the Item Filters initialized into the Item Filter Set
     *
     * @return a list of Item Filters initialized into the Item Filter Set
     */
    public List<ItemFilter> getItemFilters() {
        return itemFilters;
    }
    
    /**
     * Evaluate a set of Items against the Item Filters in the Item Filter Set
     *     Current DSpace Context
     *
     * @param context
     *     Current DSpace Context
     * @param servletContext
     *     Context of the servlet container.
     * @param childItems
     *     Collection of Items to Evaluate
     * @param save
     *     If true, save the details of each item that is evaluated
     * @param expand
     *     List of item details to include in the results
     * @return 
     *     The number of items evaluated 
     * @throws WebApplicationException
     *     Runtime exception for applications.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public int processSaveItems(Context context, ServletContext servletContext, Iterator<org.dspace.content.Item> childItems, boolean save, String expand) throws WebApplicationException, SQLException {
    	return processSaveItems(context, servletContext, childItems, new ArrayList<Item>(), save, expand);
    }

    /**
     * Evaluate a set of Items against the Item Filters in the Item Filter Set
     *
     * @param context
     *     Current DSpace Context
     * @param servletContext
     *     Context of the servlet container.
     * @param childItems
     *     Collection of Items to Evaluate
     * @param items
     *     List of items to contain saved results
     * @param save
     *     If true, save the details of each item that is evaluated
     * @param expand
     *     List of item details to include in the results
     * @return 
     *     The number of items evaluated 
     * @throws WebApplicationException
     *     Runtime exception for applications.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public int processSaveItems(Context context, ServletContext servletContext, Iterator<org.dspace.content.Item> childItems, List<Item> items, boolean save, String expand) throws WebApplicationException, SQLException {
    	int count = 0;
        while(childItems.hasNext()) {
        	count++;
            org.dspace.content.Item item = childItems.next();
            log.debug(item.getHandle() + " evaluate.");
            if(authorizeService.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) { 
                Item restItem = new Item(item, servletContext, expand, context);
                if(save) {
                    items.add(restItem);
                }
                testItem(context, item, restItem);
            } else {
                log.debug(item.getHandle() + " not authorized - not included in result set.");
            }
        }
        return count;
    }
    
}
