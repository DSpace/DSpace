/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import java.sql.SQLException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.Group;

/**
 * Represents a workflow assignments database representation.
 * These assignments describe roles and the groups connected
 * to these roles for each collection
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
@Entity
@Table(name = "cwf_collectionrole")
public class CollectionRole implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "collectionrole_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cwf_collectionrole_seq")
    @SequenceGenerator(name = "cwf_collectionrole_seq", sequenceName = "cwf_collectionrole_seq", allocationSize = 1)
    private Integer id;

    //    @Column(name = "role_id")
    //    @Lob
    @Column(name = "role_id", columnDefinition = "text")
    private String roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    /**
     * Protected constructor, create object using:
     * {@link
     * org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService#create(Context, Collection, String, Group)
     * }
     */
    protected CollectionRole() {

    }

    public void setRoleId(String id) {
        this.roleId = id;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Group getGroup() throws SQLException {
        return group;
    }

    public Integer getID() {
        return id;
    }
}
