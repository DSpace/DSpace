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
import static org.hamcrest.Matchers.nullValue;

import org.dspace.content.Item;
import org.dspace.orcid.OrcidQueue;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an OrcidQueue
 *
 * @author Mykhaylo Boychuk (4science.it)
 *
 */
public class OrcidQueueMatcher {

    private OrcidQueueMatcher() {}

    public static Matcher<? super Object> matchOrcidQueue(OrcidQueue orcidQueue) {
        Item entity = orcidQueue.getEntity();
        return allOf(
                hasJsonPath("$.id", is(orcidQueue.getID())),
            hasJsonPath("$.profileItemId", is(orcidQueue.getProfileItem().getID().toString())),
                hasJsonPath("$.entityId", entity != null ? is(entity.getID().toString()) : nullValue()),
                hasJsonPath("$.description", is(orcidQueue.getDescription())),
                hasJsonPath("$.recordType", is(orcidQueue.getRecordType())),
                hasJsonPath("$.operation", is(orcidQueue.getOperation().name())),
                hasJsonPath("$.type", is("orcidqueue"))
        );
    }
}
