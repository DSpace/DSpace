/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.app.rest.DiscoveryRestController;

/**
 * This class' purpose is to store the information that'll be shown on the /search endpoint.
 */
public class DiscoveryConfigurationRest extends BaseObjectRest<String> {

    public static final String NAME = "discover";
    public static final String PLURAL_NAME = NAME;
    public static final String CATEGORY = RestModel.DISCOVER;
    public static final String SEARCH_FILTER = "search-filter";
    public static final String SORT_OPTION = "sort-option";


    @JsonIgnore
    private String scope;
    @JsonIgnore
    private String configuration;

    private List<SearchFilterRest> filters = new LinkedList<>();
    private List<SortOptionRest> sortOptions = new LinkedList<>();

    private SortOptionRest defaultSortOption;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    /**
     * The plural name is the same as the singular name
     */
    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configurationName) {
        this.configuration = configurationName;
    }

    public void addFilter(SearchFilterRest filter) {
        filters.add(filter);
    }

    public List<SearchFilterRest> getFilters() {
        return filters;
    }

    public void addSortOption(SortOptionRest sortOption) {
        sortOptions.add(sortOption);
    }

    public List<SortOptionRest> getSortOptions() {
        return sortOptions;
    }

    public SortOptionRest getDefaultSortOption() {
        return defaultSortOption;
    }

    public void setDefaultSortOption(SortOptionRest defaultSortOption) {
        this.defaultSortOption = defaultSortOption;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof SearchConfigurationRest &&
            new EqualsBuilder().append(this.getCategory(), ((SearchConfigurationRest) object).getCategory())
                .append(this.getType(), ((SearchConfigurationRest) object).getType())
                .append(this.getController(), ((SearchConfigurationRest) object).getController())
                .append(this.getScope(), ((SearchConfigurationRest) object).getScope())
                .append(this.getConfiguration(),
                    ((SearchConfigurationRest) object).getConfiguration())
                .append(this.getFilters(), ((SearchConfigurationRest) object).getFilters())
                .append(this.getSortOptions(), ((SearchConfigurationRest) object).getSortOptions())
                .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.getCategory())
            .append(this.getType())
            .append(this.getController())
            .append(this.getScope())
            .append(this.getConfiguration())
            .append(this.getFilters())
            .append(this.getSortOptions())
            .toHashCode();
    }
}
