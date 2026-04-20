/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class represents an entry in the Filtered Collections report.
 *
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredCollection implements Cloneable, Serializable {

    private static final long serialVersionUID = -231735620268582719L;

    /** Name of the collection */
    private String label;
    /** Handle of the collection, used to make it clickable from the generated report */
    private String handle;
    /** Name of the owning community */
    private String communityLabel;
    /** Handle of the owning community, used to make it clickable from the generated report */
    private String communityHandle;
    /** Total number of items in the collection */
    private int totalItems;
    /** Number of filtered items per requested filter in the collection */
    private Map<Filter, Integer> values = new EnumMap<>(Filter.class);
    /** Number of items in the collection that match all requested filters */
    private int allFiltersValue;
    /**
     * Indicates whether this object is protected against further changes.
     * This is used in computing summary data in the parent FilteredCollectionsRest class.
     */
    private boolean sealed;

    /**
     * Shortcut method that builds a FilteredCollectionRest instance
     * from its building blocks.
     * @param label Name of the collection
     * @param handle Handle of the collection
     * @param communityLabel Name of the owning community
     * @param communityHandle Handle of the owning community
     * @param totalItems Total number of items in the collection
     * @param allFiltersValue Number of items in the collection that match all requested filters
     * @param values Number of filtered items per requested filter in the collection
     * @param doSeal true if the collection must be sealed immediately
     * @return a FilteredCollectionRest instance built from the provided parameters
     */
    public static FilteredCollection of(String label, String handle,
            String communityLabel, String communityHandle,
            int totalItems, int allFiltersValue, Map<Filter, Integer> values, boolean doSeal) {
        var coll = new FilteredCollection();
        coll.label = label;
        coll.handle = handle;
        coll.communityLabel = communityLabel;
        coll.communityHandle = communityHandle;
        coll.totalItems = totalItems;
        coll.allFiltersValue = allFiltersValue;
        Optional.ofNullable(values).ifPresent(vs -> vs.forEach(coll::addValue));
        if (doSeal) {
            coll.seal();
        }
        return coll;
    }

    /**
     * Returns the item counts per filter.
     * If this object is sealed, a defensive copy will be returned.
     *
     * @return the item counts per filter
     */
    public Map<Filter, Integer> getValues() {
        if (sealed) {
            return new EnumMap<>(values);
        }
        return values;
    }

    /**
     * Increments a filtered item count for a given filter.
     *
     * @param filter Filter to add to the requested filters in this collection
     * @param delta Number by which the filtered item count must be incremented
     * for the requested filter
     */
    public void addValue(Filter filter, int delta) {
        checkSealed();
        Integer oldValue = values.getOrDefault(filter, Integer.valueOf(0));
        int newValue = oldValue.intValue() + delta;
        values.put(filter, Integer.valueOf(newValue));
    }

    /**
     * Sets all filtered item counts for this collection.
     * The contents are copied into this object's internal Map, which is protected against
     * further tampering with the provided Map.
     *
     * @param values Values that replace the current ones
     */
    public void setValues(Map<? extends Filter, ? extends Integer> values) {
        checkSealed();
        this.values.clear();
        this.values.putAll(values);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        checkSealed();
        this.label = label;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        checkSealed();
        this.handle = handle;
    }

    public String getCommunityLabel() {
        return communityLabel;
    }

    public void setCommunityLabel(String communityLabel) {
        checkSealed();
        this.communityLabel = communityLabel;
    }

    public String getCommunityHandle() {
        return communityHandle;
    }

    public void setCommunityHandle(String communityHandle) {
        checkSealed();
        this.communityHandle = communityHandle;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        checkSealed();
        this.totalItems = totalItems;
    }

    public int getAllFiltersValue() {
        return allFiltersValue;
    }

    /**
     * Increments the count of items matching all filters.
     *
     * @param delta Number by which the count must be incremented
     */
    public void addAllFiltersValue(int delta) {
        checkSealed();
        allFiltersValue++;
    }

    /**
     * Replaces the count of items matching all filters.
     *
     * @param allFiltersValue Number that replaces the current item count
     */
    public void setAllFiltersValue(int allFiltersValue) {
        checkSealed();
        this.allFiltersValue = allFiltersValue;
    }

    public boolean getSealed() {
        return sealed;
    }

    /**
     * Seals this filtered collection object.
     * No changes to this object can be made afterwards. Any attempt will throw
     * an IllegalStateException.
     */
    public void seal() {
        sealed = true;
    }

    private void checkSealed() {
        if (sealed) {
            throw new IllegalStateException("This filtered collection record is sealed"
                    + " and cannot be modified anymore. You can apply changes to a non-sealed clone.");
        }
    }

    /**
     * Returns a non-sealed clone of this filtered collection record.
     *
     * @return a new non-sealed FilteredCollectionRest instance containing
     * all attribute values of this object
     */
    @Override
    public FilteredCollection clone() {
        var clone = new FilteredCollection();
        clone.label = label;
        clone.handle = handle;
        clone.values.putAll(values);
        clone.allFiltersValue = allFiltersValue;
        return clone;
    }

}
