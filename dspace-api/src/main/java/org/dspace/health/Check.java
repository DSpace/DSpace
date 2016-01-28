/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import org.apache.log4j.Logger;

/**
 * Abstract check interface.
 * @author LINDAT/CLARIN dev team
 */

public abstract class Check {

    protected static Logger log = Logger.getLogger(Check.class);
    long took_ = -1L;
    String report_ = null;
    private String errors_ = "";

    // this method should be overridden
    protected abstract String run( ReportInfo ri );

    public void report( ReportInfo ri ) {
        took_ = System.currentTimeMillis();
        try {
            String run_report = run(ri);
            report_ = errors_ + run_report;
        }finally {
            took_ = System.currentTimeMillis() - took_;
        }
    }

    protected void error( Throwable e ) {
        error(e, null);
    }
    protected void error( Throwable e, String msg ) {
        errors_ += "====\nException occurred!\n";
        if ( null != e ) {
            errors_ += e.toString() + "\n";
            log.error("Exception during healthcheck:", e);
        }
        if ( null != msg ) {
            errors_ += "Reason: " + msg + "\n";
        }
    }

}
