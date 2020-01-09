/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.dspace.core.ReloadableEntity;
import org.hibernate.proxy.HibernateProxyHelper;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
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

    @Override
    public Integer getID() {
        return id;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(Date uploadDate) {
        this.uploadDate = uploadDate;
    }


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
        if (this.getID() != that.getID()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 8;
        hash = 74 * hash + this.getID();
        return hash;
    }
}
