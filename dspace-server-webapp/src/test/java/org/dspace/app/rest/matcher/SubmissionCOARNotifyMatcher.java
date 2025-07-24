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
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.dspace.coarnotify.NotifyPattern;
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

    public static Matcher<? super Object> matchCOARNotifyEntry(String id, List<NotifyPattern> patterns) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath(
                "$.patterns", contains(
                patterns.stream()
                        .map(coarPattern ->
                            allOf(
                                hasJsonPath("pattern", is(coarPattern.getPattern())),
                                hasJsonPath("multipleRequest", is(coarPattern.isMultipleRequest()))
                            ))
                        .toArray(Matcher[]::new))));
    }

}
