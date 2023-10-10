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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Matcher;

/**
 * Matcher for the Submission COAR Notify.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 *
 */
public class SubmissionCOARNotifyMatcher {

    private SubmissionCOARNotifyMatcher() {
    }

    public static Matcher<? super Object> matchCOARNotifyEntry(String id, String name, List<String> patterns) {
        return allOf(
            matchCOARNotifyProperties(id, name),
            matchPatterns(patterns)
        );
    }

    private static Matcher<? super Object> matchPatterns(List<String> patterns) {
        List<Matcher<? super Object>> matchers = new LinkedList<>();
        patterns.forEach(pattern ->
            matchers.add(hasJsonPath("$.pattern", equalTo(pattern))));

        return hasJsonPath("$.patterns", contains(matchers));
    }

    public static Matcher<? super Object> matchCOARNotifyProperties(String id, String name) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.name", is(name))
        );
    }

}
