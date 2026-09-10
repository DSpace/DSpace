/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.dspace.eperson.EPerson;

/**
 * Model a single LOGIN statistic entry related to a specific user.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class LoginStatistics {

    private final EPerson user;

    private final long count;

    public LoginStatistics(EPerson user, long count) {
        this.user = user;
        this.count = count;
    }

    public EPerson getUser() {
        return user;
    }

    public long getCount() {
        return count;
    }

}
