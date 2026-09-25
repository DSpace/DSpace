/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;

import org.dspace.content.authority.Choices;
import org.junit.Test;

/**
 * Pure-POJO unit tests for the authority-elision read/write seam on {@link MetadataValue}
 * These assert the derived-authority contract without a database:
 * <ul>
 *   <li>the public {@link MetadataValue#getAuthority()} derives the UUID from {@code right_id}
 *       when the raw column is null but the relationship endpoints are set;</li>
 *   <li>the package-private {@link MetadataValue#getRawAuthority()} always returns the literal
 *       column;</li>
 *   <li>blanking the authority on an internal-backed value (librarian clear) nulls both endpoints
 *       and resets confidence, while keeping the display value;</li>
 *   <li>{@link MetadataValue#elideAuthorityColumn()} nulls only the column and leaves the
 *       endpoints intact.</li>
 * </ul>
 * The three null-{@code authority} end-states are told apart purely by whether {@code right_id}
 * is set — no flag, no confidence heuristic.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class MetadataValueAuthorityDerivationTest {

    private MetadataValue newValue() {
        // dSpaceObject stays null, so setAuthority() does not try to mark an owner modified:
        // this keeps the test a pure POJO exercise of the derivation logic.
        return new MetadataValue();
    }

    @Test
    public void rawNonNullAuthorityIsReturnedVerbatim() {
        MetadataValue mv = newValue();
        mv.setAuthority("0000-0002-1825-0097");

        assertThat(mv.getRawAuthority(), equalTo("0000-0002-1825-0097"));
        assertThat(mv.getAuthority(), equalTo("0000-0002-1825-0097"));
    }

    @Test
    public void rawNullWithEndpointsSetDerivesTheRightIdUuid() {
        MetadataValue mv = newValue();
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        mv.setLeftItem(owner).setRightItem(target);
        mv.elideAuthorityColumn();

        assertThat("raw column is physically null after elision", mv.getRawAuthority(), nullValue());
        assertThat("getter reconstitutes the UUID from right_id", mv.getAuthority(), equalTo(target.toString()));
    }

    @Test
    public void rawNullWithoutEndpointsReturnsNull() {
        MetadataValue mv = newValue();

        assertThat(mv.getRawAuthority(), nullValue());
        assertThat(mv.getAuthority(), nullValue());
    }

    @Test
    public void elideNullsOnlyTheColumnAndLeavesEndpoints() {
        MetadataValue mv = newValue();
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        mv.setLeftItem(owner).setRightItem(target);
        mv.setAuthority(target.toString());

        mv.elideAuthorityColumn();

        assertThat(mv.getRawAuthority(), nullValue());
        assertThat(mv.getLeftItem(), equalTo(owner));
        assertThat(mv.getRightItem(), equalTo(target));
        assertThat("endpoints still present, so the value is still relationship-backed",
                   mv.isRelationshipBacked(), is(true));
    }

    @Test
    public void blankSetWithEndpointsSetTearsDownTheRelationshipAndResetsConfidence() {
        MetadataValue mv = newValue();
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        mv.setLeftItem(owner).setRightItem(target);
        mv.setConfidence(Choices.CF_ACCEPTED);
        mv.setValue("Smith, John");

        // Librarian clear: blank the authority on an internal-backed value.
        mv.setAuthority("");

        assertThat("both endpoints nulled so Hibernate deletes the secondary row",
                   mv.isRelationshipBacked(), is(false));
        assertThat(mv.getLeftItem(), nullValue());
        assertThat(mv.getRightItem(), nullValue());
        assertThat("confidence reset to unset", mv.getConfidence(), is(Choices.CF_UNSET));
        assertThat("raw column cleared", mv.getRawAuthority(), nullValue());
        assertThat("derived getter now returns null (nothing to reconstitute from)",
                   mv.getAuthority(), nullValue());
        assertThat("display value is kept", mv.getValue(), equalTo("Smith, John"));
    }

    @Test
    public void blankSetWithoutEndpointsJustClearsTheColumn() {
        MetadataValue mv = newValue();
        mv.setConfidence(Choices.CF_ACCEPTED);
        mv.setValue("Free text");

        mv.setAuthority("  ");

        assertThat(mv.getRawAuthority(), nullValue());
        assertThat(mv.getAuthority(), nullValue());
        // No relationship existed, so there is nothing to tear down; confidence is left as the
        // caller set it (the librarian-clear teardown only fires when right_id was set).
        assertThat(mv.getConfidence(), is(Choices.CF_ACCEPTED));
        assertThat(mv.getValue(), equalTo("Free text"));
    }

    @Test
    public void retargetToANewNonBlankAuthorityDoesNotTearDownEndpoints() {
        MetadataValue mv = newValue();
        UUID owner = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        mv.setLeftItem(owner).setRightItem(target);

        // Retarget is a non-blank set: the coupled teardown must NOT fire here. Reconcile will
        // repoint the endpoints; the setter only handles the blank (clear) case.
        UUID newTarget = UUID.randomUUID();
        mv.setAuthority(newTarget.toString());

        assertThat(mv.getRawAuthority(), equalTo(newTarget.toString()));
        assertThat("endpoints untouched by a non-blank set", mv.isRelationshipBacked(), is(true));
        assertThat(mv.getRightItem(), equalTo(target));
    }
}
