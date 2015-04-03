/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.Collection;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.WorkflowManager;

public class ControlPanelUnpublishedItemsTab extends AbstractControlPanelTab {

	private static Logger log = Logger.getLogger(ControlPanelUnpublishedItemsTab.class);

    // The workflow status messages
    protected static final Message T_status_0 =
        message("xmlui.Submission.Submissions.status_0");
    protected static final Message T_status_1 =
        message("xmlui.Submission.Submissions.status_1");
    protected static final Message T_status_2 =
        message("xmlui.Submission.Submissions.status_2");
    protected static final Message T_status_3 =
        message("xmlui.Submission.Submissions.status_3");
    protected static final Message T_status_4 =
        message("xmlui.Submission.Submissions.status_4");
    protected static final Message T_status_5 =
        message("xmlui.Submission.Submissions.status_5");
    protected static final Message T_status_6 =
        message("xmlui.Submission.Submissions.status_6");
    protected static final Message T_status_7 =
        message("xmlui.Submission.Submissions.status_7");
    protected static final Message T_status_unknown =
        message("xmlui.Submission.Submissions.status_unknown");

		
	@Override
	public void addBody(Map objectModel, Division div) throws WingException, SQLException {
		
		Division wfdiv = div.addDivision("unpublished_items", "well well-light");
				
		wfdiv.setHead("WORKFLOW ITEMS");		
		wfdiv.addPara(null, "alert alert-info").addContent("Following items are currently in workflow mode i.e. waiting for an approval. You can view the details of the item by clicking the itemID");

		Table wftable = wfdiv.addTable("workspace_items", 1, 5);
		
		Row wfhead = wftable.addRow(Row.ROLE_HEADER);
		
		wfhead.addCellContent("WORKFLOW ID");
		wfhead.addCellContent("ITEM ID");
		wfhead.addCellContent("COLLECTION");
		wfhead.addCellContent("CURRENT STATE");
		wfhead.addCellContent("ASSIGNED TO");
		wfhead.addCellContent("LAST MODIFIED");
		
		String []row_classes = {"success", "error", "warning", "error", "warning", "error", "warning", "success", "error"};
		
		for(TableRow dbrow : getWorkflowItems()) {
			int state = dbrow.getIntColumn("state");
			Row wsrow = wftable.addRow(null, Row.ROLE_DATA, row_classes[state]);
			wsrow.addCell().addContent(dbrow.getIntColumn("workflow_id"));
			int itemID = dbrow.getIntColumn("item_id");			
			wsrow.addCell().addXref(contextPath + "/admin/item?cp=1&identifier=" + itemID).addContent(itemID);
			wsrow.addCell().addContent(Collection.find(context, dbrow.getIntColumn("collection_id")).getName());
			wsrow.addCell().addHighlight(getWorkflowStateClass(state)).addContent(getWorkflowStateMessage(state));			
			String owner = dbrow.getStringColumn("owner");
			if(owner!=null) {
				wsrow.addCellContent(owner);
			} else {
				wsrow.addCellContent("task pending");
			}
			wsrow.addCellContent(dbrow.getDateColumn("last_modified").toString());
		}
				
		Division wsdiv = div.addDivision("workspace_items", "well well-light");
				
		wsdiv.setHead("WORKSPACE ITEMS");		
		wsdiv.addPara(null, "alert alert-info").addContent("Following items are currently in workspace mode i.e. in the process of submission. You can view the details of the item by clicking the itemID");

		Table wstable = wsdiv.addTable("workspace_items", 1, 5);
		
		Row wshead = wstable.addRow(Row.ROLE_HEADER);
		
		wshead.addCellContent("WORKSPACE ID");
		wshead.addCellContent("ITEM ID");
		wshead.addCellContent("STAGE REACHED");
		wshead.addCellContent("SUBMITTER EMAIL");
		wshead.addCellContent("REJECTED");
		wshead.addCellContent("LAST MODIFIED");
		
		for(TableRow dbrow : getWorkspaceItems()) {
			int page_reached = dbrow.getIntColumn("page_reached");
			Row wsrow = wstable.addRow(null, Row.ROLE_DATA, page_reached == Integer.MAX_VALUE ? "error" : "warning");
			wsrow.addCell().addContent(dbrow.getIntColumn("workspace_item_id"));
			int itemID = dbrow.getIntColumn("item_id");			
			wsrow.addCell().addXref(contextPath + "/admin/item?cp=1&identifier=" + itemID).addContent(itemID);			
			wsrow.addCell().addContent(dbrow.getIntColumn("stage_reached"));
			wsrow.addCellContent(dbrow.getStringColumn("submitter"));
			if(page_reached == Integer.MAX_VALUE) {
				wsrow.addCell().addHighlight("label label-important").addContent("Rejected");
			} else {
				wsrow.addCell().addHighlight("label label-warning").addContent("Not Submitted");
			}
			wsrow.addCellContent(dbrow.getDateColumn("last_modified").toString());
		}
				
		
    }
	
	
	private List<TableRow> getWorkspaceItems() throws SQLException {
		String query =
				"SELECT w.workspace_item_id, " +
				"		w.item_id, " +
				"		w.stage_reached, " +
				"		coalesce(e.email, '') as submitter, " +
				"		w.page_reached, " +
				"		i.last_modified " +
				"FROM workspaceitem w JOIN item i ON (w.item_id = i.item_id) " +
				"					  LEFT JOIN eperson e ON (i.submitter_id = e.eperson_id) " +
				"ORDER BY i.last_modified desc;";
		List<TableRow> result = DatabaseManager.query(context, query).toList();
		return result;
	}
	

	private List<TableRow> getWorkflowItems() throws SQLException {
		String query =
				"SELECT w.workflow_id, " +
				"		w.item_id, " +
				"		w.collection_id, " +
				"		w.state, " +
				"		e.email as owner, " +
				"		i.last_modified " +
				"FROM workflowitem w JOIN item i ON (w.item_id = i.item_id)" +
				"					 JOIN collection c ON (w.collection_id = c.collection_id) " +
				"					 LEFT JOIN eperson e ON (w.owner = e.eperson_id) " +
				"ORDER BY i.last_modified desc;";
		List<TableRow> result = DatabaseManager.query(context, query).toList();
		return result;
	}
	
    /**
     * Determine the correct message that describes this workflow item's state.
     */
    private Message getWorkflowStateMessage(int state)
    {
		switch (state)
		{
			case WorkflowManager.WFSTATE_SUBMIT:
				return T_status_0;
			case WorkflowManager.WFSTATE_STEP1POOL:
				return T_status_1;
    		case WorkflowManager.WFSTATE_STEP1:
    			return T_status_2;
    		case WorkflowManager.WFSTATE_STEP2POOL:
    			return T_status_3;
    		case WorkflowManager.WFSTATE_STEP2:
    			return T_status_4;
    		case WorkflowManager.WFSTATE_STEP3POOL:
    			return T_status_5;
    		case WorkflowManager.WFSTATE_STEP3:
    			return T_status_6;
    		case WorkflowManager.WFSTATE_ARCHIVE:
    			return T_status_7;
   			default:
   				return T_status_unknown;
		}
    }


    /**
     * Determine the correct class to change the label color of state
     */
    private String getWorkflowStateClass(int state)
    {
		switch (state)
		{
			case WorkflowManager.WFSTATE_SUBMIT:
				return "label label-success";
			case WorkflowManager.WFSTATE_STEP1POOL:
				return "label label-important";
    		case WorkflowManager.WFSTATE_STEP1:
    			return "label label-warning";
    		case WorkflowManager.WFSTATE_STEP2POOL:
    			return "label label-important";
    		case WorkflowManager.WFSTATE_STEP2:
    			return "label label-warning";
    		case WorkflowManager.WFSTATE_STEP3POOL:
    			return "label label-important";
    		case WorkflowManager.WFSTATE_STEP3:
    			return "label label-warning";
    		case WorkflowManager.WFSTATE_ARCHIVE:
    			return "label label-success";
   			default:
   				return "label label-error";
		}
    }

}





