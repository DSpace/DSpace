package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class BrowseIndexMatchers {
    public BrowseIndexMatchers() {
    }

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
}