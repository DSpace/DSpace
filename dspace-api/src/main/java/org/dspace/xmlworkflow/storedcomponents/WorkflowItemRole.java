/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import javax.persistence.*;
import java.sql.SQLException;

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
@Table(name="cwf_workflowitemrole")
public class WorkflowItemRole implements ReloadableEntity<Integer> {

    @Id
    @Column(name="workflowitemrole_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="cwf_workflowitemrole_seq")
    @SequenceGenerator(name="cwf_workflowitemrole_seq", sequenceName="cwf_workflowitemrole_seq", allocationSize = 1)
    private Integer id;

//    @Column(name = "role_id")
//    @Lob
    @Column(name="role_id", columnDefinition = "text")
    private String roleId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflowitem_id")
    private XmlWorkflowItem workflowItem;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id")
    private EPerson ePerson;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService#create(Context)}
     *
     */
    protected WorkflowItemRole()
    {

    }


    public Integer getID() {
        return id;
    }

    public void setRoleId(String id){
        this.roleId = id;
    }

    public String getRoleId(){
        return this.roleId;
    }

    public void setWorkflowItem(XmlWorkflowItem xmlWorkflowItem){
        this.workflowItem = xmlWorkflowItem;
    }

    public XmlWorkflowItem getWorkflowItem(){
        return workflowItem;
    }

    public void setEPerson(EPerson eperson){
        this.ePerson = eperson;
    }

    public EPerson getEPerson() throws SQLException {
        return ePerson;
    }

    public void setGroup(Group group){
        this.group = group;
    }

    public Group getGroup() throws SQLException {
        return group;
    }
}
