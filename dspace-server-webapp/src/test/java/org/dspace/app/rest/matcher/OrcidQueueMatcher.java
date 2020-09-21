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

import org.dspace.app.orcid.OrcidQueue;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an OrcidQueue
 *
 * @author Mykhaylo Boychuk (4science.it)
 *
 */
public class OrcidQueueMatcher {

    private OrcidQueueMatcher() {}

    public static Matcher<? super Object> matchOrcidQueue(OrcidQueue orcidQueue, String entityType) {
        return allOf(
                hasJsonPath("$.id", is(orcidQueue.getID())),
                hasJsonPath("$.ownerId", is(orcidQueue.getOwner().getID().toString())),
                hasJsonPath("$.entityId", is(orcidQueue.getEntity().getID().toString())),
                hasJsonPath("$.entityName", is(orcidQueue.getEntity().getName())),
                hasJsonPath("$.entityType", is(entityType)),
                hasJsonPath("$.type", is("orcidqueue"))
        );
    }
}
