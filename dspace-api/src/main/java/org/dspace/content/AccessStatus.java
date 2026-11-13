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
     * the lease expiration date, if the object has an active lease
     */
    private LocalDate leaseDate;

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
     * Construct a new access status with a lease date
     *
     * @param status           the status value
     * @param availabilityDate the availability date
     * @param leaseDate        the lease expiration date
     */
    public AccessStatus(String status, LocalDate availabilityDate, LocalDate leaseDate) {
        this.status = status;
        this.availabilityDate = availabilityDate;
        this.leaseDate = leaseDate;
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

    /**
     * @return Returns the lease expiration date.
     */
    public LocalDate getLeaseDate() {
        return leaseDate;
    }

    /**
     * @param leaseDate The lease expiration date.
     */
    public void setLeaseDate(LocalDate leaseDate) {
        this.leaseDate = leaseDate;
    }
}
