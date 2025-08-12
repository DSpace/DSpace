/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.time.LocalDate;

/**
 * Utility class for access status
 */
public class AccessStatus {
    /**
     * the status value
     */
    private String status;

    /**
     * the availability date if required
     */
    private LocalDate availabilityDate;

    /**
     * Construct a new access status
     *
     * @param status           the status value
     * @param availabilityDate the availability date
     */
    public AccessStatus(String status, LocalDate availabilityDate) {
        this.status = status;
        this.availabilityDate = availabilityDate;
    }

    /**
     * @return Returns the status value.
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status The status value.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return Returns the availability date.
     */
    public LocalDate getAvailabilityDate() {
        return availabilityDate;
    }

    /**
     * @param availabilityDate The availability date.
     */
    public void setAvailabilityDate(LocalDate availabilityDate) {
        this.availabilityDate = availabilityDate;
    }
}
