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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

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
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
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
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
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
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
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
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.dataType", equalToIgnoringCase("date")),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/dateissued")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/dateissued/items"))
        );
    }

    public static Matcher<? super Object> publisherBrowseIndex(final String order) {
        return allOf(
                hasJsonPath("$.metadata", contains("dc.publisher")),
                hasJsonPath("$.metadataBrowse", Matchers.is(true)),
                hasJsonPath("$.dataType", equalToIgnoringCase("text")),
                hasJsonPath("$.order", equalToIgnoringCase(order)),
                hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
                hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/publisher")),
                hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/publisher/items"))
        );
    }

    public static Matcher<? super Object> languageBrowseIndex(final String order) {
        return allOf(
                hasJsonPath("$.metadata", contains("dc.language.iso")),
                hasJsonPath("$.metadataBrowse", Matchers.is(true)),
                hasJsonPath("$.dataType", equalToIgnoringCase("iso_lang")),
                hasJsonPath("$.order", equalToIgnoringCase(order)),
                hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
                hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/language")),
                hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/language/items"))
        );
    }

    public static Matcher<? super Object> itemtypeBrowseIndex(final String order) {
        return allOf(
                hasJsonPath("$.metadata", contains("dc.type")),
                hasJsonPath("$.metadataBrowse", Matchers.is(true)),
                hasJsonPath("$.dataType", equalToIgnoringCase("text")),
                hasJsonPath("$.order", equalToIgnoringCase(order)),
                hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
                hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/itemtype")),
                hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/itemtype/items"))
        );
    }

    public static Matcher<? super Object> rightsBrowseIndex(final String order) {
        return allOf(
                hasJsonPath("$.metadata", contains("dc.rights.label")),
                hasJsonPath("$.metadataBrowse", Matchers.is(true)),
                hasJsonPath("$.dataType", equalToIgnoringCase("text")),
                hasJsonPath("$.order", equalToIgnoringCase(order)),
                hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
                hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rights")),
                hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rights/items"))
        );
    }
}
