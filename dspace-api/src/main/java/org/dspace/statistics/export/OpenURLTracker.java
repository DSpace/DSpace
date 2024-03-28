/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.dspace.core.HibernateProxyHelper;
import org.dspace.core.ReloadableEntity;


/**
 * Class that represents an OpenURLTracker which tracks a failed transmission to IRUS
 */
@Entity
@Table(name = "OpenUrlTracker")
public class OpenURLTracker implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "tracker_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "openurltracker_seq")
    @SequenceGenerator(name = "openurltracker_seq", sequenceName = "openurltracker_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "tracker_url", length = 1000)
    private String url;

    @Column(name = "uploaddate")
    @Temporal(TemporalType.DATE)
    private Date uploadDate;

    protected OpenURLTracker() {
    }

    /**
     * Gets the OpenURLTracker id
     * @return the id
     */
    @Override
    public Integer getID() {
        return id;
    }

    /**
     * Gets the OpenURLTracker url
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the OpenURLTracker url
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the upload date
     * @return upload date
     */
    public Date getUploadDate() {
        return uploadDate;
    }

    /**
     * Set the upload date
     * @param uploadDate
     */
    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }

    /**
     * Determines whether two objects of this class are equal by comparing the ID
     * @param o - object to compare
     * @return whether the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(o);
        if (getClass() != objClass) {
            return false;
        }

        final OpenURLTracker that = (OpenURLTracker) o;
        if (!this.getID().equals(that.getID())) {
            return false;
        }

        return true;
    }

    /**
     * Returns the hash code value for the object
     * @return hash code
     */
    @Override
    public int hashCode() {
        int hash = 8;
        hash = 74 * hash + this.getID();
        return hash;
    }
}
