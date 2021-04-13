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
 * Unit tests for {@link CustomAuthorityFilter} flow
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CustomAuthorityFilterTest {

    @Test
    public void appliesOnAllEntities() {
        CustomAuthorityFilter customAuthorityFilter = new CustomAuthorityFilter() {
            @Override
            protected List<String> createFilterQueries() {
                return Arrays.asList("query 1", "query 2");
            }
        };

        List<String> queries = customAuthorityFilter.getFilterQueries("rel1");

        assertThat(queries, is(Arrays.asList("query 1", "query 2")));
    }

    @Test
    public void appliesOnGivenEntity() {
        CustomAuthorityFilter customAuthorityFilter = new CustomAuthorityFilter() {
            @Override
            protected List<String> createFilterQueries() {
                return Arrays.asList("query 1", "query 2");
            }
        };

        customAuthorityFilter.setSupportedEntities(Arrays.asList("rel1", "rel2"));

        List<String> queries = customAuthorityFilter.getFilterQueries("rel1");

        assertThat(queries, is(Arrays.asList("query 1", "query 2")));
    }

    @Test
    public void notAppliableOnGivenEntity() {
        CustomAuthorityFilter customAuthorityFilter = new CustomAuthorityFilter() {
            @Override
            protected List<String> createFilterQueries() {
                return Arrays.asList("query 1", "query 2");
            }
        };

        customAuthorityFilter.setSupportedEntities(Arrays.asList("rel3", "rel2"));

        List<String> queries = customAuthorityFilter.getFilterQueries("rel1");

        assertThat(queries, is(Collections.emptyList()));
    }

    @Test
    public void emptySupportedEntities() {
        CustomAuthorityFilter customAuthorityFilter = new CustomAuthorityFilter() {
            @Override
            protected List<String> createFilterQueries() {
                return Arrays.asList("query 1", "query 2");
            }
        };

        customAuthorityFilter.setSupportedEntities(Collections.emptyList());

        List<String> queries = customAuthorityFilter.getFilterQueries("rel1");

        assertThat(queries, is(Collections.emptyList()));
    }
}