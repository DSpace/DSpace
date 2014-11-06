/*
 */
package org.dspace.statistics;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class SolrLoggerUtilsTest {
    private static Logger log = Logger.getLogger(SolrLoggerUtilsTest.class);
    private static final String REFERRER_NO_TOKEN = "https://datadryad.org/handle/10255/2/submit/eff2d3c77151079f94669152d2a43c02eff2d3c7.continue?processonly=true";
    private static final String REFERRER_TOKEN_1 = "http://datadryad.org/review?wfID=12345&token=07cae063-87f0-4d25-ac8c-595a80ac7ea8";
    private static final String REFERRER_TOKEN_2 = "http://datadryad.org/review?wfID=12345&token=sometokenimadeup";
    private static final String REFERRER_DOI = "http://datadryad.org/review?doi=doi:10.5061/dryad.11234";
    private static final String DUMMY_TOKEN = SolrLoggerUtils.DUMMY_TOKEN;
    private static final String DUMMY_DOI = SolrLoggerUtils.DUMMY_DOI;
    private static final String REFERRER_TOKEN_REPLACED = "http://datadryad.org/review?wfID=12345&token=" + DUMMY_TOKEN;
    private static final String REFERRER_DOI_REPLACED = "http://datadryad.org/review?doi=" + DUMMY_DOI;

    public SolrLoggerUtilsTest() {
    }

    /**
     * Test of replaceReviewToken method, of class SolrLoggerUtils.
     */
    @Test
    public void testReplaceReviewTokenUUID() {
        log.info("replaceReviewToken1 - uuid");
        String referrerUri = REFERRER_TOKEN_1;
        String replacementText = DUMMY_TOKEN;
        String expResult = REFERRER_TOKEN_REPLACED;
        String result = SolrLoggerUtils.replaceReviewToken(referrerUri, replacementText);
        assertEquals(expResult, result);
    }

    /**
     * Test of replaceReviewToken method, of class SolrLoggerUtils.
     */
    @Test
    public void testReplaceReviewTokenUser() {
        log.info("replaceReviewToken - user-generated");
        String referrerUri = REFERRER_TOKEN_2;
        String replacementText = DUMMY_TOKEN;
        String expResult = REFERRER_TOKEN_REPLACED;
        String result = SolrLoggerUtils.replaceReviewToken(referrerUri, replacementText);
        assertEquals(expResult, result);
    }

    /**
     * Test of replaceReviewToken method, of class SolrLoggerUtils.
     */
    @Test
    public void testReplaceReviewTokenNoToken() {
        log.info("replaceReviewToken - no token");
        String referrerUri = REFERRER_NO_TOKEN;
        String replacementText = DUMMY_TOKEN;
        String expResult = REFERRER_NO_TOKEN;
        String result = SolrLoggerUtils.replaceReviewToken(referrerUri, replacementText);
        assertEquals(expResult, result);
    }
    /**
     * Test of isReviewTokenPresent method, of class SolrLoggerUtils.
     */
    @Test
    public void testIsReviewTokenPresentTrue() {
        log.info("isReviewTokenPresentTrue");
        String referrerUri = REFERRER_TOKEN_1;
        Boolean expResult = Boolean.TRUE;
        Boolean result = SolrLoggerUtils.isReviewTokenPresent(referrerUri);
        assertEquals(expResult, result);
    }
    /**
     * Test of isReviewTokenPresent method, of class SolrLoggerUtils.
     */
    @Test
    public void testIsReviewTokenPresentFalse() {
        log.info("isReviewTokenPresentFalse");
        String referrerUri = REFERRER_NO_TOKEN;
        Boolean expResult = Boolean.FALSE;
        Boolean result = SolrLoggerUtils.isReviewTokenPresent(referrerUri);
        assertEquals(expResult, result);
    }

    /**
     * Test of replaceReviewDOI method, of class SolrLoggerUtils.
     */
    @Test
    public void testReplaceReviewDOI() {
        log.info("replaceReviewDOI");
        String referrerUri = REFERRER_DOI;
        String replacementText = DUMMY_DOI;
        String expResult = REFERRER_DOI_REPLACED;
        String result = SolrLoggerUtils.replaceReviewDOI(referrerUri, replacementText);
        assertEquals(expResult, result);
    }

    /**
     * Test of replaceReviewDOI method, of class SolrLoggerUtils.
     */
    @Test
    public void testReplaceReviewDOINoDOI() {
        log.info("replaceReviewDOI - no DOI");
        String referrerUri = REFERRER_NO_TOKEN;
        String replacementText = DUMMY_DOI;
        String expResult = REFERRER_NO_TOKEN;
        String result = SolrLoggerUtils.replaceReviewDOI(referrerUri, replacementText);
        assertEquals(expResult, result);
    }
    /**
     * Test of isReviewDOIPresent method, of class SolrLoggerUtils.
     */
    @Test
    public void testIsReviewDOIPresentTrue() {
        log.info("isReviewDOIPresentTrue");
        String referrerUri = REFERRER_DOI;
        Boolean expResult = Boolean.TRUE;
        Boolean result = SolrLoggerUtils.isReviewDOIPresent(referrerUri);
        assertEquals(expResult, result);
    }
    /**
     * Test of isReviewDOIPresent method, of class SolrLoggerUtils.
     */
    @Test
    public void testIsReviewDOIPresentFalse() {
        log.info("isReviewDOIPresentFalse");
        String referrerUri = REFERRER_NO_TOKEN;
        Boolean expResult = Boolean.FALSE;
        Boolean result = SolrLoggerUtils.isReviewDOIPresent(referrerUri);
        assertEquals(expResult, result);
    }
}
