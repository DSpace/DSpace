/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.contentreport.Filter;

/**
 * Structured query contents for the Filtered Collections report
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredCollectionsQuery {

    private Map<Filter, Boolean> filters = new EnumMap<>(Filter.class);

    /**
     * Shortcut method that builds a FilteredCollectionsQuery instance
     * from its building blocks.
     * @param filters filters to apply to existing items.
     * The filters mapping to true will be applied, others (either missing or
     * mapping to false) will not.
     * @return a FilteredCollectionsQuery instance built from the provided parameters
     */
    public static FilteredCollectionsQuery of(Map<Filter, Boolean> filters) {
        var query = new FilteredCollectionsQuery();
        Optional.ofNullable(filters).ifPresent(query.filters::putAll);
        return query;
    }

    public static FilteredCollectionsQuery of(Collection<Filter> filters) {
        var query = new FilteredCollectionsQuery();
        Arrays.stream(Filter.values()).forEach(f -> query.filters.put(f, Boolean.FALSE));
        filters.forEach(f -> query.filters.put(f, Boolean.TRUE));
        return query;
    }

    public Map<Filter, Boolean> getFilters() {
        return filters;
    }

    public void setFilters(Map<Filter, Boolean> filters) {
        this.filters = filters;
    }

    public Set<Filter> getEnabledFilters() {
        return filters.entrySet().stream()
                .filter(e -> e.getValue().booleanValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Filter.class)));
    }

}
