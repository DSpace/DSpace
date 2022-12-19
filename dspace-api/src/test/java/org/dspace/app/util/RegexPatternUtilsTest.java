/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.dspace.AbstractUnitTest;
import org.junit.Test;

/**
 * Tests for RegexPatternUtils
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class RegexPatternUtilsTest extends AbstractUnitTest {

    @Test
    public void testValidRegexWithFlag() {
        final String insensitiveWord = "/[a-z]+/i";
        Pattern computePattern = Pattern.compile(insensitiveWord);
        assertNotNull(computePattern);

        Matcher matcher = computePattern.matcher("Hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("DSpace");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("Community");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("/wrongpattern/i");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("001");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("?/'`}{][<>.,");
        assertFalse(matcher.matches());
        computePattern = RegexPatternUtils.computePattern(insensitiveWord);
        assertNotNull(computePattern);

        matcher = computePattern.matcher("Hello");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("DSpace");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("Community");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("/wrong-pattern/i");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("001");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("?/'`}{][<>.,");
        assertFalse(matcher.matches());
    }

    @Test
    public void testRegexWithoutFlag() {
        final String sensitiveWord = "[a-z]+";
        Pattern computePattern = RegexPatternUtils.computePattern(sensitiveWord);
        assertNotNull(computePattern);

        Matcher matcher = computePattern.matcher("hello");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("dspace");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("community");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("Hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("DSpace");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("Community");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("/wrongpattern/i");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("001");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("?/'`}{][<>.,");
        assertFalse(matcher.matches());

        final String sensitiveWordWithDelimiter = "/[a-z]+/";
        computePattern = RegexPatternUtils.computePattern(sensitiveWordWithDelimiter);
        assertNotNull(computePattern);

        matcher = computePattern.matcher("hello");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("dspace");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("community");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("Hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("DSpace");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("Community");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("/wrongpattern/i");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("001");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("?/'`}{][<>.,");
        assertFalse(matcher.matches());
    }

    @Test
    public void testWithFuzzyRegex() {
        String fuzzyRegex = "/[a-z]+";
        Pattern computePattern = RegexPatternUtils.computePattern(fuzzyRegex);
        assertNotNull(computePattern);

        Matcher matcher = computePattern.matcher("/hello");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("Hello");
        assertFalse(matcher.matches());

        fuzzyRegex = "[a-z]+/";
        computePattern = RegexPatternUtils.computePattern(fuzzyRegex);
        matcher = computePattern.matcher("hello/");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("/hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("Hello");
        assertFalse(matcher.matches());

        // equals to pattern \\[a-z]+\\ -> searching for a word delimited by '\'
        fuzzyRegex = "\\\\[a-z]+\\\\";
        computePattern = RegexPatternUtils.computePattern(fuzzyRegex);
        // equals to '\hello\'
        matcher = computePattern.matcher("\\hello\\");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("/hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("Hello");
        assertFalse(matcher.matches());

        // equals to pattern /[a-z]+/ -> searching for a string delimited by '/'
        fuzzyRegex = "\\/[a-z]+\\/";
        computePattern = RegexPatternUtils.computePattern(fuzzyRegex);
        matcher = computePattern.matcher("/hello/");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("/hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("hello");
        assertFalse(matcher.matches());
        matcher = computePattern.matcher("Hello");
        assertFalse(matcher.matches());
    }

    @Test
    public void testInvalidRegex() {
        String invalidSensitive = "[a-z+";
        assertThrows(PatternSyntaxException.class, () -> RegexPatternUtils.computePattern(invalidSensitive));

        String invalidRange = "a{1-";
        assertThrows(PatternSyntaxException.class, () -> RegexPatternUtils.computePattern(invalidRange));

        String invalidGroupPattern = "(abc";
        assertThrows(PatternSyntaxException.class, () -> RegexPatternUtils.computePattern(invalidGroupPattern));

        String emptyPattern = "";
        Pattern computePattern = RegexPatternUtils.computePattern(emptyPattern);
        assertNull(computePattern);

        String blankPattern = "                      ";
        computePattern = RegexPatternUtils.computePattern(blankPattern);
        assertNull(computePattern);

        String nullPattern = null;
        computePattern = RegexPatternUtils.computePattern(nullPattern);
        assertNull(computePattern);
    }

    @Test
    public void testMultiFlagRegex() {
        String multilineSensitive = "/[a-z]+/gi";
        Pattern computePattern = RegexPatternUtils.computePattern(multilineSensitive);
        assertNotNull(computePattern);
        Matcher matcher = computePattern.matcher("hello");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("Hello");
        assertTrue(matcher.matches());

        multilineSensitive = "/[a-z]+/gim";
        computePattern = RegexPatternUtils.computePattern(multilineSensitive);
        assertNotNull(computePattern);
        matcher = computePattern.matcher("Hello" + System.lineSeparator() + "Everyone");
        assertTrue(matcher.find());
        assertEquals("Hello", matcher.group());
        assertTrue(matcher.find());
        assertEquals("Everyone", matcher.group());

        matcher = computePattern.matcher("hello");
        assertTrue(matcher.matches());
        matcher = computePattern.matcher("HELLO");
        assertTrue(matcher.matches());
    }
}
