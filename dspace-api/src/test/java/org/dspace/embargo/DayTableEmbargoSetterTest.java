/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.time.Duration;
import java.time.ZonedDateTime;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DayTableEmbargoSetter#parseTerms}. The terms parsing
 * only depends on configuration ('embargo.terms.open' and 'embargo.terms.days'),
 * not on the Context or Item, so these tests run against the test kernel
 * without a database.
 */
public class DayTableEmbargoSetterTest extends AbstractUnitTest {

    private static final String TERMS_OPEN_PROPERTY = "embargo.terms.open";
    private static final String TERMS_DAYS_PROPERTY = "embargo.terms.days";

    /**
     * Tolerance when comparing computed lift dates against "now + N days",
     * to absorb the time that passes while the test runs.
     */
    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    private ConfigurationService configurationService;
    private DayTableEmbargoSetter embargoSetter;

    private Object previousTermsOpen;
    private Object previousTermsDays;

    @Before
    public void setUp() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        previousTermsOpen = configurationService.getPropertyValue(TERMS_OPEN_PROPERTY);
        previousTermsDays = configurationService.getPropertyValue(TERMS_DAYS_PROPERTY);

        configurationService.setProperty(TERMS_OPEN_PROPERTY, "forever");
        configurationService.setProperty(TERMS_DAYS_PROPERTY,
                                         new String[] {"90 days:90", "6 months:180", "1 year:365"});
        embargoSetter = new DayTableEmbargoSetter();
    }

    @After
    public void tearDown() {
        configurationService.setProperty(TERMS_OPEN_PROPERTY, previousTermsOpen);
        configurationService.setProperty(TERMS_DAYS_PROPERTY, previousTermsDays);
    }

    /**
     * Terms matching the configured 'embargo.terms.open' value mean a
     * permanent embargo: the lift date is the constant FOREVER date rather
     * than a computed one.
     */
    @Test
    public void parseTermsReturnsForeverForOpenTerms() throws SQLException, AuthorizeException {
        DCDate result = embargoSetter.parseTerms(null, null, "forever");
        assertEquals("Open terms should return the FOREVER date",
                     EmbargoServiceImpl.FOREVER.toString(), result.toString());
    }

    /**
     * Terms found in the 'embargo.terms.days' table produce a lift date the
     * configured number of days from now.
     */
    @Test
    public void parseTermsComputesLiftDateFromDayTable() throws SQLException, AuthorizeException {
        DCDate result = embargoSetter.parseTerms(null, null, "90 days");
        assertNotNull("Terms in the day table should produce a lift date", result);

        ZonedDateTime expected = ZonedDateTime.now().plusDays(90);
        Duration difference = Duration.between(expected, result.toDate()).abs();
        assertTrue("Lift date should be 90 days from now, but was " + result.toDate()
                       + " (off by " + difference + ")",
                   difference.compareTo(TOLERANCE) <= 0);
    }

    /**
     * Every entry in the day table maps to its own day count: each configured
     * terms string yields a lift date that many days from now, independent of
     * the other entries.
     */
    @Test
    public void parseTermsComputesLiftDateForEachConfiguredEntry() throws SQLException, AuthorizeException {
        String[][] termsAndDays = {{"6 months", "180"}, {"1 year", "365"}};
        for (String[] entry : termsAndDays) {
            DCDate result = embargoSetter.parseTerms(null, null, entry[0]);
            assertNotNull("Terms '" + entry[0] + "' should produce a lift date", result);

            ZonedDateTime expected = ZonedDateTime.now().plusDays(Long.parseLong(entry[1]));
            Duration difference = Duration.between(expected, result.toDate()).abs();
            assertTrue("Lift date for '" + entry[0] + "' should be " + entry[1]
                           + " days from now, but was " + result.toDate(),
                       difference.compareTo(TOLERANCE) <= 0);
        }
    }

    /**
     * Terms text absent from the day table produces no lift date.
     */
    @Test
    public void parseTermsReturnsNullForUnknownTerms() throws SQLException, AuthorizeException {
        assertNull("Terms not in the day table should return null",
                   embargoSetter.parseTerms(null, null, "3 fortnights"));
    }

    /**
     * An item with no embargo terms metadata is not embargoed.
     */
    @Test
    public void parseTermsReturnsNullForNullTerms() throws SQLException, AuthorizeException {
        assertNull("Null terms should return null",
                   embargoSetter.parseTerms(null, null, null));
    }

    /**
     * Whitespace around the terms or the day count in a configured
     * 'terms:days' entry is ignored.
     */
    @Test
    public void parseTermsTrimsWhitespaceInDayTableEntries() throws SQLException, AuthorizeException {
        configurationService.setProperty(TERMS_DAYS_PROPERTY, new String[] {" 2 years : 730 "});

        DCDate result = embargoSetter.parseTerms(null, null, "2 years");
        assertNotNull("Day table entries with surrounding whitespace should still match", result);

        ZonedDateTime expected = ZonedDateTime.now().plusDays(730);
        Duration difference = Duration.between(expected, result.toDate()).abs();
        assertTrue("Lift date should be 730 days from now, but was " + result.toDate(),
                   difference.compareTo(TOLERANCE) <= 0);
    }
}
