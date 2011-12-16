package org.dspace.workflow;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Bram De Schouwer
 */
/*
 * Pool task representing the database representation of a pool task for a step and an eperson
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
        TableRow row = DatabaseManager.find(context, "tasklistitem", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new PoolTask(context, row);
        }
    }

    public static List<PoolTask> findByEperson(Context context, int eperson_id) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context,"tasklistitem",
                "SELECT * FROM tasklistitem WHERE eperson_id= "+eperson_id);
        List<PoolTask> list = new ArrayList<PoolTask>();
        while(tri.hasNext()){
            TableRow row = tri.next();
            list.add(new PoolTask(context, row));
        }
        tri.close();
        return list;
    }

    public static List<PoolTask> find(Context context, WorkflowItem workflowItem) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context,"tasklistitem",
                "SELECT * FROM tasklistitem WHERE workflow_item_id= "+workflowItem.getID());
        List<PoolTask> list = new ArrayList<PoolTask>();
        while(tri.hasNext()){
            TableRow row = tri.next();
            list.add(new PoolTask(context, row));
        }
        tri.close();
        return list;
    }

    public static PoolTask findByWorkflowIdAndEPerson(Context context, int workflowID, int epersonID) throws SQLException {
        TableRow row = DatabaseManager.querySingleTable(context,"tasklistitem",
                "SELECT * FROM tasklistitem WHERE workflow_item_id= ? AND eperson_id = ?", workflowID, epersonID);

        if(row != null)
            return new PoolTask(context, row);
        else
            return null;
    }
    static PoolTask create(Context context) throws SQLException {

        TableRow row = DatabaseManager.create(context, "tasklistitem");

        return new PoolTask(context, row);
    }


    void delete() throws SQLException
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

    public void setWorkflowID(String id){
        myRow.setColumn("workflow_id", id);
    }

    public String getWorkflowID(){
        return myRow.getStringColumn("workflow_id");
    }

    public void setWorkflowItemID(int id){
        myRow.setColumn("workflow_item_id", id);
    }

    public int getWorkflowItemID(){
        return myRow.getIntColumn("workflow_item_id");
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
