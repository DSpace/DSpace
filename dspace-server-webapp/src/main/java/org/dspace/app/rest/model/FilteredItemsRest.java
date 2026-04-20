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

import org.dspace.app.rest.ContentReportRestController;

/**
 * This class serves as a REST representation of a Filtered Items Report.
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
    public static final String CATEGORY = RestModel.CONTENT_REPORT;

    /** Items included in the report */
    private List<FilteredItemRest> items = new ArrayList<>();
    /** Total item count (for pagination) */
    private long itemCount;

    /**
     * Builds a FilteredItemsRest instance from a list of items and an total item count.
     * To avoid adding a dependency to any Spring-managed service here, the items
     * provided here are already converted to FilteredItemRest instances.
     * @param items the items to add to the FilteredItemsRest instance to be created
     * @param itemCount total number of items found regardless of any pagination constraint
     * @return a FilteredItemsRest instance built from the provided data
     */
    public static FilteredItemsRest of(List<FilteredItemRest> items, long itemCount) {
        var itemsRest = new FilteredItemsRest();
        itemsRest.items.addAll(items);
        itemsRest.itemCount = itemCount;
        return itemsRest;
    }

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
        return ContentReportRestController.class;
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
    public List<FilteredItemRest> getItems() {
        return new ArrayList<>(items);
    }

    public long getItemCount() {
        return itemCount;
    }

}
