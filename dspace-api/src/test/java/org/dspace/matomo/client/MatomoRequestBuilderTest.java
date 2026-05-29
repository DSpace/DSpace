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

import org.dspace.AbstractUnitTest;
import org.dspace.matomo.model.MatomoRequestDetails;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class MatomoRequestBuilderTest extends AbstractUnitTest {

    MatomoRequestBuilder matomoRequestBuilder;

    @Before
    public void setUp() throws Exception {
        matomoRequestBuilder = new MatomoRequestBuilder();
    }

    @Test
    public void testNull() throws JSONException {
        matomoRequestBuilder = new MatomoRequestBuilder();
        String json = matomoRequestBuilder.buildJSON(null);
        assertThat(json, emptyOrNullString());
    }

    @Test
    public void testEmptyRequests() throws JSONException {
        matomoRequestBuilder = new MatomoRequestBuilder();
        String json = matomoRequestBuilder.buildJSON(new MatomoBulkRequest("my-token", List.of()));
        JSONObject jsonObject = new JSONObject(json);
        assertThat(jsonObject.getString("token_auth"), is("my-token"));

        assertThat(jsonObject.has("requests"), is(true));

        JSONArray requests = jsonObject.getJSONArray("requests");
        assertThat(requests, notNullValue());
        assertThat(requests.length(), is(0));
    }

    @Test
    public void testNullRequests() throws JSONException {
        matomoRequestBuilder = new MatomoRequestBuilder();
        String json = matomoRequestBuilder.buildJSON(new MatomoBulkRequest("my-token", null));
        JSONObject jsonObject = new JSONObject(json);
        assertThat(jsonObject.getString("token_auth"), is("my-token"));

        assertThat(jsonObject.has("requests"), is(false));
    }


    @Test
    public void testSinglelRequest() throws JSONException {
        matomoRequestBuilder = new MatomoRequestBuilder();
        MatomoRequestDetails e1 =
            new MatomoRequestDetails()
                .addParameter("rec", "1")
                .addParameter("idsite", "1")
                .addParameter("action_name", "my-action");
        String json = matomoRequestBuilder.buildJSON(new MatomoBulkRequest("my-token", List.of(e1)));
        JSONObject jsonObject = new JSONObject(json);
        assertThat(jsonObject.getString("token_auth"), is("my-token"));

        assertThat(jsonObject.has("requests"), is(true));

        JSONArray jsonArray = jsonObject.getJSONArray("requests");
        assertThat(jsonArray.length(), is(1));

        String requestUrl = jsonArray.getString(0);
        assertThat(requestUrl, startsWith("?"));

        String[] parameters = requestUrl.substring(1).split("&");
        assertThat(
            List.of(parameters),
            containsInAnyOrder("rec=1", "idsite=1", "action_name=my-action")
        );
    }

    @Test
    public void testMultipleRequests() throws JSONException {
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
        JSONObject jsonObject = new JSONObject(json);
        assertThat(jsonObject.getString("token_auth"), is("my-token"));

        assertThat(jsonObject.has("requests"), is(true));

        JSONArray jsonArray = jsonObject.getJSONArray("requests");
        assertThat(jsonArray.length(), is(2));


        List<Object> list = jsonArray.toList();
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