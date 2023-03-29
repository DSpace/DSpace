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

import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for a Pool Task
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class PoolTaskMatcher {

    private PoolTaskMatcher() { }

    /**
     * Check if the returned json expose all the required links and properties
     * 
     * @param pTask
     *            the pool task
     * @param step
     *            the step name
     * @return
     */
    public static Matcher matchPoolTask(PoolTask pTask, String step) {
        return allOf(
                // Check workflowitem properties
                matchProperties(pTask),
                // Check links
                matchLinks(pTask));
    }

    /**
     * Check that the id and type are exposed
     * 
     * @param pTask
     *            the pool task, if empty only the type will be checked
     * @return
     */
    public static Matcher<? super Object> matchProperties(PoolTask pTask) {
        return allOf(
                pTask != null ? hasJsonPath("$.id", is(pTask.getID())) : hasJsonPath("$.id"),
                hasJsonPath("$.type", is("pooltask"))
        );
    }

    /**
     * Check that the required links are present
     * 
     * @param pTask
     *            the pool task, if empty only the generic links structure will be checked
     * @return
     */
    public static Matcher<? super Object> matchLinks(PoolTask pTask) {
        return allOf(
                pTask != null
                        ? hasJsonPath("$._links.self.href",
                                is(REST_SERVER_URL + "workflow/pooltasks/" + pTask.getID()))
                        : hasJsonPath("$._links.self.href"),
                hasJsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.workflowitem.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.group.href", startsWith(REST_SERVER_URL)));
    }
}
