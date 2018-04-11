/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 * 
 */

package org.dspace.eperson;

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

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.eperson.service.UnitService;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Class representing a campus unit.
 * 
 * @author Ben Wallberg
 */

@Entity
@Table(name="unit")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class Unit extends DSpaceObject
{

    @Column(name="unit_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name="name", length = 256, unique = true)
    private String name;

    @Column(name="faculty_only")
    private boolean facultyOnly;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "epersongroup2unit",
            joinColumns = { @JoinColumn(name = "unit_id") },
            inverseJoinColumns = { @JoinColumn(name = "eperson_group_id") }
    )
    private Set<Group> groups = new HashSet<>();

    @Transient
    private boolean unitChanged;

    @Transient
    protected transient UnitService unitService;

    Unit () {

    }

    /**
     * get name of unit
     * 
     * @return name
     */
    public String getName()
    {
        return this.name == null ? "" : this.name;
    }

    /**
     * set name of unit
     * 
     * @param name
     *            new unit name
     */
    public void setName(String name)
    {
        this.name = name;
        setModified();
    }

    /**
     * set faculty requirement
     * 
     * @param login
     *            boolean yes/no
     */
    public void setFacultyOnly(boolean facultyOnly)
    {
        this.facultyOnly = facultyOnly;
        setModified();
    }

    /**
     * faculty only?
     * 
     * @return boolean, yes/no
     */
    public boolean getFacultyOnly()
    {
        return this.facultyOnly;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Unit as this
     * object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same unit as
     *         this object
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof Unit))
        {
            return false;
        }

        return (getID() == ((Unit) other).getID());
    }

    public int getType()
    {
        return Constants.UNIT;
    }

    public String getHandle()
    {
        return null;
    }

    /**
     * Get the groups this unit maps to
     * 
     * @return array of <code>Group</code> s this unit maps to
     * @throws SQLException
     */
    public List<Group> getGroups() throws SQLException
    {
        return new ArrayList<Group>(this.groups);
    }

    /**
     * Add an existing group to this unit
     * 
     * @param group
     *            the group to add
     */
    void addGroup(Group group) throws SQLException
    {
        groups.add(group);
        setModified();
    }

    /**
     * Remove a group from this unit
     * 
     * @param group
     *            the group to remove
     */
    void removeGroup(Group group) throws SQLException
    {
        this.groups.remove(group);
        setModified();

    }

    /**
     * Returns true or false based on whether a given group is a member.
     */
    public boolean isMember(Group group)
    {
        return this.groups.contains(group);
    }
}
