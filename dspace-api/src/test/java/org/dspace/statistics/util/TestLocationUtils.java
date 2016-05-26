/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.util.Locale;
import org.dspace.AbstractDSpaceTest;
import static org.junit.Assert.*;

import org.dspace.core.I18nUtil;
import org.junit.Test;

/**
 * @author mwood
 */
public class TestLocationUtils extends AbstractDSpaceTest
{
    private static final String UNKNOWN_CONTINENT = I18nUtil
            .getMessage("org.dspace.statistics.util.LocationUtils.unknown-continent");
    private static final String UNKNOWN_COUNTRY = I18nUtil
            .getMessage("org.dspace.statistics.util.LocationUtils.unknown-country");

    /**
     * Test method for {@link org.dspace.statistics.util.LocationUtils#getContinentCode(java.lang.String)}.
     */
    @Test
    public void testGetContinentCode()
    {
        assertEquals(LocationUtils.getContinentCode("US"), "NA");
        assertTrue(LocationUtils.getContinentCode(null).length() > 2); // message
        assertTrue(LocationUtils.getContinentCode("xyz").length() > 2); // message
    }

    /**
     * Test method for {@link org.dspace.statistics.util.LocationUtils#getContinentName(java.lang.String)}.
     */
    @Test
    public void testGetContinentNameString()
    {
        assertEquals("North America", LocationUtils.getContinentName("NA"));
        assertEquals(UNKNOWN_CONTINENT, LocationUtils.getContinentName(null));
        assertEquals(UNKNOWN_CONTINENT, LocationUtils.getContinentName("XXXX"));
    }

    /**
     * Test method for {@link org.dspace.statistics.util.LocationUtils#getContinentName(java.lang.String, java.util.Locale)}.
     */
    @Test
    public void testGetContinentNameStringLocale()
    {
        assertEquals("North America", LocationUtils.getContinentName(
                "NA", Locale.ENGLISH));
    }

    /**
     * Test method for {@link org.dspace.statistics.util.LocationUtils#getCountryName(java.lang.String)}.
     */
    @Test
    public void testGetCountryNameString()
    {
        assertEquals(Locale.US.getDisplayCountry(), LocationUtils.getCountryName(
                "US"));
        assertEquals(UNKNOWN_COUNTRY, LocationUtils.getCountryName(null));
        assertEquals("XX", LocationUtils.getCountryName("XX"));
    }

    /**
     * Test method for {@link org.dspace.statistics.util.LocationUtils#getCountryName(java.lang.String, java.util.Locale)}.
     */
    @Test
    public void testGetCountryNameStringLocale()
    {
        assertEquals("United States", LocationUtils.getCountryName(
                "US", Locale.ENGLISH));
    }
}
