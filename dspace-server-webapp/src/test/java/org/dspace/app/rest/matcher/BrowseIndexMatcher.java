/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_FLAT;
import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_HIERARCHICAL;
import static org.dspace.app.rest.model.BrowseIndexRest.BROWSE_TYPE_VALUE_LIST;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for a browse index
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class BrowseIndexMatcher {

    private BrowseIndexMatcher() { }

    public static Matcher<? super Object> subjectBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.subject.*")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_VALUE_LIST)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("text")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/subject")),
            hasJsonPath("$._links.entries.href", is(REST_SERVER_URL + "discover/browses/subject/entries")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/subject/items"))
        );
    }

    public static Matcher<? super Object> titleBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_FLAT)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("title")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/title")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/title/items"))
        );
    }

    public static Matcher<? super Object> contributorBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.contributor.*", "dc.creator")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_VALUE_LIST)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("text")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/author")),
            hasJsonPath("$._links.entries.href", is(REST_SERVER_URL + "discover/browses/author/entries")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/author/items"))
        );
    }

    public static Matcher<? super Object> dateIssuedBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.date.issued")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_FLAT)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.dataType", equalToIgnoringCase("date")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/dateissued")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/dateissued/items"))
        );
    }

    public static Matcher<? super Object> hierarchicalBrowseIndex(final String vocabulary) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.subject")),
            hasJsonPath("$.browseType", equalToIgnoringCase(BROWSE_TYPE_HIERARCHICAL)),
            hasJsonPath("$.type", equalToIgnoringCase("browse")),
            hasJsonPath("$.facetType", equalToIgnoringCase("subject")),
            hasJsonPath("$.vocabulary", equalToIgnoringCase(vocabulary)),
            hasJsonPath("$._links.vocabulary.href",
                        is(REST_SERVER_URL + String.format("submission/vocabularies/%s/", vocabulary))),
            hasJsonPath("$._links.items.href",
                        is(REST_SERVER_URL + String.format("discover/browses/%s/items", vocabulary))),
            hasJsonPath("$._links.entries.href",
                        is(REST_SERVER_URL + String.format("discover/browses/%s/entries", vocabulary))),
            hasJsonPath("$._links.self.href",
                        is(REST_SERVER_URL + String.format("discover/browses/%s", vocabulary)))
        );
    }
}
