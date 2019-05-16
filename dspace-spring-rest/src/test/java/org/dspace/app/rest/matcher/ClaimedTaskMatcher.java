/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for a Claimed Task
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class ClaimedTaskMatcher {

    private ClaimedTaskMatcher() { }

    /**
     * Check if the returned json expose all the required links and properties
     * 
     * @param ptask
     *            the pool task
     * @param step
     *            the step name
     * @return
     */
    public static Matcher matchClaimedTask(ClaimedTask cTask, String step) {
        return allOf(
                hasJsonPath("$.step", is(step)),
                // Check workflowitem properties
                matchProperties(cTask),
                // Check links
                matchLinks(cTask));
    }

    /**
     * Check that the id and type are exposed
     * 
     * @param cTask
     *            the claimed task, if empty only the type will be checked
     * @return
     */
    public static Matcher<? super Object> matchProperties(ClaimedTask cTask) {
        return allOf(
                cTask != null ? hasJsonPath("$.id", is(cTask.getID())) : hasJsonPath("$.id"),
                hasJsonPath("$.type", is("claimedtask"))
        );
    }

    /**
     * Check that the required links are present
     * 
     * @param cTask
     *            the claimed task, if empty only the generic links structure will be checked
     * @return
     */
    public static Matcher<? super Object> matchLinks(ClaimedTask cTask) {
        return allOf(
                cTask != null
                        ? hasJsonPath("$._links.self.href",
                                is(REST_SERVER_URL + "workflow/claimedtasks/" + cTask.getID()))
                        : hasJsonPath("$._links.self.href"),
                hasJsonPath("$._links.owner.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.workflowitem.href", startsWith(REST_SERVER_URL)));
    }
}
