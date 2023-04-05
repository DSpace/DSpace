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
import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.core.Constants;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Class representing a campus unit.
 *
 * @author Ben Wallberg
 */
@Entity
@Table(name = "unit")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class Unit extends DSpaceObject implements DSpaceObjectLegacySupport {
    @Column(name = "unit_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name = "name", length = 256, unique = true)
    private String name;

    @Column(name = "faculty_only")
    private boolean facultyOnly;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "epersongroup2unit",
            joinColumns = { @JoinColumn(name = "unit_id") },
            inverseJoinColumns = { @JoinColumn(name = "eperson_group_id") }
    )
    private final Set<Group> groups = new HashSet<>();

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.UnitService#create(Context)}
     */
    protected Unit () {
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    /**
     * get name of unit
     *
     * @return name
     */
    @Override
    public String getName() {
        return this.name == null ? "" : this.name;
    }

    /**
     * set name of unit
     *
     * @param name new unit name
     */
    public void setName(String name) {
        this.name = name;
        setModified();
    }

    /**
     * Sets whether this unit is faculty-only
     *
     * @param facultyOnly true if the unit is faculty-only, false otherwise.
     */
    public void setFacultyOnly(boolean facultyOnly) {
        this.facultyOnly = facultyOnly;
        setModified();
    }

    /**
     * Returns true if this is a faculty-only unit, false otherwise.
     *
     * @return true if this is a faculty-only unit, false otherwise.
     */
    public boolean getFacultyOnly() {
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
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Unit)) {
            return false;
        }
        Unit otherUnit = (Unit) other;

        UUID otherUnitId = otherUnit.getID();
        UUID unitId = getID();

        if ((unitId == null) || (otherUnitId == null)) {
            return false;
        }
        return unitId.equals(otherUnitId);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return 0;
    }

    @Override
    public int getType() {
        return Constants.UNIT;
    }

    @Override
    public String getHandle() {
        return null;
    }

    /**
     * Get the groups this unit maps to
     *
     * @return array of <code>Group</code> s this unit maps to
     * @throws SQLException if a database error occurs
     */
    public List<Group> getGroups() throws SQLException {
        return new ArrayList<>(this.groups);
    }

    /**
     * Add an existing group to this unit
     *
     * @param group the group to add
     */
    void addGroup(Group group) throws SQLException {
        groups.add(group);
        setModified();
    }

    /**
     * Remove a group from this unit
     *
     * @param group the group to remove
     */
    void removeGroup(Group group) throws SQLException {
        this.groups.remove(group);
        setModified();
    }

    /**
     * Returns true if the given Group is a member of this unit, false
     * otherwise.
     *
     * @param group the group to check for membership in this unit.
     * @return true if the given Group is a member of this unit, false otherwise
     */
    public boolean isMember(Group group) {
        return this.groups.contains(group);
    }
}
