package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.hamcrest.Matcher;

public class CommunityGroupMatcher {

    private CommunityGroupMatcher() {
    }

    /**
     * Returns a Matcher for a CommunityGroup based on the given name
     * 
     * @param shortName the shortName of CommunityGroup being matched
     * @return a Matcher for a CommunityGroup based on the given name
     */
    public static Matcher<? super Object> matchCommunityGroupWithId(int id) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.type", is("communitygroup")),
                hasJsonPath("$._links.self.href", containsString("/api/eperson/communitygroups/")),
                hasJsonPath("$._links.communities.href", endsWith("/communities")));
    }

}
