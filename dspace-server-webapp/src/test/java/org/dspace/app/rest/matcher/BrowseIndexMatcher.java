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
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/dateissued")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/dateissued/items"))
        );
    }

    public static Matcher<? super Object> rodeptBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("oairecerif.author.affiliation", "oairecerif.editor.affiliation")),
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rodept")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rodept/items"))
        );
    }

    public static Matcher<? super Object> typeBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.type")),
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/type")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/type/items"))
        );
    }

    public static Matcher<? super Object> rpnameBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rpname")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rpname/items"))
        );
    }

    public static Matcher<? super Object> rpdeptBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("person.affiliation.name")),
            hasJsonPath("$.metadataBrowse", Matchers.is(true)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/rpdept")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/rpdept/items"))
        );
    }

    public static Matcher<? super Object> ounameBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/ouname")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/ouname/items"))
        );
    }

    public static Matcher<? super Object> pjtitleBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/pjtitle")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/pjtitle/items"))
        );
    }

    public static Matcher<? super Object> eqtitleBrowseIndex(final String order) {
        return allOf(
            hasJsonPath("$.metadata", contains("dc.title")),
            hasJsonPath("$.metadataBrowse", Matchers.is(false)),
            hasJsonPath("$.order", equalToIgnoringCase(order)),
            hasJsonPath("$.sortOptions[*].name", containsInAnyOrder("title", "dateissued", "dateaccessioned")),
            hasJsonPath("$._links.self.href", is(REST_SERVER_URL + "discover/browses/eqtitle")),
            hasJsonPath("$._links.items.href", is(REST_SERVER_URL + "discover/browses/eqtitle/items"))
                    );
    }
}
