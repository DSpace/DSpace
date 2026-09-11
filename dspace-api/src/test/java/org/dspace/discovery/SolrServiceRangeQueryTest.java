/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link SolrServiceImpl#isRangeQuery(String)}, which decides whether a filter value is
 * passed to Solr unescaped.
 *
 * @author Bram Luyten (bram at atmire.com)
 */
public class SolrServiceRangeQueryTest {

    @Test
    public void acceptsRangeQueries() {
        assertTrue(SolrServiceImpl.isRangeQuery("[2000 TO 2010]"));
        assertTrue(SolrServiceImpl.isRangeQuery("[* TO *]"));
        assertTrue(SolrServiceImpl.isRangeQuery("[2000-01-01T00:00:00Z TO NOW]"));
        assertTrue(SolrServiceImpl.isRangeQuery("[TO]"));
    }

    @Test
    public void rejectsValuesThatAreNotRangeQueries() {
        assertFalse(SolrServiceImpl.isRangeQuery(""));
        assertFalse(SolrServiceImpl.isRangeQuery("[]"));
        assertFalse(SolrServiceImpl.isRangeQuery("2000 TO 2010"));
        assertFalse(SolrServiceImpl.isRangeQuery("[2000 TO 2010"));
        assertFalse(SolrServiceImpl.isRangeQuery("2000 TO 2010]"));
    }

    /**
     * "TO" is matched case sensitively, as it was by the pattern this replaced.
     */
    @Test
    public void rejectsLowerCaseTo() {
        assertFalse(SolrServiceImpl.isRangeQuery("[2000 to 2010]"));
    }

    /**
     * "." never matched a line terminator, so a value containing one was escaped rather than
     * passed through as a range. That is preserved: loosening it would let a value skip escaping
     * that did not skip it before.
     */
    @Test
    public void rejectsValuesContainingALineTerminator() {
        assertFalse(SolrServiceImpl.isRangeQuery("[2000 \nTO 2010]"));
        assertFalse(SolrServiceImpl.isRangeQuery("[2000 \rTO 2010]"));
        assertFalse(SolrServiceImpl.isRangeQuery("[2000 " + (char) 0x0085 + "TO 2010]"));
        assertFalse(SolrServiceImpl.isRangeQuery("[2000 " + (char) 0x2028 + "TO 2010]"));
        assertFalse(SolrServiceImpl.isRangeQuery("[2000 " + (char) 0x2029 + "TO 2010]"));
    }

    /**
     * A value that opens a range and never closes it has to be rejected promptly. The pattern
     * used previously needed time quadratic in the length of the value, where the checks that
     * replaced it are linear. The bound is therefore a very wide margin and is not sensitive to
     * a slow CI machine.
     */
    @Test(timeout = 5000)
    public void rejectsUnclosedRangeWithoutBacktracking() {
        assertFalse(SolrServiceImpl.isRangeQuery("[" + "TO".repeat(60000) + "X"));
    }
}
