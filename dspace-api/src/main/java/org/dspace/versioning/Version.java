/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
@Entity
@Table(name="versionitem")
public class Version implements ReloadableEntity<Integer> {

    @Id
    @Column(name="versionitem_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="versionitem_seq")
    @SequenceGenerator(name="versionitem_seq", sequenceName="versionitem_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "version_number")
    private int versionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id")
    private EPerson ePerson;

    @Column(name = "version_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date versionDate;

    @Column(name = "version_summary", length = 255)
    private String summary;

    @ManyToOne
    @JoinColumn(name = "versionhistory_id")
    private VersionHistory versionHistory;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.versioning.service.VersioningService#createNewVersion(Context, Item)}
     * or
     * {@link org.dspace.versioning.service.VersioningService#createNewVersion(Context, Item, String)}
     * or
     * {@link org.dspace.versioning.service.VersioningService#createNewVersion(Context, VersionHistory, Item, String, Date, int)}
     *
     */
    protected Version()
    {

    }

    public Integer getID() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int version_number) {
        this.versionNumber = version_number;
    }

    public EPerson getEPerson() {
        return ePerson;
    }

    public void setePerson(EPerson ePerson) {
        this.ePerson = ePerson;
    }

    public Date getVersionDate() {
        return versionDate;
    }

    public void setVersionDate(Date versionDate) {
        this.versionDate = versionDate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String versionSummary) {
        this.summary = versionSummary;
    }

    public VersionHistory getVersionHistory() {
        return versionHistory;
    }

    public void setVersionHistory(VersionHistory versionHistory) {
        this.versionHistory = versionHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
        {
            return true;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(o);
        if (getClass() != objClass)
        {
            return false;
        }

        final Version that = (Version)o;
        if (this.getID() != that.getID())
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash=7;
        hash=79*hash+ this.getID();
        return hash;
    }
}
