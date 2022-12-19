/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class useful for check regex and patterns.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class RegexPatternUtils {

    // checks input having the format /{pattern}/{flags}
    // allowed flags are: g,i,m,s,u,y
    public static final String REGEX_INPUT_VALIDATOR = "(/?)(.+)\\1([gimsuy]*)";
    // flags usable inside regex definition using format (?i|m|s|u|y)
    public static final String REGEX_FLAGS = "(?%s)";
    public static final Pattern PATTERN_REGEX_INPUT_VALIDATOR =
        Pattern.compile(REGEX_INPUT_VALIDATOR, CASE_INSENSITIVE);

    /**
     * Computes a pattern starting from a regex definition with flags that
     * uses the standard format: <code>/{regex}/{flags}</code> (ECMAScript format).
     * This method can transform an ECMAScript regex into a java {@code Pattern} object
     * wich can be used to validate strings.
     * <br/>
     * If regex is null, empty or blank a null {@code Pattern} will be retrieved
     * If it's a valid regex, then a non-null {@code Pattern} will be retrieved,
     * an exception will be thrown otherwise.
     *
     * @param regex with format <code>/{regex}/{flags}</code>
     * @return {@code Pattern} regex pattern instance
     * @throws PatternSyntaxException
     */
    public static final Pattern computePattern(String regex) throws PatternSyntaxException {
        if (StringUtils.isBlank(regex)) {
            return null;
        }
        Matcher inputMatcher = PATTERN_REGEX_INPUT_VALIDATOR.matcher(regex);
        String regexPattern = regex;
        String regexFlags = "";
        if (inputMatcher.matches()) {
            regexPattern =
                Optional.of(inputMatcher.group(2))
                    .filter(StringUtils::isNotBlank)
                    .orElse(regex);
            regexFlags =
                Optional.ofNullable(inputMatcher.group(3))
                    .filter(StringUtils::isNotBlank)
                    .map(flags -> String.format(REGEX_FLAGS, flags))
                    .orElse("")
                    .replaceAll("g", "");
        }
        return Pattern.compile(regexFlags + regexPattern);
    }

    private RegexPatternUtils() {}

}
