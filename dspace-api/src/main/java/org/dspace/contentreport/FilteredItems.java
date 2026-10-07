/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Item;

/**
 * This class represents a list of items for a  Filtered Items report query.
 * Since the underlying list should correspond to only a page of results,
 * the total number of items found through the query is included in this report.
 *
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredItems implements Serializable {

    private static final long serialVersionUID = 7980375013177658249L;

    /** Items included in the report */
    private List<Item> items = new ArrayList<>();
    /** Total item count (for pagination) */
    private long itemCount;

    /**
     * Returns a defensive copy of the items included in this report.
     *
     * @return the items included in this report
     */
    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Adds an {@link ItemRest} object to this report.
     *
     * @param item {@link ItemRest} to add to this report
     */
    public void addItem(Item item) {
        items.add(item);
    }

    /**
     * Sets all items for this report.
     * The contents are copied into this object's internal list, which is protected
     * against further tampering with the provided list.
     *
     * @param items Values that replace the current ones
     */
    public void setItems(List<Item> items) {
        this.items.clear();
        this.items.addAll(items);
    }

    public long getItemCount() {
        return itemCount;
    }

    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }

}
