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

import org.dspace.app.rest.model.WorkflowActionRest;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.hamcrest.Matcher;

/**
 * @author Maria Verdonck (Atmire) on 06/01/2020
 */
public class WorkflowActionMatcher {

    private static final String WORKFLOW_ACTIONS_ENDPOINT
            = "/api/" + WorkflowActionRest.CATEGORY + "/" + WorkflowActionRest.NAME_PLURAL + "/";

    private WorkflowActionMatcher() {

    }

    public static Matcher<? super Object> matchWorkflowActionEntry(WorkflowActionConfig workflowAction) {
        return allOf(
                hasJsonPath("$.id", is(workflowAction.getId())),
                hasJsonPath("$.options", is(workflowAction.getOptions())),
                hasJsonPath("$._links.self.href", containsString(WORKFLOW_ACTIONS_ENDPOINT + workflowAction.getId()))
        );
    }
}
