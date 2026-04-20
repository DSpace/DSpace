/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Identifies the category/section of filters defined in the {@link Filter} enum.
 * This enum will be used when/if the structured filter definitions are returned to
 * the Angular layer through a REST endpoint.
 *
 * @author Jean-François Morin (Université Laval)
 */
public enum FilterCategory {

    PROPERTY("property"),
    BITSTREAM("bitstream"),
    BITSTREAM_MIME("bitstream_mime"),
    MIME("mime"),
    BUNDLE("bundle"),
    PERMISSION("permission");

    private String id;
    private List<Filter> filters;

    FilterCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<Filter> getFilters() {
        if (filters == null) {
            filters = Arrays.stream(Filter.values())
                    .filter(f -> f.getCategory() == this)
                    .collect(Collectors.toUnmodifiableList());
        }
        return filters;
    }

}
