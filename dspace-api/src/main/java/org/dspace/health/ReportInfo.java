/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

/**
 * Information about a report run accessible by each check.
 * @author LINDAT/CLARIN dev team
 */
public class ReportInfo {

    private boolean verbose_;
    private GregorianCalendar from_ = null;
    private GregorianCalendar till_ = null;

    public ReportInfo(int for_last_n_days) {
        GregorianCalendar cal = new GregorianCalendar();
        till_ = new GregorianCalendar(
            cal.get(YEAR), cal.get(MONTH), cal.get(DAY_OF_MONTH)
        );
        // get info from the last n days
        from_ = (GregorianCalendar)till_.clone();
        from_.add(DAY_OF_MONTH, -for_last_n_days);
        // filter output
        verbose_ = false;
    }

    public void verbose( boolean verbose ) {
        verbose_ = verbose;
    }
    public boolean verbose() {
        return verbose_;
    }

    public Date from() {
        return from_.getTime();
    }

    public Date till() {
        return till_.getTime();
    }
}
