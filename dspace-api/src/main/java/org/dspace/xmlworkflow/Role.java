/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.*;

import java.sql.SQLException;

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
public class Role {

    private String id;
    private String name;
    private String description;
    private boolean isInternal;
    private Scope scope;

    public static enum Scope{
        REPOSITORY,
        COLLECTION,
        ITEM
    }

    public Role(String id, String name, String description, boolean isInternal, Scope scope){
        this.id = id;
        this.name = name;
        this.description = description;
        this.isInternal = isInternal;
        this.scope = scope;
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
        if(scope == Scope.REPOSITORY){
            Group group = Group.findByName(context, name);
            if(group == null)
                return new RoleMembers();
            else{
                RoleMembers assignees =  new RoleMembers();
                assignees.addGroup(group);
                return assignees;
            }
        } else
        if(scope == Scope.COLLECTION){
            CollectionRole collectionRole = CollectionRole.find(context,wfi.getCollection().getID(),id);
            if(collectionRole != null){
                RoleMembers assignees =  new RoleMembers();
                assignees.addGroup(collectionRole.getGroup());
                return assignees;
            }
            return new RoleMembers();
        }else{
            WorkflowItemRole[] roles = WorkflowItemRole.find(context, wfi.getID(), id);
            RoleMembers assignees = new RoleMembers();
            for (WorkflowItemRole itemRole : roles){
                EPerson user = itemRole.getEPerson();
                if(user != null)
                    assignees.addEPerson(user);

                Group group = itemRole.getGroup();
                if(group != null)
                    assignees.addGroup(group);
            }

            return assignees;
        }
    }

}
