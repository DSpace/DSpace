/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 *
 */
package org.dspace.app.webui.util;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.sql.SQLException;
import java.util.*;


public class ViewAgreement
{
    /** log4j category */
    private static Logger log = Logger.getLogger(ViewAgreement.class);

    /**
     * determine whether requester should acknowledge usage agreement first ;
     * base decision on handle settings in configuration file of item or enclosing collection(s) and community
     *
     * agreement status is kept in the session - aka a mapping from DSPaceObjects to AgreementStatus info
     *
     * @param session the current HttpSession that carries Agreement information
     * @param item the item to check for the need of agreement
     * @return whether user should sign agreement before proceeding
     */
    public static boolean mustAgree(HttpSession session, Item item) {
        log.debug("> mustAgree " + item.getHandle());
        ViewAgreement viewAgreements = getViewAgreement(session);
        DSpaceObject obj = viewAgreements.getWantsAgreement(item);
        if (obj == null) {
            if (log.isDebugEnabled())
                log.debug(String.format("item %s   obj = null -> mustAgree=false", item.getHandle()));
            return false;    // nothing in config found that says we need an agreement
        } else {
            ViewAgreementStatus status = viewAgreements.getAgreementStatus(obj);
            if (log.isDebugEnabled()) {
                log.debug(String.format("item %s  status %s   timedOut %s -> mustAgree=%s",
                        item.getHandle(), status.toString(), String.valueOf(status.isTimedOut()), status.isTimedOut()));
            }
            return status.isTimedOut();
        }
    }

    public static void doAgree(HttpSession session, Item item) {
        ViewAgreement viewAgreements = getViewAgreement(session);
        DSpaceObject obj = viewAgreements.getWantsAgreement(item);
        if (obj != null) {
            viewAgreements.getAgreementStatus(obj).agree();
        } else {
            log.debug("should not call for an item that does not require agreement");
        }
    }

    public static String getText(HttpSession session, Item item) {
        assert (item != null);
        String theAgreementText = null;

        ViewAgreement viewAgreements = getViewAgreement(session);
        DSpaceObject obj = viewAgreements.getWantsAgreement(item);
        if (obj == null) {
            log.error(String.format("getText called for item %s which has no agreement requirement", item.getHandle()));
        } else {
            String filename = viewAgreements.agreements.get(obj).agreementFile;
            if (filename != null) {
                String realFilename = I18nUtil.getAgreementFilename(Locale.getDefault(), filename);
                try {
                    FileInputStream fisTargetFile = new FileInputStream(new File(realFilename));
                    theAgreementText = IOUtils.toString(fisTargetFile, "UTF-8");
                } catch (IOException e) {
                    log.error("could not read agreement file " + realFilename);
                }
            }
        }
        return theAgreementText;
    }

    private Map<DSpaceObject, ViewAgreementStatus> agreements;

    private ViewAgreement() {
         agreements = new HashMap<DSpaceObject, ViewAgreementStatus>();
    }

    private static ViewAgreement getViewAgreement(HttpSession session) {
        ViewAgreement status;
        status = (ViewAgreement) session.getAttribute("agreement");
        if (status == null) {
            status = new ViewAgreement();
            session.setAttribute("agreement", status);
        }
        return status;
    }

    private ViewAgreementStatus getAgreementStatus(DSpaceObject obj) {
        ViewAgreementStatus status = agreements.get(obj);
        assert(status != null || (obj == null));
        return status;
    }

    private void setAgreementText(DSpaceObject obj, String agreementFile) {
        ViewAgreementStatus status = agreements.get(obj);
        if (status == null) {
            agreements.put(obj, new ViewAgreementStatus(obj, agreementFile));
        } else {
            status.agreementFile = agreementFile;
        }
    }

    private void setAgreementTimeout(DSpaceObject obj, int timeout) {
        ViewAgreementStatus status = agreements.get(obj);
        if (status == null) {
            assert false;
            log.error("should never  get here", new Exception("Should never get here"));
        } else {
            if (timeout <= 0) {
                log.warn(String.format("Agreement timeout for %s must be greater zero; found %d instead",
                        obj.getHandle(), timeout));
                timeout = ViewAgreementStatus.DEFAULT_TIMEOUT;
                log.warn(String.format("Setting agreement timeout for %s to %d",
                        obj.getHandle(), timeout));

            }
            status.timeoutMilliSec = timeout * 60 * 1000;
        }
    }

    private  DSpaceObject getWantsAgreement(Item item) {
        DSpaceObject obj = item;
        try {
            while (obj != null) {
                log.debug("getWantsAgreement: obj = " + obj.getHandle());
                String handle = obj.getHandle();
                String handle_view_agreement_file = ConfigurationManager.getProperty(handle + ".bitstream_view_agreement_file");
                if (handle_view_agreement_file != null) {
                    this.setAgreementText(obj, handle_view_agreement_file);
                    String handle_view_timeout_str = ConfigurationManager.getProperty(handle + ".bitstream_view_timeout");
                    if (handle_view_timeout_str != null) {
                        try {
                            this.setAgreementTimeout(obj, Integer.parseInt(handle_view_timeout_str));
                        } catch (NumberFormatException e) {
                            // stick with default
                        }
                    }
                    return obj;
                }
                obj = obj.getParentObject();
            }
        } catch (SQLException e) {
            log.error("How come???  SQLException", e);
            obj = null;
        } finally {
            if (log.isDebugEnabled() && obj != null) {
                log.debug(String.format("getWantsAgreement item %s  - obj %s", item.getHandle(), obj.getHandle()));
            }
        }
        return obj;
    }




}


class ViewAgreementStatus {

    private static Logger log = Logger.getLogger(ViewAgreementStatus.class);

    DSpaceObject obj;
    Date lastTimeAgreed;
    String agreementFile;
    int timeoutMilliSec;

    /**
     * milliseconds  until agreement has to be reaffirmed
     */
    static final int DEFAULT_TIMEOUT = 20 * 60 * 1000;

    ViewAgreementStatus(DSpaceObject obj, String agreementFile) {
        this.obj = obj;
        this.agreementFile = agreementFile;
        this.lastTimeAgreed = null;
        this.timeoutMilliSec = DEFAULT_TIMEOUT ;
    }

    void agree() {
        this.lastTimeAgreed = new Date();
        if (log.isDebugEnabled())
            log.debug(String.format("agree obj %s at %s", obj.getHandle(), String.valueOf(lastTimeAgreed)));
    }

    boolean isTimedOut() {
        if (lastTimeAgreed == null) {
            if (log.isDebugEnabled())
                log.debug(String.format("isTimedOut obj %s    lastTimeAgreed == null  -> true", obj.getHandle()));
            return true;
        }
        Date now = new Date();
        long milliSecs = (now.getTime() - lastTimeAgreed.getTime() );
        boolean timedOut = milliSecs > timeoutMilliSec;
        if (log.isDebugEnabled())
            log.debug(String.format("isTimedOut obj %s    deltaMilli %d  -> %s",
                    obj.getHandle(), (milliSecs - timeoutMilliSec), String.valueOf(timedOut)));
        return  (milliSecs > timeoutMilliSec);
    }

    public String toString() {
        return String.format("(obj %s, lastAgree %s, txt %36s)",
                obj.getHandle(),
                (lastTimeAgreed == null ? "never" : lastTimeAgreed),
                agreementFile);
    }
}