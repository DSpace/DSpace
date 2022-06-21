/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.matcher;

import static org.hamcrest.Matchers.is;

import org.dspace.content.Item;
import org.dspace.orcid.OrcidOperation;
import org.dspace.orcid.OrcidQueue;
import org.hamcrest.BaseMatcher;
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

    private final Matcher<Item> profileItemMatcher;

    private final Matcher<Item> entityMatcher;

    private final Matcher<String> recordTypeMatcher;

    private final Matcher<String> putCodeMatcher;

    private final Matcher<String> descriptionMatcher;

    private final Matcher<String> metadataMatcher;

    private final Matcher<OrcidOperation> operationMatcher;

    private final Matcher<Integer> attemptsMatcher;

    private OrcidQueueMatcher(Matcher<Item> profileItemMatcher, Matcher<Item> entityMatcher,
        Matcher<String> recordTypeMatcher, Matcher<String> putCodeMatcher, Matcher<String> metadataMatcher,
        Matcher<String> descriptionMatcher, Matcher<OrcidOperation> operationMatcher,
        Matcher<Integer> attemptsMatcher) {
        this.profileItemMatcher = profileItemMatcher;
        this.entityMatcher = entityMatcher;
        this.recordTypeMatcher = recordTypeMatcher;
        this.putCodeMatcher = putCodeMatcher;
        this.metadataMatcher = metadataMatcher;
        this.descriptionMatcher = descriptionMatcher;
        this.operationMatcher = operationMatcher;
        this.attemptsMatcher = attemptsMatcher;
    }

    public static OrcidQueueMatcher matches(Item profileItem, Item entity, String recordType,
        OrcidOperation operation) {
        return new OrcidQueueMatcher(is(profileItem), is(entity), is(recordType), anything(),
            anything(), anything(), is(operation), anything());
    }

    public static OrcidQueueMatcher matches(Item profileItem, Item entity, String recordType,
        OrcidOperation operation, int attempts) {
        return new OrcidQueueMatcher(is(profileItem), is(entity), is(recordType), anything(),
            anything(), anything(), is(operation), is(attempts));
    }

    public static OrcidQueueMatcher matches(Item profileItem, Item entity, String recordType,
        String putCode, OrcidOperation operation) {
        return new OrcidQueueMatcher(is(profileItem), is(entity), is(recordType), is(putCode),
            anything(), anything(), is(operation), anything());
    }

    public static OrcidQueueMatcher matches(Item profileItem, Item entity, String recordType,
        String putCode, String metadata, String description, OrcidOperation operation) {
        return new OrcidQueueMatcher(is(profileItem), is(entity), is(recordType),
            is(putCode), is(metadata), is(description), is(operation), anything());
    }

    public static OrcidQueueMatcher matches(Item item, String recordType,
        String putCode, String metadata, String description, OrcidOperation operation) {
        return new OrcidQueueMatcher(is(item), is(item), is(recordType),
            is(putCode), is(metadata), is(description), is(operation), anything());
    }

    public static OrcidQueueMatcher matches(Item profileItem, Item entity, String recordType,
        String putCode, Matcher<String> metadata, String description, OrcidOperation operation) {
        return new OrcidQueueMatcher(is(profileItem), is(entity), is(recordType),
            is(putCode), metadata, is(description), is(operation), anything());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an orcid queue record that with the following attributes:")
            .appendText(" item profileItem ").appendDescriptionOf(profileItemMatcher)
            .appendText(", item entity ").appendDescriptionOf(entityMatcher)
            .appendText(", record type ").appendDescriptionOf(recordTypeMatcher)
            .appendText(", metadata ").appendDescriptionOf(metadataMatcher)
            .appendText(", description ").appendDescriptionOf(descriptionMatcher)
            .appendText(", operation ").appendDescriptionOf(operationMatcher)
            .appendText(", attempts ").appendDescriptionOf(attemptsMatcher)
            .appendText(" and put code ").appendDescriptionOf(putCodeMatcher);
    }

    @Override
    protected boolean matchesSafely(OrcidQueue item) {
        return profileItemMatcher.matches(item.getProfileItem())
            && entityMatcher.matches(item.getEntity())
            && recordTypeMatcher.matches(item.getRecordType())
            && metadataMatcher.matches(item.getMetadata())
            && putCodeMatcher.matches(item.getPutCode())
            && descriptionMatcher.matches(item.getDescription())
            && operationMatcher.matches(item.getOperation())
            && attemptsMatcher.matches(item.getAttempts());
    }

    private static <T> Matcher<T> anything() {
        return new BaseMatcher<T>() {

            @Override
            public boolean matches(Object item) {
                return true;
            }

            @Override
            public void describeTo(Description description) {

            }
        };

    }

}
