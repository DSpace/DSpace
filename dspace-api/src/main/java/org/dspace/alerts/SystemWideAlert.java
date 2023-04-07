/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.alerts;

import java.util.Date;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Database object representing system-wide alerts
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "systemwidealert")
public class SystemWideAlert implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_id_seq")
    @SequenceGenerator(name = "alert_id_seq", sequenceName = "alert_id_seq", allocationSize = 1)
    @Column(name = "alert_id", unique = true, nullable = false)
    private Integer alertId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "allow_sessions")
    private String allowSessions;

    @Column(name = "countdown_to")
    @Temporal(TemporalType.TIMESTAMP)
    private Date countdownTo;

    @Column(name = "active")
    private boolean active;

    protected SystemWideAlert() {
    }

    /**
     * This method returns the ID that the system-wide alert holds within the database
     *
     * @return The ID that the system-wide alert holds within the database
     */
    @Override
    public Integer getID() {
        return alertId;
    }

    /**
     * Set the ID for the system-wide alert
     *
     * @param alertID The ID to set
     */
    public void setID(final Integer alertID) {
        this.alertId = alertID;
    }

    /**
     * Retrieve the message of the system-wide alert
     *
     * @return the message of the system-wide alert
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the message of the system-wide alert
     *
     * @param message The message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Retrieve what kind of sessions are allowed while the system-wide alert is active
     *
     * @return what kind of sessions are allowed while the system-wide alert is active
     */
    public AllowSessionsEnum getAllowSessions() {
        return AllowSessionsEnum.fromString(allowSessions);
    }

    /**
     * Set what kind of sessions are allowed while the system-wide alert is active
     *
     * @param allowSessions Integer representing what kind of sessions are allowed
     */
    public void setAllowSessions(AllowSessionsEnum allowSessions) {
        this.allowSessions = allowSessions.getValue();
    }

    /**
     * Retrieve the date to which will be count down when the system-wide alert is active
     *
     * @return the date to which will be count down when the system-wide alert is active
     */
    public Date getCountdownTo() {
        return countdownTo;
    }

    /**
     * Set the date to which will be count down when the system-wide alert is active
     *
     * @param countdownTo The date to which will be count down
     */
    public void setCountdownTo(final Date countdownTo) {
        this.countdownTo = countdownTo;
    }

    /**
     * Retrieve whether the system-wide alert is active
     *
     * @return whether the system-wide alert is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set whether the system-wide alert is active
     *
     * @param active    Whether the system-wide alert is active
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same SystemWideAlert
     * as this object, <code>false</code> otherwise
     *
     * @param other object to compare to
     * @return <code>true</code> if object passed in represents the same
     * system-wide alert as this object
     */
    @Override
    public boolean equals(Object other) {
        return (other instanceof SystemWideAlert &&
                new EqualsBuilder().append(this.getID(), ((SystemWideAlert) other).getID())
                                   .append(this.getMessage(), ((SystemWideAlert) other).getMessage())
                                   .append(this.getAllowSessions(), ((SystemWideAlert) other).getAllowSessions())
                                   .append(this.getCountdownTo(), ((SystemWideAlert) other).getCountdownTo())
                                   .append(this.isActive(), ((SystemWideAlert) other).isActive())
                                   .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.getID())
                .append(this.getMessage())
                .append(this.getAllowSessions())
                .append(this.getCountdownTo())
                .append(this.isActive())
                .toHashCode();
    }

}
