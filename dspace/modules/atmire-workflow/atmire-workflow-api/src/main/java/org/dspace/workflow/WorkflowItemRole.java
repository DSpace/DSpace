package org.dspace.workflow;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author
 */
/*
 * Represents a workflow assignments database representation.
 * These assignments describe roles and the groups connected
 * to these roles for each collection
 */
public class WorkflowItemRole {
    /** Our context */
    private Context myContext;

    /** The row in the table representing this object */
    private TableRow myRow;

    /**
     * Construct an ResourcePolicy
     *
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    WorkflowItemRole(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
    }

    public static WorkflowItemRole find(Context context, int id)
            throws SQLException
    {
        TableRow row = DatabaseManager.find(context, "workflowitemrole", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new WorkflowItemRole(context, row);
        }
    }

    public static WorkflowItemRole[] find(Context context, int workflowItemId, String role) throws SQLException {
         TableRowIterator tri = DatabaseManager.queryTable(context,"workflowitemrole",
                "SELECT * FROM workflowitemrole WHERE workflow_item_id= ? AND role_id= ? ",
                workflowItemId, role);

        ArrayList<WorkflowItemRole> roles = new ArrayList<WorkflowItemRole>();
        while(tri.hasNext()){

            roles.add(new WorkflowItemRole(context, tri.next()));
        }
        return roles.toArray(new WorkflowItemRole[roles.size()]);
    }

    public static WorkflowItemRole[] findAllForItem(Context context, int workflowItemId) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context,"workflowitemrole",
               "SELECT * FROM workflowitemrole WHERE workflow_item_id= ? ",
               workflowItemId);

       ArrayList<WorkflowItemRole> roles = new ArrayList<WorkflowItemRole>();
       while(tri.hasNext()){

           roles.add(new WorkflowItemRole(context, tri.next()));
       }
       return roles.toArray(new WorkflowItemRole[roles.size()]);
    }

    public static WorkflowItemRole create(Context context) throws SQLException,
            AuthorizeException {

        TableRow row = DatabaseManager.create(context, "workflowitemrole");

        return new WorkflowItemRole(context, row);
    }


    public void delete() throws SQLException
    {
        DatabaseManager.delete(myContext, myRow);
    }


    public void update() throws SQLException
    {
        DatabaseManager.update(myContext, myRow);
    }

    public void setRoleId(String id){
        myRow.setColumn("role_id",id);
    }

    public String getRoleId(){
        return myRow.getStringColumn("role_id");
    }

    public void setWorkflowItemId(int id){
        myRow.setColumn("workflow_item_id", id);
    }

    public int getWorkflowItemId(){
        return myRow.getIntColumn("workflow_item_id");
    }

    public void setEPerson(EPerson eperson){
        myRow.setColumn("eperson_id", eperson.getID());
    }

    public EPerson getEPerson() throws SQLException {
        return EPerson.find(myContext, myRow.getIntColumn("eperson_id"));
    }

    public void setGroup(Group group){
        myRow.setColumn("group_id", group.getID());
    }

    public Group getGroup() throws SQLException {
        return Group.find(myContext, myRow.getIntColumn("group_id"));
    }

}
