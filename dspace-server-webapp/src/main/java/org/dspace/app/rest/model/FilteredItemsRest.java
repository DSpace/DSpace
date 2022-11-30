/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.ContentReportsRestController;

/**
 * This class serves as a REST representation of a Filtered Items Report from the DSpace statistics.
 * The name must match that of the associated resource class (FilteredItemsResource) except for
 * the suffix. This is why it is not named something like FilteredItemsReportRest.
 *
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredItemsRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -2483812920345013458L;
    /** Type of instances of this class, used by the DSpace REST infrastructure */
    public static final String NAME = "filtereditemsreport";
    /** Category of instances of this class, used by the DSpace REST infrastructure */
    public static final String CATEGORY = RestModel.CONTENT_REPORTS;

    /** Items included in the report */
    private List<FilteredItemRest> items = new ArrayList<>();
    /** Total item count (for pagination) */
    private long itemCount;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Return controller class responsible for this Rest object
     *
     * @return Controller class responsible for this Rest object
     */
    @Override
    public Class<?> getController() {
        return ContentReportsRestController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return getType();
    }

    /**
     * Returns a defensive copy of the items included in this report.
     *
     * @return the items included in this report
     */
    public List<ItemRest> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Adds an {@link ItemRest} object to this report.
     *
     * @param item {@link ItemRest} to add to this report
     */
    public void addItem(FilteredItemRest item) {
        items.add(item);
    }

    /**
     * Sets all items for this report.
     * The contents are copied into this object's internal list, which is protected against
     * further tampering with the provided list.
     *
     * @param items Values that replace the current ones
     */
    public void setItems(List<FilteredItemRest> items) {
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
