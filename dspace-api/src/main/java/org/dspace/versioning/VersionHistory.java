/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
@Entity
@Table(name="versionhistory", schema = "public")
public class VersionHistory {

    @Id
    @Column(name="versionhistory_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="versionhistory_seq")
    @SequenceGenerator(name="versionhistory_seq", sequenceName="versionhistory_seq", allocationSize = 1)
    private int id;

    //We use fetchtype eager for versions since we always require our versions when loading the history
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "versionHistory")
    @OrderBy(value = "versionNumber desc")
    private List<Version> versions = new ArrayList<Version>();

    public int getId() {
        return id;
    }

    public List<Version> getVersions() {
        return versions;
    }

    void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    void addVersionAtStart(Version version)
    {
        this.versions.add(0, version);
    }

    void removeVersion(Version version) {
        this.versions.remove(version);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(o);
        if (getClass() != objClass)
        {
            return false;
        }

        final VersionHistory that = (VersionHistory)o;
        if (this.getId() != that.getId())
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int hash=7;
        hash=79*hash+ this.getId();
        return hash;
    }

}