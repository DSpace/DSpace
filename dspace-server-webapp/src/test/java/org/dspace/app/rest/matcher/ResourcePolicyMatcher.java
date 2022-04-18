/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import javax.annotation.Nullable;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hamcrest.Matcher;

/**
 * Provide convenient org.hamcrest.Matcher to verify a ResourcePolicyRest json response
 *
 * @author Mykhaylo Boychuk (4science.it)
 *
 */
public class ResourcePolicyMatcher {

    private ResourcePolicyMatcher() {
    }

    public static Matcher<? super Object> matchResourcePolicyProperties(@Nullable Group group,
        @Nullable EPerson eperson, DSpaceObject dso, @Nullable String rpType, int action, @Nullable String name) {
        return allOf(
            hasJsonPath("$.name", is(name)),
            hasJsonPath("$.action", is(Constants.actionText[action])),
            rpType != null ?
                hasJsonPath("$.policyType", is(rpType)) :
                hasNoJsonPath("$.policyType"),
            hasJsonPath("$.type", is("resourcepolicy")),
            hasJsonPath("$._embedded.resource.uuid", is(dso.getID().toString())),
            eperson != null ?
                hasJsonPath("$._embedded.eperson.id",
                    is(eperson.getID().toString())) :
                hasJsonPath("$._embedded.eperson", nullValue()),
            group != null ?
                hasJsonPath("$._embedded.group.id",
                    is(group.getID().toString())) :
                hasJsonPath("$._embedded.group", nullValue())
                    );
    }

    public static Matcher<? super Object> matchResourcePolicy(ResourcePolicy resourcePolicy) {
        return allOf(hasJsonPath("$.id", is(resourcePolicy.getID())),
                hasJsonPath("$.name", is(resourcePolicy.getRpName())),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(resourcePolicy.getStartDate())),
                hasJsonPath("$.endDate", is(resourcePolicy.getEndDate())),
                resourcePolicy.getRpType() != null ?
                        hasJsonPath("$.policyType", is(resourcePolicy.getRpType())) :
                               hasNoJsonPath("$.policyType"),
                hasJsonPath("$.type", is("resourcepolicy")),
                hasJsonPath("$._embedded.resource.id", is(resourcePolicy.getdSpaceObject().getID().toString())),
                resourcePolicy.getEPerson() != null ?
                               hasJsonPath("$._embedded.eperson.id",
                                        is(resourcePolicy.getEPerson().getID().toString())) :
                               hasJsonPath("$._embedded.eperson", nullValue()),
                resourcePolicy.getGroup() != null ?
                               hasJsonPath("$._embedded.group.id",
                                        is(resourcePolicy.getGroup().getID().toString())) :
                               hasJsonPath("$._embedded.group", nullValue())
                        );
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "eperson",
                "group",
                "resource"
        );
    }

}
