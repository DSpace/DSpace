/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */


/* Created for LINDAT/CLARIAH-CZ (UFAL) */

package org.dspace.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.handle.HandlePlugin;
import org.dspace.handle.PIDService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

public class DSpaceApi {

    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger();

    private static ConfigurationService configurationService = new DSpace().getConfigurationService();

    private DSpaceApi() {

    }
    /**
     * Create a new handle PID. This is modified implementation for UFAL, using
     * the PID service pidconsortium.eu as wrapped in the PIDService class.
     *
     * Note: this function creates a handle to a provisional existing URL and
     * the handle must be updated to point to the final URL once DSpace is able
     * to report the URL exists (otherwise the pidservice will refuse to set the
     * URL)
     *
     * @return A new handle PID
     * @exception Exception If error occurrs
     */
    public static String handle_HandleManager_createId(Logger log, Long id,
                                                       String prefix, String suffix) throws IOException {

        /* Modified by PP for use pidconsortium.eu at UFAL/CLARIN */

        String base_url = configurationService.getProperty("dspace.server.url") + "?dummy=" + id;

        /* OK check whether this url has not received pid earlier */
        //This should usually return null (404)
        String handle = null;
        try {
            handle = PIDService.findHandle(base_url, prefix);
        } catch (Exception e) {
            log.error("Error finding handle: " + e);
        }
        //if not then log and reuse - this is a dummy url, those should not be seen anywhere
        if (handle != null) {
            log.warn("Url [" + base_url + "] already has PID(s) (" + handle + ").");
            return handle;
        }
        /* /OK/ */

        log.debug("Asking for a new PID using a dummy URL " + base_url);

        /* request a new PID, initially pointing to dspace base_uri+id */
        String pid = null;
        try {
            if (suffix != null && !suffix.isEmpty() && PIDService.supportsCustomPIDs()) {
                pid = PIDService.createCustomPID(base_url, prefix, suffix);
            } else {
                pid = PIDService.createPID(base_url, prefix);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }

        log.debug("got PID " + pid);
        return pid;
    }

    /**
     * Modify an existing PID to point to the corresponding DSpace handle
     *
     * @exception SQLException If a database error occurs
     */
    public static void handle_HandleManager_registerFinalHandleURL(Logger log,
                                                                   String pid, DSpaceObject dso) throws IOException {
        if (pid == null) {
            log.info("Modification failed invalid/null PID.");
            return;
        }

        String url = configurationService.getProperty("dspace.url");
        url = url + (url.endsWith("/") ? "" : "/") + "handle/" + pid;

        /*
         * request modification of the PID to point to the correct URL, which
         * itself should contain the PID as a substring
         */
        log.debug("Asking for changing the PID '" + pid + "' to " + url);

        try {
            Map<String, String> fields = HandlePlugin.extractMetadata(dso);
            PIDService.modifyPID(pid, url, fields);
        } catch (Exception e) {
            throw new IOException("Failed to map PID " + pid + " to " + url
                    + " (" + e.toString() + ")");
        }
    }
}
