/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Provides some custom filter queries to be used during Item Authority lookup.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public abstract class CustomAuthorityFilter {

    protected List<String> customQueries;

    /**
     * Evaluates the authority context and returns the list of custom Solr filter
     * queries if this filter is applicable.
     *
     * @param linkableEntityAuthority the authority instance being queried
     * @return a list of custom solr filter queries, or an empty list if
     * {@link #appliesTo(LinkableEntityAuthority)} returns false.
     */
    public List<String> getFilterQueries(LinkableEntityAuthority linkableEntityAuthority) {
        if (appliesTo(linkableEntityAuthority)) {
            return createFilterQueries();
        }
        return Collections.emptyList();
    }

    /**
     * Determines if this filter instance should provide additional queries for the
     * specific authority configuration or relationship type.
     *
     * @param linkableEntityAuthority the authority instance being queried
     * @return true if the filter should be applied, false otherwise
     */
    public abstract boolean appliesTo(LinkableEntityAuthority linkableEntityAuthority);

    /**
     * Safely retrieves the internal list of configured custom Solr queries.
     *
     * @return the list of filter queries, or an empty list if none are configured.
     */
    protected final List<String> createFilterQueries() {
        return Optional.ofNullable(customQueries).orElseGet(Collections::emptyList);
    }

}
