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
import static org.hamcrest.Matchers.is;

import org.dspace.app.rest.model.hateoas.NBTopicResource;
import org.hamcrest.Matcher;

/**
 * Matcher related to {@link NBTopicResource}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class NBTopicMatcher {

    private NBTopicMatcher() { }

    public static Matcher<? super Object> matchNBTopicEntry(String key, int totalEvents) {
        return allOf(
            hasJsonPath("$.type", is("nbtopic")),
            hasJsonPath("$.name", is(key)),
            hasJsonPath("$.id", is(key.replace("/", "!"))),
            hasJsonPath("$.totalEvents", is(totalEvents))
        );
    }


    public static Matcher<? super Object> matchNBTopicEntry(String key) {
        return allOf(
            hasJsonPath("$.type", is("nbtopic")),
            hasJsonPath("$.name", is(key)),
            hasJsonPath("$.id", is(key.replace("/", "/")))
        );
    }

}
