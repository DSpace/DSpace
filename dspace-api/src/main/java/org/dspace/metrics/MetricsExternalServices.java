/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public abstract class MetricsExternalServices {

    public static final int DEFAULT_FETCH_SIZE = 1;

    @Autowired
    private ItemService itemService;

    protected int fetchSize = DEFAULT_FETCH_SIZE;

    /**
     * Returns the name of the external service.
     */
    public abstract String getServiceName();

    /**
     * Updates and fetches metrics of a target {@code item}
     * 
     * @param context
     * @param item
     * @param param
     * @return
     */
    public abstract boolean updateMetric(Context context, Item item, String param);

    /**
     * Updates and fetches all items by delegating call to
     * {@link org.dspace.metrics.MetricsExternalServices.updateMetric(Context, Item,
     * String)}
     * 
     * @param context
     * @param itemList
     * @param param
     * @return number of items updated successfully
     */
    public long updateMetric(Context context, List<Item> itemList, String param) {
        return this.updateMetric(context, itemList.iterator(), param);
    }

    /**
     * Updates and fetches all items by delegating call to
     * {@link org.dspace.metrics.MetricsExternalServices.updateMetric(Context, Item,
     * String)}
     * 
     * @param context
     * @param itemIterator iterator of items
     * @param param
     * @return number of items updated successfully
     */
    public long updateMetric(Context context, Iterator<Item> itemIterator, String param) {
        long count = 0;
        while (itemIterator.hasNext()) {
            if (this.updateMetric(context, itemIterator.next(), param)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Set the last import metadata value to the given item based on the service name.
     */
    public void setLastImportMetadataValue(Context context, Item item) {
        try {
            item = context.reloadEntity(item);
            String metadataField = "dspace.lastimport." + getServiceName();
            String currentDate = DCDate.getCurrent().toString();
            itemService.setMetadataSingleValue(context, item, new MetadataFieldName(metadataField), null, currentDate);
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getFilters() {
        return Collections.emptyList();
    }

    /**
     * Checks if the service can fetch multiple items at once
     * 
     * @return true if can fetch multiple items at once
     */
    public boolean canMultiFetch() {
        return this.getFetchSize() > DEFAULT_FETCH_SIZE;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public List<String> getLogs() {
        return null;
    }
}
