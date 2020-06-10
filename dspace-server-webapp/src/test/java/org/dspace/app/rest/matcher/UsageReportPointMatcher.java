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

import org.hamcrest.Matcher;

/**
 * Matcher to match {@Link UsageReportPointRest}
 *
 * @author Maria Verdonck (Atmire) on 10/06/2020
 */
public class UsageReportPointMatcher {

    private UsageReportPointMatcher() {
    }

    public static Matcher<? super Object> matchUsageReportPoint(String id, String type, int views) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.values.views", is(views))
                    );
    }
}
