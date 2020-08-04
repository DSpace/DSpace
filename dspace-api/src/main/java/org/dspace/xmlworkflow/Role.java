/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.WorkflowItemRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * The role that is responsible for a certain step
 * Can either be on a group in the repo, or a collection group
 * or an item role will check for workflowItemRoles
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Role implements BeanNameAware {

    @Autowired
    private GroupService groupService;
    @Autowired
    private CollectionRoleService collectionRoleService;
    @Autowired
    private WorkflowItemRoleService workflowItemRoleService;

    private String id;
    private String name;
    private String description;
    private boolean isInternal = false;
    private Scope scope = Scope.COLLECTION;

    @Override
    public void setBeanName(String s) {
        this.id = s;
    }

    public enum Scope {
        REPOSITORY,
        COLLECTION,
        ITEM
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }

    public boolean isInternal() {
        return isInternal;
    }

    public Scope getScope() {
        return scope;
    }

    public RoleMembers getMembers(Context context, XmlWorkflowItem wfi) throws SQLException {
        if (scope == Scope.REPOSITORY) {
            Group group = groupService.findByName(context, name);
            if (group == null) {
                return new RoleMembers();
            } else {
                RoleMembers assignees = new RoleMembers();
                assignees.addGroup(group);
                return assignees;
            }
        } else if (scope == Scope.COLLECTION) {
            CollectionRole collectionRole = collectionRoleService.find(context, wfi.getCollection(), id);
            if (collectionRole != null) {
                RoleMembers assignees = new RoleMembers();
                assignees.addGroup(collectionRole.getGroup());
                return assignees;
            }
            return new RoleMembers();
        } else {
            List<WorkflowItemRole> roles = workflowItemRoleService.find(context, wfi, id);
            RoleMembers assignees = new RoleMembers();
            for (WorkflowItemRole itemRole : roles) {
                EPerson user = itemRole.getEPerson();
                if (user != null) {
                    assignees.addEPerson(user);
                }

                Group group = itemRole.getGroup();
                if (group != null) {
                    assignees.addGroup(group);
                }
            }

            return assignees;
        }
    }

    /**
     * The name specified in the name attribute of a role will be used to lookup the in DSpace.
     * The lookup will depend on the scope specified in the "scope" attribute:
     * @param name
     */
    @Required
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the description of the role
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Set the scope attribute, depending on the scope the users will be retrieved in the following manner:
     * * collection: The collection value specifies that the group will be configured at the level of the collection.
     * * repository: The repository scope uses groups that are defined at repository level in DSpace.
     * item: The item scope assumes that a different action in the workflow will assign a number of EPersons or
     * Groups to a specific workflow-item in order to perform a step.
     * @param scope the scope parameter
     */
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * Optional attribute which isn't really used at the moment, false by default
     * @param internal if the role is internal
     */
    public void setInternal(boolean internal) {
        isInternal = internal;
    }
}
