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
 * Create one or more static filter queries according to data passed during construction.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class SimpleQueryCustomAuthorityFilter extends CustomAuthorityFilter {

    private final List<String> customQueries;

    public SimpleQueryCustomAuthorityFilter(List<String> customQueries) {
        this.customQueries = customQueries;
    }

    @Override
    protected List<String> createFilterQueries() {
        return Optional.ofNullable(customQueries).orElseGet(Collections::emptyList);
    }
}
