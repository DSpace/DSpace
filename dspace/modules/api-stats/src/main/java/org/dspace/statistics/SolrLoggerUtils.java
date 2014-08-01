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
    private static final Pattern REVIEW_TOKEN_PATTERN = Pattern.compile("(http.*token=)(.*)");
    static final String DUMMY_TOKEN = "00000000-0000-0000-0000-000000000000";

        /**
     * Replaces the review token with replacement text if token is present
     * @param referrerUri A referrer string
     * @param replacementText The text to replace the token with
     * @return a referrer string, with the token replaced
     */
    static String replaceReviewToken(String referrerUri, String replacementText) {
        Matcher matcher = REVIEW_TOKEN_PATTERN.matcher(referrerUri);
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
        if(referrerUri == null) {
            return false;
        } else {
            return REVIEW_TOKEN_PATTERN.matcher(referrerUri).matches();
        }
    }
}
