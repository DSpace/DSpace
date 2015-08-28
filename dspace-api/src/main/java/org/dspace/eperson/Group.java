/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a group of e-people.
 * 
 * @author David Stuve
 * @version $Revision$
 */
@Entity
@Table(name = "epersongroup", schema = "public" )
public class Group extends DSpaceObject implements DSpaceObjectLegacySupport
{

    @Transient
    public static final String ANONYMOUS = "Anonymous";

    @Transient
    public static final String ADMIN = "Administrator";

    /**
     * Initial value is set to 2 since 0 & 1 are reserved for anonymous & administrative uses
     */
    @Column(name="eperson_group_id", insertable = false, updatable = false)
    private Integer legacyId;

    /** lists of epeople and groups in the group */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "public",
            name = "epersongroup2eperson",
            joinColumns = {@JoinColumn(name = "eperson_group_id") },
            inverseJoinColumns = {@JoinColumn(name = "eperson_id") }
    )
    private List<EPerson> epeople = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "public",
            name = "group2group",
            joinColumns = {@JoinColumn(name = "parent_id") },
            inverseJoinColumns = {@JoinColumn(name = "child_id") }
    )
    private List<Group> groups = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "groups")
    private List<Group> parentGroups = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "supervisorGroups")
    private List<WorkspaceItem> supervisedItems = new ArrayList<>();

    @Transient
    private boolean groupsChanged;

    @Transient
    private GroupService groupService;

    public Group() {
    }

    /**
     * get the ID of the group object
     *
     * @return id
     *
     * @deprecated use getID()
     */
    public int getLegacyID()
    {
        return legacyId;
    }

    void addMember(EPerson e)
    {
        getMembers().add(e);
    }

    /**
     * Return EPerson members of a Group
     */
    public List<EPerson> getMembers()
    {
        return epeople;
    }

    void addMember(Group g)
    {
        getMemberGroups().add(g);
        groupsChanged = true;
    }

    void addParentGroup(Group group)
    {
        getParentGroups().add(group);
        groupsChanged = true;
    }

    void removeParentGroup(Group group)
    {
        getParentGroups().remove(group);
        groupsChanged = true;
    }

    boolean remove(EPerson e)
    {
        return getMembers().remove(e);
    }

    boolean remove(Group g)
    {
        groupsChanged = true;
        return getMemberGroups().remove(g);
    }

    boolean contains(Group g)
    {
        return getMemberGroups().contains(g);
    }

    boolean contains(EPerson e)
    {
        return getMembers().contains(e);
    }

    List<Group> getParentGroups() {
        return parentGroups;
    }

    /**
     * Return Group members of a Group.
     */
    public List<Group> getMemberGroups()
    {
        return groups;
    }
    
    /**
     * Return <code>true</code> if <code>other</code> is the same Group as
     * this object, <code>false</code> otherwise
     * 
     * @param obj
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same group
     *         as this object
     */
     @Override
     public boolean equals(Object obj)
     {
         if (obj == null)
         {
             return false;
         }
         Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
         if (getClass() != objClass)
         {
             return false;
         }
         final Group other = (Group) obj;
         if (!this.getID().equals(other.getID()))
         {
             return false;
         }
         return true;
     }

     @Override
     public int hashCode()
     {
         int hash = 7;
         hash = 59 * hash + this.getID().hashCode();
         hash = 59 * hash + (this.getName() != null? this.getName().hashCode():0);
         return hash;
     }



    @Override
    public int getType()
    {
        return Constants.GROUP;
    }

    @Override
    public String getName()
    {
        return getGroupService().getName(this);
    }

    public void setName(Context context, String name) throws SQLException
    {
        getGroupService().setMetadataSingleValue(context, this, MetadataSchema.DC_SCHEMA, "title", null, null, name);
    }

    public boolean isGroupsChanged() {
        return groupsChanged;
    }

    public void clearGroupsChanged() {
        this.groupsChanged = false;
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    public List<WorkspaceItem> getSupervisedItems() {
        return supervisedItems;
    }

    private GroupService getGroupService() {
        if(groupService == null)
        {
            groupService = EPersonServiceFactory.getInstance().getGroupService();
        }
        return groupService;
    }
}
