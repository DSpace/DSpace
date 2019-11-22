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
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.ProcessStatus;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class ProcessMatcher {

    private ProcessMatcher() {
    }

    private static Matcher<? super Object> matchProcess(String name, String userId) {
        return allOf(
            hasJsonPath("$.scriptName", is(name)),
            hasJsonPath("$.userId", is(userId))
        );

    }

    public static Matcher<? super Object> matchProcess(String name, String userId, Integer processId,
                                                       List<DSpaceCommandLineParameter> list, ProcessStatus status) {
        return allOf(
            matchProcess(name, userId, list, Collections.singletonList(status)),
            hasJsonPath("$.processId", is(processId))
        );
    }

    public static Matcher<? super Object> matchProcess(String name, String userId,
                                                       List<DSpaceCommandLineParameter> list, ProcessStatus status) {
        return allOf(
            matchProcess(name, userId, list, Collections.singletonList(status))
        );
    }

    public static Matcher<? super Object> matchProcess(String name, String userId,
                                                       List<DSpaceCommandLineParameter> list,
                                                       List<ProcessStatus> statuses) {
        return allOf(
            matchProcess(name, userId),
            hasJsonPath("$.startTime", anyOf(any(String.class), nullValue())),
            hasJsonPath("$.endTime", anyOf(any(String.class), nullValue())),
            hasJsonPath("$.processStatus", Matchers.anyOf(
                statuses.stream().map(status -> is(status.toString())).collect(Collectors.toList())
            )),
            hasJsonPath("$.parameters", Matchers.containsInAnyOrder(
                list.stream().map(dSpaceCommandLineParameter -> ParameterValueMatcher
                    .matchParameterValue(dSpaceCommandLineParameter.getName(), dSpaceCommandLineParameter.getValue()))
                    .collect(Collectors.toList())
            ))
        );
    }

}
