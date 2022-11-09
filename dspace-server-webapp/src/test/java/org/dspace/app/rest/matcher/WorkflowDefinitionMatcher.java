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

import java.util.UUID;

import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * @author Maria Verdonck (Atmire) on 03/01/2020
 */
public class WorkflowDefinitionMatcher {

    private static XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

    private static final String WORKFLOW_DEFINITIONS_ENDPOINT
            = "/api/" + WorkflowDefinitionRest.CATEGORY + "/" + WorkflowDefinitionRest.NAME_PLURAL + "/";

    private WorkflowDefinitionMatcher() {
    }

    public static Matcher<? super Object> matchWorkflowDefinitionEntry(Workflow workflow) {
        return allOf(
                hasJsonPath("$.name", is(workflow.getID())),
                hasJsonPath("$.isDefault", is(xmlWorkflowFactory.isDefaultWorkflow(workflow.getID()))),
                hasJsonPath("$._links.self.href", containsString(WORKFLOW_DEFINITIONS_ENDPOINT + workflow.getID()))
        );
    }

    public static Matcher<? super Object> matchWorkflowOnWorkflowName(String workflowName) {
        return allOf(
                hasJsonPath("$.name", is(workflowName)),
                hasJsonPath("$.isDefault", is(xmlWorkflowFactory.isDefaultWorkflow(workflowName))),
                hasJsonPath("$._links.self.href", containsString(WORKFLOW_DEFINITIONS_ENDPOINT + workflowName))
        );
    }

    public static Matcher<? super Object> matchCollectionEntry(String name, UUID uuid, String handle) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.handle", is(handle)),
                hasJsonPath("$.type", is("collection")),
                hasJsonPath("$.metadata", Matchers.allOf(
                        MetadataMatcher.matchMetadata("dc.title", name)
                ))
        );
    }

    /**
     * Verifies that the content of the `json` matches
     * the detail of the steps
     * Actually we can checks only the identifier to assure they are the same.
     * 
     * @param step target step of the workflow
     * @return Matcher
     */
    public static Matcher<? super Object> matchStep(Step step) {
        return allOf(
                hasJsonPath("$.id", is(step.getId()))
        );
    }
}
