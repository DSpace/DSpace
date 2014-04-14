/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Claimed task representing the database representation of an action claimed by an eperson
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ClaimedTask {
     /** Our context */
    private Context myContext;

    /** The row in the table representing this object */
    private TableRow myRow;

    /**
     * Construct an Claimed Task
     *
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    ClaimedTask(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
    }

    public static ClaimedTask find(Context context, int id)
            throws SQLException {
        TableRow row = DatabaseManager.find(context, "cwf_claimtask", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new ClaimedTask(context, row);
        }
    }

    public static List<ClaimedTask> findByWorkflowId(Context context, int workflowID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context,"cwf_claimtask",
                "SELECT * FROM cwf_claimtask WHERE workflowitem_id= "+workflowID);
        List<ClaimedTask> list = new ArrayList<ClaimedTask>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            list.add(new ClaimedTask(context, row));
        }
        return list;
    }

    public static ClaimedTask findByWorkflowIdAndEPerson(Context context, int workflowID, int epersonID) throws SQLException {
        TableRow row = DatabaseManager.querySingleTable(context,"cwf_claimtask",
                "SELECT * FROM cwf_claimtask WHERE workflowitem_id= ? AND owner_id= ?", workflowID, epersonID);
        if(row == null)
            return null;
        else
            return new ClaimedTask(context, row);
    }

    public static List<ClaimedTask> findByEperson(Context context, int epersonID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context,"cwf_claimtask",
                "SELECT * FROM cwf_claimtask WHERE owner_id= "+epersonID);
        List<ClaimedTask> list = new ArrayList<ClaimedTask>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            list.add(new ClaimedTask(context, row));
        }
        return list;
    }

    public static List<ClaimedTask> find(Context c, int wfiID, String stepID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(c,"cwf_claimtask",
                "SELECT * FROM cwf_claimtask WHERE workflowitem_id="+wfiID+" AND step_id= ?", stepID);
        List<ClaimedTask> list = new ArrayList<ClaimedTask>();

        while(tri.hasNext()) {
            TableRow row = tri.next();
            list.add(new ClaimedTask(c, row));
        }
        return list;

    }

    public static ClaimedTask find(Context c, int epersonID, int wfiID, String stepID, String actionID) throws SQLException {
        TableRow row = DatabaseManager.querySingleTable(c,"cwf_claimtask",
                "SELECT * FROM cwf_claimtask WHERE workflowitem_id="+wfiID+" AND owner_id= "+epersonID+" AND action_id= ? AND step_id= ?",actionID, stepID);

        return new ClaimedTask(c, row);
    }
    public static List<ClaimedTask> find(Context c, int wfiID, String stepID, String actionID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(c,"cwf_claimtask",
                "SELECT * FROM cwf_claimtask WHERE workflowitem_id="+wfiID+" AND step_id= ? AND action_id=?", stepID, actionID);
        List<ClaimedTask> list = new ArrayList<ClaimedTask>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            list.add(new ClaimedTask(c, row));
        }
        return list;
    }

    public static List<ClaimedTask> find(Context c, XmlWorkflowItem workflowItem) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(c,"cwf_claimtask",
                "SELECT * FROM cwf_claimtask WHERE workflowitem_id="+workflowItem.getID());
        List<ClaimedTask> list = new ArrayList<ClaimedTask>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            list.add(new ClaimedTask(c, row));
        }
        return list;
    }

    public static List<ClaimedTask> findAllInStep(Context c, String stepID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(c,"cwf_claimtask", "SELECT * FROM cwf_claimtask WHERE step_id= ?", stepID);

        List<ClaimedTask> list = new ArrayList<ClaimedTask>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            list.add(new ClaimedTask(c, row));
        }
        return list;
    }

    public static ClaimedTask create(Context context) throws SQLException {

        TableRow row = DatabaseManager.create(context, "cwf_claimtask");

        return new ClaimedTask(context, row);
    }


    public void delete() throws SQLException
    {
        DatabaseManager.delete(myContext, myRow);
    }


    public void update() throws SQLException
    {
        DatabaseManager.update(myContext, myRow);
    }

    public void setOwnerID(int ownerID){
        myRow.setColumn("owner_id", ownerID);
    }
    public int getOwnerID(){
        return myRow.getIntColumn("owner_id");
    }
    public void setWorkflowItemID(int workflowItemID){
        myRow.setColumn("workflowitem_id", workflowItemID);
    }
    public int getWorkflowItemID(){
        return myRow.getIntColumn("workflowitem_id");
    }
    public void setActionID(String actionID){
        myRow.setColumn("action_id", actionID);
    }
    public String getActionID(){
        return myRow.getStringColumn("action_id");
    }
    public void setStepID(String stepID){
        myRow.setColumn("step_id", stepID);
    }
    public String getStepID(){
        return myRow.getStringColumn("step_id");
    }

    public void setWorkflowID(String workflowID){
        myRow.setColumn("workflow_id", workflowID);
    }

    public String getWorkflowID(){
        return myRow.getStringColumn("workflow_id");
    }
}
