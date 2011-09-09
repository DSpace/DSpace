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
public class InProgressUser {
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
    InProgressUser(Context context, TableRow row)
    {
        myContext = context;
        myRow = row;
    }

    public static InProgressUser find(Context context, int id)
            throws SQLException {
        TableRow row = DatabaseManager.find(context, "cwf_in_progress_user", id);

        if (row == null)
        {
            return null;
        }
        else
        {
            return new InProgressUser(context, row);
        }
    }

    public static InProgressUser findByWorkflowItemAndEPerson(Context context, int wfiID, int epersonID) throws SQLException {
        TableRow row = DatabaseManager.querySingleTable(context,"cwf_in_progress_user",
                "SELECT * FROM cwf_in_progress_user WHERE workflowitem_id= ? AND user_id= ?", wfiID, epersonID);
        if(row == null)
            return null;
        else
            return new InProgressUser(context, row);
    }

    public static List<InProgressUser> findByEperson(Context context, int epersonID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context,"cwf_in_progress_user",
                "SELECT * FROM cwf_in_progress_user WHERE user_id = "+epersonID);
        List<InProgressUser> list = new ArrayList<InProgressUser>();
        while(tri.hasNext()) {
            TableRow row = tri.next();
            list.add(new InProgressUser(context, row));
        }
        return list;
    }

    public static List<InProgressUser> findByWorkflowItem(Context c, int wfiID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(c,"cwf_in_progress_user",
                "SELECT * FROM cwf_in_progress_user WHERE workflowitem_id="+wfiID);
        List<InProgressUser> list = new ArrayList<InProgressUser>();

        while(tri.hasNext()) {
            TableRow row = tri.next();
            list.add(new InProgressUser(c, row));
        }
        return list;

    }

    public static int getNumberOfInProgressUsers(Context c, int wfiID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(c,"cwf_in_progress_user",
                "SELECT * FROM cwf_in_progress_user WHERE workflowitem_id="+wfiID+" AND finished= '0'");
        return tri.toList().size();
    }

    public static int getNumberOfFinishedUsers(Context c, int wfiID) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(c,"cwf_in_progress_user",
                "SELECT * FROM cwf_in_progress_user WHERE workflowitem_id="+wfiID+" AND finished= '1'");
        return tri.toList().size();
    }

    public static InProgressUser create(Context context) throws SQLException {

        TableRow row = DatabaseManager.create(context, "cwf_in_progress_user");

        return new InProgressUser(context, row);
    }


    public void delete() throws SQLException
    {
        DatabaseManager.delete(myContext, myRow);
    }


    public void update() throws SQLException
    {
        DatabaseManager.update(myContext, myRow);
    }

    public void setUserID(int userID){
        myRow.setColumn("user_id", userID);
    }
    public int getUserID(){
        return myRow.getIntColumn("user_id");
    }
    public void setWorkflowItemID(int workflowItemID){
        myRow.setColumn("workflowitem_id", workflowItemID);
    }
    public int getWorkflowItemID(){
        return myRow.getIntColumn("workflowitem_id");
    }
    public String getActionID(){
        return myRow.getStringColumn("action_id");
    }

    public void setWorkflowID(String workflowID){
        myRow.setColumn("workflow_id", workflowID);
    }

    public String getWorkflowID(){
        return myRow.getStringColumn("workflow_id");
    }

    public boolean isFinished(){
        return myRow.getBooleanColumn("finished");
    }

    public void setFinished(boolean finished){
        myRow.setColumn("finished", finished);
    }
}
