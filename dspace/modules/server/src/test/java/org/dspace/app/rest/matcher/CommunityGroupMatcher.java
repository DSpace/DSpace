package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matcher;

public class CommunityGroupMatcher {

    private CommunityGroupMatcher() {
    }

    /**
     * Returns a Matcher for a CommunityGroup based on the given name
     *
     * @return a Matcher for a CommunityGroup based on the given name
     */
    public static Matcher<? super Object> matchCommunityGroupWithId(int id) {
        return allOf(
                hasJsonPath("$.id", is(id)),
                hasJsonPath("$.type", is("communitygroup")),
                hasJsonPath("$._links.self.href", containsString("/api/core/communitygroups/" + String.valueOf(id))),
                hasJsonPath("$._links.communities.href", endsWith("/communities")));
    }

}
