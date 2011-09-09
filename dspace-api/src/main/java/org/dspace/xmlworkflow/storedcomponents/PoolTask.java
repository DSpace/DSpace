/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xmlworkflow.WorkflowRequirementsManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Pool task representing the database representation of a pool task for a step and an eperson
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class PoolTask {
     /** Our context */
    private Context myContext;

    /** The row in the table representing this object */
    private TableRow myRow;

    /**
     * Construct an PoolTask
     *
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    PoolTask(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
    }

    public static PoolTask find(Context context, int id)
            throws SQLException {
        TableRow row = DatabaseManager.find(context, "cwf_pooltask", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new PoolTask(context, row);
        }
    }

    public static List<PoolTask> findByEperson(Context context, int eperson_id) throws SQLException, AuthorizeException, IOException {
        TableRowIterator tri = DatabaseManager.queryTable(context,"cwf_pooltask",
                "SELECT * FROM cwf_pooltask WHERE eperson_id= "+eperson_id);
        //Hashmap to map workflow item id's to pooltasks. This will allow to have a list of unique workflowitems for which
        //the user will see PoolTasks
        HashMap<Integer, PoolTask> tasks = new HashMap<Integer, PoolTask>();
        //Get all PoolTasks for a specific eperson
        while(tri.hasNext()){
            TableRow row = tri.next();
            PoolTask task = new PoolTask(context, row);
            tasks.put(task.getWorkflowItemID(), task);
        }
        tri.close();
        //Get all PoolTasks for groups of which this eperson is a member
        for(Group group: Group.allMemberGroups(context, EPerson.find(context, eperson_id))){
            tri = DatabaseManager.queryTable(context,"cwf_pooltask",
                    "SELECT * FROM cwf_pooltask WHERE group_id= "+group.getID());
            while(tri.hasNext()){
                TableRow row = tri.next();
                PoolTask task = new PoolTask(context, row);
                XmlWorkflowItem wfi = XmlWorkflowItem.find(context, task.getWorkflowItemID());
                //If the user has not claimed and not finished the step, return a pooltask for the user
                if(!(InProgressUser.findByWorkflowItemAndEPerson(context, wfi.getID(), eperson_id)!=null)){
                    tasks.put(task.getWorkflowItemID(), task);
                }
            }
            tri.close();
        }
        return new ArrayList(tasks.values());
    }

    public static List<PoolTask> find(Context context, XmlWorkflowItem workflowItem) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context,"cwf_pooltask",
                "SELECT * FROM cwf_pooltask WHERE workflowitem_id= "+workflowItem.getID());
        List<PoolTask> list = new ArrayList<PoolTask>();
        while(tri.hasNext()){
            TableRow row = tri.next();
            list.add(new PoolTask(context, row));
        }
        tri.close();
        return list;
    }

    public static PoolTask findByWorkflowIdAndEPerson(Context context, int workflowID, int epersonID) throws SQLException, AuthorizeException, IOException {
        TableRow row = DatabaseManager.querySingleTable(context,"cwf_pooltask",
                "SELECT * FROM cwf_pooltask WHERE workflowitem_id= ? AND eperson_id = ?", workflowID, epersonID);
        //If there is a pooltask for this eperson, return it
        if(row != null)
            return new PoolTask(context, row);
        else{
            //If the user has a is processing or has finished the step for a workflowitem, there is no need to look for pooltasks for one of his
            //groups because the user already has the task claimed
            XmlWorkflowItem wfi = XmlWorkflowItem.find(context, workflowID);
            if(InProgressUser.findByWorkflowItemAndEPerson(context, workflowID, epersonID)!=null){
                return null;
            }
            else{
                //If the user does not have a claimedtask yet, see whether one of the groups of the user has pooltasks
                //for this workflow item
                for(Group group: Group.allMemberGroups(context, EPerson.find(context, epersonID))){
                    row = DatabaseManager.querySingleTable(context,"cwf_pooltask",
                        "SELECT * FROM cwf_pooltask WHERE workflowitem_id= ? AND group_id = ?", workflowID, group.getID());
                    if(row != null){
                        return new PoolTask(context, row);
                    }
                }
            }
        }
        return null;
    }
    public static PoolTask create(Context context) throws SQLException {

        TableRow row = DatabaseManager.create(context, "cwf_pooltask");

        return new PoolTask(context, row);
    }


    public void delete() throws SQLException
    {
        DatabaseManager.delete(myContext, myRow);
    }


    public void update() throws SQLException
    {
        DatabaseManager.update(myContext, myRow);
    }

    public void setEpersonID(int id){
        myRow.setColumn("eperson_id", id);
    }

    public int getEpersonID(){
        return myRow.getIntColumn("eperson_id");
    }

    public void setGroupID(int id){
        myRow.setColumn("group_id", id);
    }

    public int getGroupID(){
        return myRow.getIntColumn("group_id");
    }

    public void setWorkflowID(String id){
        myRow.setColumn("workflow_id", id);
    }

    public String getWorkflowID(){
        return myRow.getStringColumn("workflow_id");
    }

    public void setWorkflowItemID(int id){
        myRow.setColumn("workflowitem_id", id);
    }

    public int getWorkflowItemID(){
        return myRow.getIntColumn("workflowitem_id");
    }

    public void setStepID(String stepID){
        myRow.setColumn("step_id", stepID);
    }

    public String getStepID() throws SQLException {
        return myRow.getStringColumn("step_id");
    }

    public void setActionID(String actionID){
        myRow.setColumn("action_id", actionID);
    }

    public String getActionID(){
        return myRow.getStringColumn("action_id");
    }

}
