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
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.app.rest.contentreport.Filter;

/**
 * Structured query contents for the Filtered Collections report
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredCollectionsQuery {

    private Map<Filter, Boolean> filters = new EnumMap<>(Filter.class);

    public Map<Filter, Boolean> getFilters() {
        return filters;
    }

    public void setFilters(Map<Filter, Boolean> filters) {
        this.filters = filters;
    }

    /**
     * The setFilters() method above must not be overloaded, otherwise any
     * attempt to deserialize FilteredCollectionsQuery instances from a
     * JSON payload will result in an error 415 (Unsupported Media Type).
     */
    public void setFiltersFromCollection(Collection<Filter> filters) {
        Arrays.stream(Filter.values()).forEach(f -> this.filters.put(f, Boolean.FALSE));
        filters.forEach(f -> this.filters.put(f, Boolean.TRUE));
    }

    public Set<Filter> getEnabledFilters() {
        return filters.entrySet().stream()
                .filter(e -> e.getValue().booleanValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Filter.class)));
    }

}
