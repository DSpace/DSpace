/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */


/* Created for LINDAT/CLARIAH-CZ (UFAL) */
package org.dspace.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *  Provides logging capabilities to XSL interpreter.
 *
 *  Class is copied from the LINDAT/CLARIAH-CZ (https://github.com/ufal/clarin-dspace) and modified by
 *  @author Marian Berger (marian.berger at dataquest.sk)
 */
public class XslLogUtil {

    /** log4j logger */
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger
            .getLogger("Missed");

    private XslLogUtil() {}

    private static final HashMap<String, String> defaults;

    static {
        defaults = new HashMap<String, String>();
        defaults.put("type", "corpus");
        defaults.put("mediaType", "text");
        defaults.put("distributionAccessMedium", "downloadable");
    }

    /**
     * Logs missing item
     * @param key key for logging
     * @param handle handle of missing item
     * @return key witch which it was logged
     */
    public static String logMissing(String key, String handle) {
        String val = XslLogUtil.getDefaultVal(key);
        log_error(String.format("Item with handle %s is missing value for %s. Using '%s' instead.", handle, key, val));
        return val;
    }

    /**
     * Logs missing item
     * @param key key for logging
     * @param handle handle of missing item
     * @param msg modified log message
     * @return key witch which it was logged
     */

    public static String logMissing(String key, String handle, String msg) {
        String val = XslLogUtil.getDefaultVal(key);
        log_error(String.format("%s:%s\n%s", handle, key, msg));
        return val;
    }

    private static String getDefaultVal(String key) {
        String val = "No value given";
        if (defaults.containsKey(key)) {
            val = defaults.get(key);
        }
        return val;
    }

    //
    // logger wrapper
    // - should be synchronized but one message more or less is not important
    //

    private static Map<String, Set<String>> _logged_msgs =
            new HashMap<String, Set<String>>();
    private static final SimpleDateFormat _logged_fmt =
            new SimpleDateFormat("dd/MM/yyyy");

    static private boolean _already_logged(String message) {
        String today = _logged_fmt.format(Calendar.getInstance().getTime());
        if (!_logged_msgs.containsKey(today)) {
            _logged_msgs.clear();
            _logged_msgs.put(today, new HashSet<String>());
        }
        Set<String> msgs = _logged_msgs.get(today);
        if (Objects.nonNull(msgs) && msgs.contains(message)) {
            return true;
        }
        if (Objects.nonNull(msgs)) {
            msgs.add(message);
        }
        return false;
    }

    private static void log_info(String message) {
        if (_already_logged(message)) {
            return;
        }
        log.info(message);
    }

    private static void log_error(String message) {
        if (_already_logged(message)) {
            return;
        }
        log.error(message);
    }

}
