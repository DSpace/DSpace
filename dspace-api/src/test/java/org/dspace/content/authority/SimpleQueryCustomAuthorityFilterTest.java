/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link SimpleQueryCustomAuthorityFilter}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class SimpleQueryCustomAuthorityFilterTest {

    @Test
    public void staticQueriesReturned() {
        List<String> filterQueries = new SimpleQueryCustomAuthorityFilter(Arrays.asList("query 1", "query 2"))
            .createFilterQueries();

        assertThat(filterQueries, is(Arrays.asList("query 1", "query 2")));
    }

    @Test
    public void nullParamInConstructor() {
        List<String> filterQueries = new SimpleQueryCustomAuthorityFilter(null)
            .createFilterQueries();

        assertThat(filterQueries, is(Collections.emptyList()));
    }

    @Test
    public void emptyListInConstructor() {
        List<String> filterQueries = new SimpleQueryCustomAuthorityFilter(Collections.emptyList())
            .createFilterQueries();

        assertThat(filterQueries, is(Collections.emptyList()));
    }
}