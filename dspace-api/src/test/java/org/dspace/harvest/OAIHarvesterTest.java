/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

/**
 * Tests for OAIHarvester date processing logic.
 */
public class OAIHarvesterTest {

    @Test
    public void testTruncatedInstantProducesNoFractionalSeconds() {
        Instant withFraction = Instant.parse("2026-09-08T13:13:40.706Z");
        String formatted = DateTimeFormatter.ISO_INSTANT.format(withFraction.truncatedTo(ChronoUnit.SECONDS));
        assertEquals("2026-09-08T13:13:40Z", formatted);
    }

    @Test
    public void testTruncatedInstantDoesNotEndWithDot() {
        Instant withFraction = Instant.parse("2026-09-08T13:21:57.123Z");
        String formatted = DateTimeFormatter.ISO_INSTANT.format(withFraction.truncatedTo(ChronoUnit.SECONDS));
        assertFalse("Timestamp must not end with a dot", formatted.endsWith("."));
    }

    @Test
    public void testSubstringTruncationWithSecondGranularity() {
        String granularity = "YYYY-MM-DDThh:mm:ssZ";
        Instant withFraction = Instant.parse("2026-09-08T13:13:40.706Z");
        String formatted = DateTimeFormatter.ISO_INSTANT.format(withFraction.truncatedTo(ChronoUnit.SECONDS));
        String truncated = formatted.substring(0, granularity.length());
        assertEquals("2026-09-08T13:13:40Z", truncated);
    }
}
