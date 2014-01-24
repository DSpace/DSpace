package org.dspace.usagelogging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.rog>
 */
public class EventLogger {
    static Logger log = Logger.getLogger(EventLogger.class);
    public static void log(Context context, String action, String userInfo) {
        try {
            String message = LogManager.getHeader(context, "action="+action, userInfo != null ? userInfo : "");
            // Send to logfile configured for EventLogger
            log.log(Level.INFO, message);
        } catch (Exception ex) {
            log.error("Exception in EventLogger.log()", ex);
        }
    }
}
