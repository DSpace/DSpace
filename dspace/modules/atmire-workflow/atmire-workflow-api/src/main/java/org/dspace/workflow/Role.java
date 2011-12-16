package org.dspace.workflow;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 16-aug-2010
 * Time: 16:25:52
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

    public EPerson[] getMembers(Context context, WorkflowItem wfi) throws SQLException {
        if(scope == Scope.REPOSITORY){
            Group group = Group.findByName(context, name);
            if(group == null)
                return null;
            else
                return Group.allMembers(context, group);
        } else
        if(scope == Scope.COLLECTION){
            CollectionRole wa = CollectionRole.find(context,wfi.getCollection().getID(),id);
            if(wa != null){
                return Group.allMembers(context, wa.getGroup());
            }
        return null;
        }else{
            WorkflowItemRole[] roles = WorkflowItemRole.find(context, wfi.getID(), id);
            List<EPerson> result = new ArrayList<EPerson>();

            for (WorkflowItemRole itemRole : roles){
                EPerson user = itemRole.getEPerson();
                if(user != null)
                    result.add(user);

                Group group = itemRole.getGroup();
                if(group != null)
                    result.addAll(Arrays.asList(Group.allMembers(context, group)));
            }

            return result.toArray(new EPerson[result.size()]);
        }
    }

}
