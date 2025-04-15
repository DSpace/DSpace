package org.dspace.app;

import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.catalina.valves.JsonAccessLogValve;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extended version of JsonAccessLogValue that enables a single JSON attribute
 * consisting of a key and and string value to be appended at the end of
 * log entries.
 * <p>
 * This is done by placing "#<KEY>:<VALUE>#" in the pattern provided to the
 * valve. For example, to have "logFile:access.log" included in the log
 * entries, the pattern would be:
 * <p>
 * {@code "%h %s %b #logFile:access.log#"}
 * <p>
 * This class has the following limitations:
 * <p>
 * 1. The appended attribute is always placed as the last attribute in the
 *    log entry, irrespective of its location in the pattern.
 * <p>
 * 2. Only one JSON attribute can be appended.
 */
public class UmdExtendedJsonAccessLogValve extends JsonAccessLogValve {
    private AccessLogElement keyValueElement;
    private static final Log logger = LogFactory.getLog(UmdExtendedJsonAccessLogValve.class);

    @Override
    public void setPattern(String pattern) {
        logger.debug("pattern='" + pattern + "'");
        if (pattern == null) {
            // No pattern given -- delegate to superclass.
            super.setPattern(pattern);
            return;
        }

        Pattern regex = Pattern.compile(".*\\#(?<key>.+)\\:(?<value>.+)\\#.*");
        Matcher matcher = regex.matcher(pattern);

        logger.debug("matcher.matches()=" + matcher.matches());
        if (!matcher.matches()) {
            // No "#<KEY>:<VALUE>#" pattern present -- delegate to superclass.
            this.keyValueElement = null;
            super.setPattern(pattern);
            return;
        }

        String key = matcher.group("key");
        String value = matcher.group("value");
        logger.debug("key='" + key + "', value='" + value + "'");
        this.keyValueElement = new StringElement(
            wrap(key) + ":" + wrap(value)
        );

        // Strip out the "#key:value#" in case a shorthand pattern such as
        // "common" or "combined" is being used.
        String strippedPattern = pattern.replaceAll("\\s*\\#.*\\#\\s*", "");
        super.setPattern(strippedPattern);
    }

    /**
     * Override superclass to handle the additional "#<key>:<value>#" element
     * (if present).
     */
    @Override
    protected AccessLogElement[] createLogElements() {
        if (keyValueElement == null) {
            return super.createLogElements();
        }

        List<AccessLogElement> logElements = new ArrayList<>(Arrays.asList(super.createLogElements()));

        if (logElements.size() > 0) {
            if (hasSubObject(logElements)) {
                // Replace the last element ("}}") with two "}" elements,
                // so that we can append outside the nested sub-object
                logElements.remove(logElements.size() - 1);
                logElements.add(new StringElement("}"));
                logElements.add(new StringElement("}"));
            }
        }

        List<AccessLogElement> appendedElements = new ArrayList<>();
        if (logElements.size() == 1) {
            // The logElements from JsonAccessLogValve has only a "}" even if
            // there are no other elements in the pattern, so we need to append a
            // starting "{", if there is only one log element.
            logElements.add(0, new StringElement("{"));
        }
        if (logElements.size() > 2) {
            // logElements at this point will have at least "{" and "}" elements
            // so only use a comma to append the next element if there are more
            // than two elements.
            appendedElements.add(new StringElement(","));
        }
        appendedElements.add(keyValueElement);

        logElements.addAll(logElements.size() - 1, appendedElements);
        return logElements.toArray(new AccessLogElement[0]);
    }

    /**
     * Returns true if the given array of AccessLogElements contains
     * "sub-objects" (as defined by JsonAccessLogValve) which are
     * nested in the resulting JSON, and always appear at the end.
     *
     * @param logElements the array of AccessLogElements
     * @return true if the given array of AccessLogElements has one or more
     * sub-objects, false otherwise.
     */
    protected boolean hasSubObject(List<AccessLogElement> logElements) {
        // Determine if last object is a sub-object
        // In the JsonAccessLogValve implementation, all sub-objects are placed
        // at the end, and have "}}" as the last AccessLogElement in the array.
        int lastElementIndex = logElements.size() - 1;
        AccessLogElement lastElement = logElements.get(lastElementIndex);

        if (lastElement instanceof StringElement) {
            StringElement e = (StringElement) lastElement;
            CharArrayWriter caw = new CharArrayWriter();
            e.addElement(caw, null, null, null, 0l);
            return "}}".equals(caw.toString());
        }

        return false;
    }

    /**
     * Wraps the given String in escaped double quotes for output to JSON.
     *
     * @param str the String to wrap in escaped double quotes
     * @return the given String in escaped double quotes
     */
    protected String wrap(String str) {
        return "\"" + str + "\"";
    }
}
