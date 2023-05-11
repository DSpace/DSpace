/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import static org.dspace.util.MultiFormatDateParser.parse;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Date;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link Matcher} to match a ResourcePolicy.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResourcePolicyMatcher extends TypeSafeMatcher<ResourcePolicy> {

    private final Matcher<Integer> actionId;

    private final Matcher<EPerson> ePerson;

    private final Matcher<Group> group;

    private final Matcher<String> rptype;

    private final Matcher<String> rpName;

    private final Matcher<String> description;

    private final Matcher<Date> startDate;

    private final Matcher<Date> endDate;

    public ResourcePolicyMatcher(Matcher<Integer> actionId, Matcher<EPerson> ePerson, Matcher<Group> group,
                                 Matcher<String> rpName, Matcher<String> rptype, Matcher<Date> startDate,
                                 Matcher<Date> endDate, Matcher<String> description) {
        this.actionId = actionId;
        this.ePerson = ePerson;
        this.group = group;
        this.rptype = rptype;
        this.rpName = rpName;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Resource policy with action id ").appendDescriptionOf(actionId)
            .appendText(" and EPerson ").appendDescriptionOf(ePerson)
            .appendText(" and Group ").appendDescriptionOf(group)
            .appendText(" and rpType ").appendDescriptionOf(rptype)
            .appendText(" and rpName ").appendDescriptionOf(rpName)
            .appendText(" and description ").appendDescriptionOf(this.description)
            .appendText(" and start date ").appendDescriptionOf(startDate)
            .appendText(" and end date ").appendDescriptionOf(endDate);
    }

    public static ResourcePolicyMatcher matches(int actionId, EPerson ePerson, String rptype) {
        return new ResourcePolicyMatcher(is(actionId), is(ePerson), nullValue(Group.class),
            any(String.class), is(rptype), any(Date.class), any(Date.class), any(String.class));
    }

    public static ResourcePolicyMatcher matches(int actionId, EPerson ePerson, String rpName, String rptype) {
        return new ResourcePolicyMatcher(is(actionId), is(ePerson), nullValue(Group.class),
            is(rpName), is(rptype), any(Date.class), any(Date.class), any(String.class));
    }

    public static ResourcePolicyMatcher matches(int actionId, Group group, String rptype) {
        return new ResourcePolicyMatcher(is(actionId), nullValue(EPerson.class), is(group),
            any(String.class), is(rptype), any(Date.class), any(Date.class), any(String.class));
    }

    public static ResourcePolicyMatcher matches(int actionId, Group group, String rpName, String rptype) {
        return new ResourcePolicyMatcher(is(actionId), nullValue(EPerson.class), is(group), is(rpName),
            is(rptype), any(Date.class), any(Date.class), any(String.class));
    }

    public static ResourcePolicyMatcher matches(int actionId, Group group, String rpName, String rptype,
        String description) {
        return new ResourcePolicyMatcher(is(actionId), nullValue(EPerson.class), is(group), is(rpName),
            is(rptype), any(Date.class), any(Date.class), is(description));
    }

    public static ResourcePolicyMatcher matches(int actionId, Group group, String rpName, String rpType, Date startDate,
        Date endDate, String description) {
        return new ResourcePolicyMatcher(is(actionId), nullValue(EPerson.class), is(group), is(rpName),
            is(rpType), is(startDate), is(endDate), is(description));
    }

    public static ResourcePolicyMatcher matches(int actionId, Group group, String rpName, String rpType,
        String startDate, String endDate, String description) {
        return matches(actionId, group, rpName, rpType, startDate != null ? parse(startDate) : null,
            endDate != null ? parse(endDate) : null, description);
    }

    @Override
    protected boolean matchesSafely(ResourcePolicy resourcePolicy) {
        return actionId.matches(resourcePolicy.getAction())
            && ePerson.matches(resourcePolicy.getEPerson())
            && group.matches(resourcePolicy.getGroup())
            && rptype.matches(resourcePolicy.getRpType())
            && rpName.matches(resourcePolicy.getRpName())
            && description.matches(resourcePolicy.getRpDescription())
            && startDate.matches(resourcePolicy.getStartDate())
            && endDate.matches(resourcePolicy.getEndDate());
    }

    private static <T> Matcher<T> any(Class<T> clazz) {
        return LambdaMatcher.matches((obj) -> true, "any value");
    }

}
