/*
 */
package org.dspace.statistics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for checking/replacing review tokens in referrer strings.
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class SolrLoggerUtils {
    private static final Pattern REVIEW_TOKEN_PATTERN = Pattern.compile("(http.*/review.*token=)(.*)");
    private static final Pattern REVIEW_DOI_PATTERN = Pattern.compile("(http.*/review.*doi=)(.*)");
    static final String DUMMY_TOKEN = "00000000-0000-0000-0000-000000000000";
    static final String DUMMY_DOI = "doi:00.0000/dryad.00000";

    /**
     * Replaces the review token with replacement text if token is present
     * @param referrerUri A referrer string
     * @param replacementText The text to replace the token with
     * @return a referrer string, with the token replaced
     */
    static String replaceReviewToken(String referrerUri, String replacementText) {
        return replacePattern(REVIEW_TOKEN_PATTERN, referrerUri, replacementText);
    }

    /**
     * Replaces the review DOI with replacement text if review DOI is present
     * @param referrerUri A referrer string
     * @param replacementText The text to replace the doi with
     * @return a referrer string, with the doi replaced
     */
    static String replaceReviewDOI(String referrerUri, String replacementText) {
        return replacePattern(REVIEW_DOI_PATTERN, referrerUri, replacementText);
    }

    private static String replacePattern(Pattern pattern, String referrerUri, String replacementText) {
        Matcher matcher = pattern.matcher(referrerUri);
        if(matcher.matches()) {
            return matcher.group(1) + replacementText;
        } else {
            return referrerUri;
        }

    }

    /**
     * Returns true if a string looks like a URL and contains 'token=...'
     * @param referrerUri The string to test
     * @return True if a token is found, false if not.
     */
    static Boolean isReviewTokenPresent(String referrerUri) {
        return patternMatches(REVIEW_TOKEN_PATTERN, referrerUri);
    }

    /**
     * Returns true if a string looks like a URL and contains 'doi=...'
     * @param referrerUri The string to test
     * @return True if a doi is found, false if not.
     */
    static Boolean isReviewDOIPresent(String referrerUri) {
        return patternMatches(REVIEW_DOI_PATTERN, referrerUri);
    }

    static Boolean patternMatches(Pattern pattern, String referrerUri) {
        if(referrerUri == null) {
            return false;
        } else {
            return pattern.matcher(referrerUri).matches();
        }
    }
}
