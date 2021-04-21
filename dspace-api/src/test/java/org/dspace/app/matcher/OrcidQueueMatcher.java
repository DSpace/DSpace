/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import static org.hamcrest.Matchers.is;

import org.dspace.app.orcid.OrcidQueue;
import org.dspace.content.Item;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Implementation of {@link org.hamcrest.Matcher} to match a OrcidQueue by all
 * its attributes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueMatcher extends TypeSafeMatcher<OrcidQueue> {

    private final Matcher<Item> ownerMatcher;

    private final Matcher<Item> entityMatcher;

    private final Matcher<String> entityTypeMatcher;

    private final Matcher<String> putCodeMatcher;

    private OrcidQueueMatcher(Matcher<Item> ownerMatcher, Matcher<Item> entityMatcher,
        Matcher<String> entityTypeMatcher, Matcher<String> putCodeMatcher) {
        this.ownerMatcher = ownerMatcher;
        this.entityMatcher = entityMatcher;
        this.entityTypeMatcher = entityTypeMatcher;
        this.putCodeMatcher = putCodeMatcher;
    }

    public static OrcidQueueMatcher matches(Item owner, Item entity, String entityType, String putCode) {
        return new OrcidQueueMatcher(is(owner), is(entity), is(entityType), is(putCode));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an orcid queue record that with the following attributes:")
            .appendText(" item owner ").appendDescriptionOf(ownerMatcher)
            .appendText(", item entity ").appendDescriptionOf(entityMatcher)
            .appendText(", entity type ").appendDescriptionOf(entityTypeMatcher)
            .appendText(" and put code ").appendDescriptionOf(putCodeMatcher);
    }

    @Override
    protected boolean matchesSafely(OrcidQueue item) {
        return ownerMatcher.matches(item.getOwner())
            && entityMatcher.matches(item.getEntity())
            && entityTypeMatcher.matches(item.getEntityType())
            && putCodeMatcher.matches(item.getPutCode());
    }

}
