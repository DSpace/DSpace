/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.dspace.AbstractUnitTest;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class MatomoRequestBuilderTest extends AbstractUnitTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    MatomoRequestBuilder matomoRequestBuilder;

    @BeforeEach
    public void setUp() throws Exception {
        matomoRequestBuilder = new MatomoRequestBuilder();
    }

    @Test
    public void testNull() {
        matomoRequestBuilder = new MatomoRequestBuilder();
        String json = matomoRequestBuilder.buildJSON(null);
        assertThat(json, emptyOrNullString());
    }

    @Test
    public void testEmptyRequests() {
        matomoRequestBuilder = new MatomoRequestBuilder();
        String json = matomoRequestBuilder.buildJSON(new MatomoBulkRequest("my-token", List.of()));
        JsonNode jsonObject = MAPPER.readTree(json);
        assertThat(jsonObject.get("token_auth").asString(), is("my-token"));

        assertThat(jsonObject.has("requests"), is(true));

        JsonNode requests = jsonObject.get("requests");
        assertThat(requests, notNullValue());
        assertThat(requests.size(), is(0));
    }

    @Test
    public void testNullRequests() {
        matomoRequestBuilder = new MatomoRequestBuilder();
        String json = matomoRequestBuilder.buildJSON(new MatomoBulkRequest("my-token", null));
        JsonNode jsonObject = MAPPER.readTree(json);
        assertThat(jsonObject.get("token_auth").asString(), is("my-token"));

        assertThat(jsonObject.has("requests"), is(false));
    }


    @Test
    public void testSinglelRequest() {
        matomoRequestBuilder = new MatomoRequestBuilder();
        MatomoRequestDetails e1 =
            new MatomoRequestDetails()
                .addParameter("rec", "1")
                .addParameter("idsite", "1")
                .addParameter("action_name", "my-action");
        String json = matomoRequestBuilder.buildJSON(new MatomoBulkRequest("my-token", List.of(e1)));
        JsonNode jsonObject = MAPPER.readTree(json);
        assertThat(jsonObject.get("token_auth").asString(), is("my-token"));

        assertThat(jsonObject.has("requests"), is(true));

        JsonNode jsonArray = jsonObject.get("requests");
        assertThat(jsonArray.size(), is(1));

        String requestUrl = jsonArray.get(0).asString();
        assertThat(requestUrl, startsWith("?"));

        String[] parameters = requestUrl.substring(1).split("&");
        assertThat(
            List.of(parameters),
            containsInAnyOrder("rec=1", "idsite=1", "action_name=my-action")
        );
    }

    @Test
    public void testMultipleRequests() {
        matomoRequestBuilder = new MatomoRequestBuilder();
        MatomoRequestDetails e1 =
            new MatomoRequestDetails()
                .addParameter("rec", "1")
                .addParameter("idsite", "1")
                .addParameter("action_name", "my-first-action");
        MatomoRequestDetails e2 =
            new MatomoRequestDetails()
                .addParameter("rec", "1")
                .addParameter("idsite", "1")
                .addParameter("action_name", "my-second-action");
        String json = matomoRequestBuilder.buildJSON(new MatomoBulkRequest("my-token", List.of(e1,e2)));
        JsonNode jsonObject = MAPPER.readTree(json);
        assertThat(jsonObject.get("token_auth").asString(), is("my-token"));

        assertThat(jsonObject.has("requests"), is(true));

        JsonNode jsonArray = jsonObject.get("requests");
        assertThat(jsonArray.size(), is(2));


        List<Object> list = StreamSupport.stream(jsonArray.spliterator(), false)
                                          .map(JsonNode::asString)
                                          .collect(Collectors.toList());
        assertThat(list.size(), is(2));
        assertThat(list,
            containsInAnyOrder(
                new UrlParameterMatcher("rec=1", "idsite=1", "action_name=my-second-action"),
                new UrlParameterMatcher("rec=1", "idsite=1", "action_name=my-first-action")
            )
        );
    }

    private static final class UrlParameterMatcher extends BaseMatcher<Object> {

        List<String> parameterList;

        public UrlParameterMatcher(String... parameters) {
            parameterList = List.of(parameters);
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof String s)) {
                return false;
            }
            return parameterList.stream().allMatch(s::contains);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an UrlParameterMatcher with the following params: ")
                       .appendValue(parameterList);
        }
    }

}