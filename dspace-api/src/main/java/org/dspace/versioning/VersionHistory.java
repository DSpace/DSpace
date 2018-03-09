/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
 */
@Entity
@Table(name="versionhistory")
public class VersionHistory implements ReloadableEntity<Integer> {
    
    private static final Logger log = Logger.getLogger(VersionHistory.class);

    @Id
    @Column(name="versionhistory_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="versionhistory_seq")
    @SequenceGenerator(name="versionhistory_seq", sequenceName="versionhistory_seq", allocationSize = 1)
    private Integer id;

    //We use fetchtype eager for versions since we always require our versions when loading the history
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "versionHistory")
    @OrderBy(value = "versionNumber desc")
    private List<Version> versions = new ArrayList<Version>();

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.versioning.service.VersionHistoryService#create(Context)}
     *
     */
    protected VersionHistory()
    {

    }

    public Integer getID() {
        return id;
    }

    /**
     * Please use {@link org.dspace.versioning.service.VersioningService#getVersionsByHistory(Context, VersionHistory)} instead.
     * 
     * To keep version number stables we keep information about deleted Versions.
     * {@code org.dspace.versioning.service.VersioningService#getVersionsByHistory(Context, VersionHistory) VersioningService#getVersionsByHistory} filters
     * such versions and returns only active versions.
     */
    protected List<Version> getVersions() {
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
        if (this.getID() != that.getID())
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int hash=7;
        hash=79*hash+ this.getID();
        return hash;
    }

}
