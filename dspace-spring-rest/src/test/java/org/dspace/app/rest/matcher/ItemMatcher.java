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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import org.dspace.content.Item;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an item
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class ItemMatcher {

    public static Matcher<? super Object> matchItemWithTitleAndDateIssued(Item item, String title, String dateIssued) {
        return allOf(
                //Check item properties
                matchItemProperties(item),

                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.metadata[?(@.key=='dc.title')].value", contains(title)),
                hasJsonPath("$.metadata[?(@.key=='dc.date.issued')].value", contains(dateIssued)),

                //Check links
                matchItemLinks(item)
        );
    }

    public static Matcher<? super Object> matchItemLinks(Item item) {
        return allOf(
                hasJsonPath("$._links.self.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.bitstreams.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.owningCollection.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.templateItemOf.href", startsWith(REST_SERVER_URL)),
                hasJsonPath("$._links.self.href", startsWith(REST_SERVER_URL))
        );
    }

    public static Matcher<? super Object> matchItemProperties(Item item) {
        return allOf(
                hasJsonPath("$.uuid", is(item.getID().toString())),
                hasJsonPath("$.name", is(item.getName())),
                hasJsonPath("$.handle", is(item.getHandle())),
                hasJsonPath("$.inArchive", is(item.isArchived())),
                hasJsonPath("$.discoverable", is(item.isDiscoverable())),
                hasJsonPath("$.withdrawn", is(item.isWithdrawn())),
                hasJsonPath("$.lastModified", is(notNullValue())),
                hasJsonPath("$.type", is("item"))
        );
    }

}
