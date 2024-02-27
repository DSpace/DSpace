/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.contentreport.Filter;
import org.dspace.contentreport.FilteredCollection;

/**
 * This class serves as a REST representation of a single Collection in a {@link FilteredCollectionsRest}
 * from the DSpace statistics. It takes its values from a @link FilteredCollection} instance.
 * It must not extend BaseObjectRest<?>.
 *
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredCollectionRest {

    public static final String NAME = "filtered-collection";
    public static final String CATEGORY = RestModel.CONTENT_REPORT;

    /** Name of the collection */
    private String label;
    /** Handle of the collection, used to make it clickable from the generated report */
    private String handle;
    /** Name of the owning community */
    @JsonProperty("community_label")
    private String communityLabel;
    /** Handle of the owning community, used to make it clickable from the generated report */
    @JsonProperty("community_handle")
    private String communityHandle;
    /** Total number of items in the collection */
    @JsonProperty("nb_total_items")
    private int totalItems;
    /** Number of filtered items per requested filter in the collection */
    private Map<Filter, Integer> values = new EnumMap<>(Filter.class);
    /** Number of items in the collection that match all requested filters */
    @JsonProperty("all_filters_value")
    private int allFiltersValue;

    /**
     * Builds a FilteredCollectionRest instance from a {@link FilteredCollection} instance.
     * @param model the FilteredCollection instance that provides values to the
     * FilteredCollectionRest instance to be created
     * @return a FilteredCollectionRest instance built from the provided model object
     */
    public static FilteredCollectionRest of(FilteredCollection model) {
        Objects.requireNonNull(model);

        var coll = new FilteredCollectionRest();
        coll.label = model.getLabel();
        coll.handle = model.getHandle();
        coll.communityLabel = model.getCommunityLabel();
        coll.communityHandle = model.getCommunityHandle();
        coll.totalItems = model.getTotalItems();
        coll.allFiltersValue = model.getAllFiltersValue();
        Optional.ofNullable(model.getValues()).ifPresent(coll.values::putAll);

        return coll;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public Map<Filter, Integer> getValues() {
        return values;
    }

    public String getLabel() {
        return label;
    }

    public String getHandle() {
        return handle;
    }

    public String getCommunityLabel() {
        return communityLabel;
    }

    public String getCommunityHandle() {
        return communityHandle;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getAllFiltersValue() {
        return allFiltersValue;
    }

}
