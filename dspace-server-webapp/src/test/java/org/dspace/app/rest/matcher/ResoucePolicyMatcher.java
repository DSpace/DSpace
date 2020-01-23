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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Constants;
import org.hamcrest.Matcher;

/**
 * Provide convenient org.hamcrest.Matcher to verify a ResourcePolicyRest json response
 * 
 * @author Mykhaylo Boychuk (4science.it)
 *
 */
public class ResoucePolicyMatcher {

    private ResoucePolicyMatcher() {
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

}
