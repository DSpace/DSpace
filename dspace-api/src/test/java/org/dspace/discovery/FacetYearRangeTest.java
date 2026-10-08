/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.junit.Test;

/**
 * Tests for the year range that {@link FacetYearRange} recovers from previously selected filter
 * queries.
 *
 * @author Bram Luyten (bram at atmire.com)
 */
public class FacetYearRangeTest {

    private static final String FACET = "dateIssued";

    /**
     * Resolve the range from the given filter queries. Every case below finds its range or its
     * single year in the filter queries themselves, so the search index is never consulted and
     * the context, scope, search service and parent query are never dereferenced.
     *
     * @param filterQueries the filter queries to resolve
     * @return the resolved range
     * @throws SearchServiceException never, since the search index is not consulted
     */
    private FacetYearRange rangeOf(String... filterQueries) throws SearchServiceException {
        DiscoverySearchFilterFacet facet = new DiscoverySearchFilterFacet();
        facet.setIndexFieldName(FACET);
        FacetYearRange range = new FacetYearRange(facet);
        range.calculateRange(null, Arrays.asList(filterQueries), null, null, null);
        return range;
    }

    @Test
    public void resolvesRange() throws Exception {
        FacetYearRange range = rangeOf(FACET + ".year:[2000 TO 2010]");
        assertEquals(2000, range.getOldestYear());
        assertEquals(2010, range.getNewestYear());
    }

    @Test
    public void resolvesRangePaddedWithSpaces() throws Exception {
        FacetYearRange range = rangeOf(FACET + ".year:[ 2000 TO 2010 ]");
        assertEquals(2000, range.getOldestYear());
        assertEquals(2010, range.getNewestYear());
    }

    @Test
    public void resolvesRangeWithRepeatedSpaces() throws Exception {
        FacetYearRange range = rangeOf(FACET + ".year:[2000  TO  2010]");
        assertEquals(2000, range.getOldestYear());
        assertEquals(2010, range.getNewestYear());
    }

    @Test
    public void resolvesRangeWithNegativeYears() throws Exception {
        FacetYearRange range = rangeOf(FACET + ".year:[-500 TO -100]");
        assertEquals(-500, range.getOldestYear());
        assertEquals(-100, range.getNewestYear());
    }

    @Test
    public void resolvesSingleYear() throws Exception {
        FacetYearRange range = rangeOf(FACET + ".year:2005 OR " + FACET + ".year:2006");
        assertEquals(2005, range.getOldestYear());
        assertEquals(2005, range.getNewestYear());
    }

    /**
     * A filter query that opens a range and never closes it has to be rejected promptly. The
     * wildcard pattern used previously needed time quadratic in the length of the value, where
     * recognising it as "not a range" is now linear. The bound is therefore a very wide margin
     * and is not sensitive to a slow CI machine.
     */
    @Test(timeout = 5000)
    public void rejectsUnclosedRangeWithoutBacktracking() throws Exception {
        FacetYearRange range = rangeOf(FACET + ".year:2005 OR [" + " TO ".repeat(60000) + "x");
        assertEquals(2005, range.getOldestYear());
        assertEquals(2005, range.getNewestYear());
    }
}
