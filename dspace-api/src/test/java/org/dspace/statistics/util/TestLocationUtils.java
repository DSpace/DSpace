/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.dspace.AbstractDSpaceTest;
import org.junit.Test;

/**
 * @author mwood
 */
public class TestLocationUtils extends AbstractDSpaceTest {
    /**
     * Test method for {@link org.dspace.statistics.util.LocationUtils#getContinentCode(java.lang.String)}.
     */
    @Test
    public void testGetContinentCode() {
        assertEquals("NA", LocationUtils.getContinentCode("US"));
        assertTrue(LocationUtils.getContinentCode(null).length() > 2); // message
        assertTrue(LocationUtils.getContinentCode("xyz").length() > 2); // message
    }

    /**
     * Test method for
     * {@link org.dspace.statistics.util.LocationUtils#getContinentName(java.lang.String, java.util.Locale)}.
     */
    @Test
    public void testGetContinentNameStringLocale() {
        assertEquals("North America", LocationUtils.getContinentName(
            "NA", Locale.ENGLISH));
    }

    /**
     * Test method for
     * {@link org.dspace.statistics.util.LocationUtils#getCountryName(java.lang.String, java.util.Locale)}.
     */
    @Test
    public void testGetCountryNameStringLocale() {
        assertEquals("United States", LocationUtils.getCountryName(
            "US", Locale.ENGLISH));
    }
}
