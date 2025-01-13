/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Information about a report run accessible by each check.
 *
 * @author LINDAT/CLARIN dev team
 */
public class ReportInfo {

    private boolean verbose_;
    private Instant from_;
    private Instant till_;

    public ReportInfo(int for_last_n_days) {
        till_ = Instant.now();

        // get info from the last n days
        from_ = till_.minus(for_last_n_days, ChronoUnit.DAYS);
        // filter output
        verbose_ = false;
    }

    public void verbose(boolean verbose) {
        verbose_ = verbose;
    }

    public boolean verbose() {
        return verbose_;
    }

    public Instant from() {
        return from_;
    }

    public Instant till() {
        return till_;
    }
}
