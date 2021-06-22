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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.stream.Collectors;

import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.xmlworkflow.state.Step;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * @author Maria Verdonck (Atmire) on 13/01/2020
 */
public class WorkflowStepMatcher {

    private static final String WORKFLOW_ACTIONS_ENDPOINT
        = "/api/" + WorkflowStepRest.CATEGORY + "/" + WorkflowStepRest.NAME_PLURAL + "/";;

    private WorkflowStepMatcher() {}

    public static Matcher<? super Object> matchWorkflowStepEntry(Step workflowStep) {
        return allOf(
            hasJsonPath("$.id", is(workflowStep.getId())),
            hasJsonPath("$._links.self.href", containsString(WORKFLOW_ACTIONS_ENDPOINT + workflowStep.getId())),
            hasJsonPath("$._embedded.workflowactions._embedded.workflowactions", Matchers.containsInAnyOrder(
                workflowStep.getActions()
                    .stream()
                    .map(x -> WorkflowActionMatcher.matchWorkflowActionEntry(x))
                    .collect(Collectors.toList())
            ))
        );
    }


}
