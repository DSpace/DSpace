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

    /**
     * Creates a new login statistic entry.
     *
     * @param user  the user the statistic refers to
     * @param count the number of logins performed by the user
     */
    public LoginStatistics(EPerson user, long count) {
        this.user = user;
        this.count = count;
    }

    /**
     * Returns the user related to this statistic entry.
     *
     * @return the user
     */
    public EPerson getUser() {
        return user;
    }

    /**
     * Returns the number of logins performed by the user.
     *
     * @return the login count
     */
    public long getCount() {
        return count;
    }

}
