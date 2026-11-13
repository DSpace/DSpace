/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

public class StatisticsCategoryMatcher {

    private StatisticsCategoryMatcher() { }

    public static Matcher<? super Object> match(String id, String type) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.category-type", is(type)),
            hasJsonPath("$.type", is("category")),
            hasJsonPath("$.uniqueType", is("statistics.category")),
            hasJsonPath("$._links.self.href", endsWith("/" +  id))
        );
    }

}
