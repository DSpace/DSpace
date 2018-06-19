/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */

package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.content.service.EtdUnitService;
import org.dspace.core.Constants;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Class representing an ETD department as seen in the Proquest metadata element
 * /DISS_submission/DISS_description/DISS_institution/DISS_inst_contact
 *
 * @author Ben Wallberg
 */


@Entity
@Table(name="etdunit")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class EtdUnit extends DSpaceObject implements DSpaceObjectLegacySupport
{

    @Column(name="etdunit_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name="name", length = 256, unique = true)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "collection2etdunit",
            joinColumns = {@JoinColumn(name = "etdunit_id") },
            inverseJoinColumns = {@JoinColumn(name = "collection_id") }
    )
    private Set<Collection> collections = new HashSet<>();

    @Transient
    protected transient EtdUnitService etdunitService;

    protected EtdUnit() {

    }

    /**
     * get name of etdunit
     *
     * @return name
     */
    @Override
    public String getName()
    {
        return this.name == null ? "" : this.name;
    }

    /**
     * set name of etdunit
     *
     * @param name
     *            new etdunit name
     */
    public void setName(String name)
    {
        this.name = name;
        setModified();
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same EtdUnit as
     * this object, <code>false</code> otherwise
     *
     * @param other
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same etdunit
     *         as this object
     */
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof EtdUnit))
        {
            return false;
        }

        return (getID() == ((EtdUnit) other).getID());
    }

    @Override
    public int getType()
    {
        return Constants.ETDUNIT;
    }

    @Override
    public String getHandle()
    {
        return null;
    }

    /**
     * Get the collections this etdunit maps to
     *
     * @return array of <code>Collection</code> s this etdunit maps to
     * @throws SQLException
     */
    public List<Collection> getCollections() throws SQLException
    {
        return new ArrayList<Collection>(this.collections);
    }

    /**
     * Add an existing collection to this etdunit
     *
     * @param collection
     *            the collection to add
     */
    void addCollection(Collection collection) throws SQLException
    {
        this.collections.add(collection);
        setModified();
    }

    /**
     * Remove a collection from this etdunit
     *
     * @param collection
     *            the collection to remove
     */
    void removeCollection(Collection collection) throws SQLException
    {
        this.collections.remove(collection);
        setModified();
    }

    /**
     * Returns true or false based on whether a given collection is a member.
     */
    public boolean isMember(Collection collection)
    {
        return collections.contains(collection);
    }

    @Override
    public Integer getLegacyId() {
      return this.legacyId;
    }
}
