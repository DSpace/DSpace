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

import org.dspace.app.rest.model.UsageReportPointRest;
import org.hamcrest.Matcher;

/**
 * Matcher to match {@Link UsageReportPointRest}
 *
 * @author Maria Verdonck (Atmire) on 10/06/2020
 */
public class UsageReportPointMatcher {

    private UsageReportPointMatcher() {
    }

    /**
     * Matcher for the usage report points (see {@link UsageReportPointRest})
     *
     * @param id    Id to match if of json of UsageReportPoint
     * @param label    Label to match if of json of UsageReportPoint
     * @param type  Type to match if of json of UsageReportPoint
     * @param views Nr of views, is in the values key-value pair of values of UsageReportPoint with key "views"
     * @return The matcher
     */
    public static Matcher<? super Object> matchUsageReportPoint(String id, String label, String type, int views) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.label", is(label)),
            hasJsonPath("$.type", is(type)),
            hasJsonPath("$.values.views", is(views))
        );
    }
}
