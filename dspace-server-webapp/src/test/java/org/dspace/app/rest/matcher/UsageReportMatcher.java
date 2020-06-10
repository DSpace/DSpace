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

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.rest.model.UsageReportPointRest;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Matcher to match {@Link UsageReportRest}
 *
 * @author Maria Verdonck (Atmire) on 10/06/2020
 */
public class UsageReportMatcher {

    private UsageReportMatcher() {
    }

    private static Matcher<? super Object> matchUsageReport(String id, String reportType) {
        return allOf(
            hasJsonPath("$.id", is(id)),
            hasJsonPath("$.report-type", is(reportType))
                    );
    }

    public static Matcher<? super Object> matchUsageReport(String id, String reportType,
                                                           List<UsageReportPointRest> points) {
        return allOf(
            matchUsageReport(id, reportType),
            hasJsonPath("$.points", Matchers.containsInAnyOrder(
                points.stream().map(point -> UsageReportPointMatcher
                    .matchUsageReportPoint(point.getId(), point.getType(), point.getValues().get("views")))
                      .collect(Collectors.toList()))));
    }
}
