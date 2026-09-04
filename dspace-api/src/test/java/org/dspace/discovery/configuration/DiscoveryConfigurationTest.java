/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import static org.dspace.discovery.configuration.DiscoveryConfiguration.DEFAULT_SPELL_CHECK_COUNT;
import static org.dspace.discovery.configuration.DiscoveryConfiguration.MAX_SPELL_CHECK_COUNT;
import static org.dspace.discovery.configuration.DiscoveryConfiguration.MIN_SPELL_CHECK_COUNT;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link DiscoveryConfiguration}, focused on the spellcheck count boundaries.
 */
public class DiscoveryConfigurationTest {

    private final DiscoveryConfiguration configuration = new DiscoveryConfiguration();

    @Test
    public void testSpellCheckCountDefaultValue() {
        assertEquals(DEFAULT_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());
    }

    @Test
    public void testSpellCheckCountWithinBoundsIsKept() {
        configuration.setSpellCheckCount(MIN_SPELL_CHECK_COUNT);
        assertEquals(MIN_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());

        configuration.setSpellCheckCount(5);
        assertEquals(5, configuration.getSpellCheckCount());

        configuration.setSpellCheckCount(MAX_SPELL_CHECK_COUNT);
        assertEquals(MAX_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());
    }

    @Test
    public void testSpellCheckCountIsClampedToLowerBound() {
        configuration.setSpellCheckCount(0);
        assertEquals(MIN_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());

        configuration.setSpellCheckCount(-1);
        assertEquals(MIN_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());

        configuration.setSpellCheckCount(Integer.MIN_VALUE);
        assertEquals(MIN_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());
    }

    @Test
    public void testSpellCheckCountIsClampedToUpperBound() {
        configuration.setSpellCheckCount(MAX_SPELL_CHECK_COUNT + 1);
        assertEquals(MAX_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());

        configuration.setSpellCheckCount(999999999);
        assertEquals(MAX_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());

        configuration.setSpellCheckCount(Integer.MAX_VALUE);
        assertEquals(MAX_SPELL_CHECK_COUNT, configuration.getSpellCheckCount());
    }
}
