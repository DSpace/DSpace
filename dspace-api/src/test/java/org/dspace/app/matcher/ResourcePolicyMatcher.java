/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import java.util.Objects;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link org.hamcrest.Matcher} to match a ResourcePolicy.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResourcePolicyMatcher extends TypeSafeMatcher<ResourcePolicy> {

    private final int actionId;

    private final EPerson ePerson;

    private final Group group;

    private final String rptype;

    private ResourcePolicyMatcher(int actionId, EPerson ePerson, Group group, String rptype) {
        this.actionId = actionId;
        this.ePerson = ePerson;
        this.group = group;
        this.rptype = rptype;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("ResourcePolicy with the following attributes [actionId=" + actionId + ", ePerson="
            + ePerson + ", group=" + group + ", rptype=" + rptype + "]");
    }

    public static ResourcePolicyMatcher matches(int actionId, EPerson ePerson, String rptype) {
        return new ResourcePolicyMatcher(actionId, ePerson, null, rptype);
    }

    public static ResourcePolicyMatcher matches(int actionId, Group group, String rptype) {
        return new ResourcePolicyMatcher(actionId, null, group, rptype);
    }

    @Override
    protected boolean matchesSafely(ResourcePolicy resourcePolicy) {
        return Objects.equals(resourcePolicy.getAction(), actionId)
            && Objects.equals(resourcePolicy.getRpType(), rptype)
            && Objects.equals(resourcePolicy.getEPerson(), ePerson)
            && Objects.equals(resourcePolicy.getGroup(), group);
    }

}
