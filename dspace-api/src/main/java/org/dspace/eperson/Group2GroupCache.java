/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Database entity representation of the group2groupcache table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name = "group2groupcache" )
public class Group2GroupCache implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id", nullable = false)
    public Group parent;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "child_id", nullable = false)
    public Group child;

    public Group getParent() {
        return parent;
    }

    public void setParent(Group parent) {
        this.parent = parent;
    }

    public Group getChild() {
        return child;
    }

    public void setChild(Group child) {
        this.child = child;
    }

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.GroupService}
     *
     */
    protected Group2GroupCache()
    {

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
        {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (getClass() != objClass)
        {
            return false;
        }
        final Group2GroupCache other = (Group2GroupCache) obj;
        if(!parent.equals(other.getParent()))
        {
            return false;
        }
        if(!child.equals(other.getChild()))
        {
            return false;
        }
        return true;
    }
}
