/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.workflow;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.xml.sax.SAXException;

/**
 * 
 * This step displays a workflow item to the user and and presents several
 * possible actions that they may perform on the task.
 * 
 * In general, the user may accept the item, reject the item, or edit the item's
 * metadata before accepting or rejecting. The user is also given the option
 * of taking the task or returning it to the pool.
 * 
 * based on class by Scott Phillips
 * modified for LINDAT/CLARIN
 */
public class PerformTaskStep extends AbstractStep
{
	
    /** Language Strings **/
    protected static final Message T_info1= 
        message("xmlui.Submission.workflow.PerformTaskStep.info1");
    protected static final Message T_take_help = 
        message("xmlui.Submission.workflow.PerformTaskStep.take_help");
    protected static final Message T_take_submit = 
        message("xmlui.Submission.workflow.PerformTaskStep.take_submit");
    protected static final Message T_leave_help = 
        message("xmlui.Submission.workflow.PerformTaskStep.leave_help");
    protected static final Message T_leave_submit = 
        message("xmlui.Submission.workflow.PerformTaskStep.leave_submit");
    protected static final Message T_approve_help = 
        message("xmlui.Submission.workflow.PerformTaskStep.approve_help");
    protected static final Message T_approve_submit = 
        message("xmlui.Submission.workflow.PerformTaskStep.approve_submit");
    protected static final Message T_commit_help = 
        message("xmlui.Submission.workflow.PerformTaskStep.commit_help");
    protected static final Message T_commit_submit = 
        message("xmlui.Submission.workflow.PerformTaskStep.commit_submit");
    protected static final Message T_reject_help = 
        message("xmlui.Submission.workflow.PerformTaskStep.reject_help");
    protected static final Message T_reject_submit =
        message("xmlui.Submission.workflow.PerformTaskStep.reject_submit");
    protected static final Message T_edit_help = 
        message("xmlui.Submission.workflow.PerformTaskStep.edit_help");
    protected static final Message T_edit_submit = 
        message("xmlui.Submission.workflow.PerformTaskStep.edit_submit");
    protected static final Message T_return_help = 
        message("xmlui.Submission.workflow.PerformTaskStep.return_help");
    protected static final Message T_return_submit = 
        message("xmlui.Submission.workflow.PerformTaskStep.return_submit");
    protected static final Message T_cancel_submit = 
        message("xmlui.general.cancel");
    protected static final Message T_curate_help =
		message("xmlui.Submission.workflow.PerformTaskStep.curate_help");
    protected static final Message T_approve_curate = 
		message("xmlui.Submission.workflow.PerformTaskStep.curate_submit");	
    protected static final Message T_record_format_help = 
		message("xmlui.Submission.workflow.PerformTaskStep.record_format_help");
    protected static final Message T_label_move = 
		message("xmlui.administrative.item.EditItemStatusForm.label_move");
    protected static final Message T_submit_move = 
		message("xmlui.administrative.item.EditItemStatusForm.submit_move");
    protected static final Message T_head_parent_collections = 
		message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");
	
	/** Copy the workflow manager's state values so that we can reference them easier. */
	private static final int WFSTATE_STEP1POOL = WorkflowManager.WFSTATE_STEP1POOL;
	private static final int WFSTATE_STEP1     = WorkflowManager.WFSTATE_STEP1;
	private static final int WFSTATE_STEP2POOL = WorkflowManager.WFSTATE_STEP2POOL;
	private static final int WFSTATE_STEP2     = WorkflowManager.WFSTATE_STEP2;
	private static final int WFSTATE_STEP3POOL = WorkflowManager.WFSTATE_STEP3POOL;
	private static final int WFSTATE_STEP3     = WorkflowManager.WFSTATE_STEP3;
	
	
	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public PerformTaskStep()
	{
		this.requireWorkflow = true;
	}
	
	
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	// Get any metadata that may be removed by unselecting one of these options.
    	Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow";
		int state = ((WorkflowItem) submission).getState();
    	
    	Request request = ObjectModelHelper.getRequest(objectModel);
		String showfull = request.getParameter("showfull");
		
        // Generate a from asking the user two questions: multiple
        // titles & published before.
        Division div = body.addInteractiveDivision("perform-task", actionURL,
                Division.METHOD_POST, "primary workflow");
        div.setHead(T_workflow_head);
        
        Metadatum[] dcv = item.getMetadataByMetadataString("local.submission.note");
        if(dcv != null && dcv.length>0){
        	Division note = div.addDivision("note", "alert alert-info");
        	note.setHead("User's note:");
        	note.addPara().addContent(dcv[0].value);
        }

        ReferenceSet appearsInclude;

		// if the user selected showsimple, remove showfull.
		if (showfull != null && request.getParameter("showsimple") != null)
        {
            showfull = null;
        }
		
        if (showfull == null)
        {
            ReferenceSet referenceSet = div.addReferenceSet("narf",
                    ReferenceSet.TYPE_SUMMARY_VIEW);
            appearsInclude = referenceSet.addReference(item).addReferenceSet(
                    ReferenceSet.TYPE_DETAIL_LIST, null, "hierarchy");
        } 
        else
        {
            div.addHidden("showfull").setValue("true");
            ReferenceSet referenceSet = div.addReferenceSet("narf",
                    ReferenceSet.TYPE_DETAIL_VIEW);
            appearsInclude = referenceSet.addReference(item).addReferenceSet(
                    ReferenceSet.TYPE_DETAIL_LIST, null, "hierarchy");
        }
      
        appearsInclude.setHead(T_head_parent_collections);
        appearsInclude.addReference(collection);
		   
        // FIXME: set the correct table size.
        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);
        
        // Header
        Row row;

        if (showfull == null)
        {
            // Show full item record
            row = table.addRow();
            row.addCellContent(T_record_format_help);
            row.addCell().addButton("showfull").setValue(T_showfull);
        }
        else
        {
            // Show simple item record
            row = table.addRow();
            row.addCellContent(T_record_format_help);
            row.addCell().addButton("showsimple").setValue(T_showsimple);
        }

        if (state == WFSTATE_STEP1POOL ||
        	state == WFSTATE_STEP2POOL ||
        	state == WFSTATE_STEP3POOL)
        {
	        // Take task
	        row = table.addRow();
	        row.addCellContent(T_take_help);
	        row.addCell().addButton("submit_take_task").setValue(T_take_submit);
	     
	        // Leave task
	        row = table.addRow();
	        row.addCellContent(T_leave_help);
	        row.addCell().addButton("submit_leave").setValue(T_leave_submit);
        }
        
        if (state == WFSTATE_STEP1 ||
        	state == WFSTATE_STEP2)
        {
	        // Approve task
	        row = table.addRow();
	        row.addCellContent(T_approve_help);
	        row.addCell().addButton("submit_approve").setValue(T_approve_submit);
            // Check task
            row = table.addRow();
            row.addCellContent(T_curate_help);
            row.addCell().addButton("submit_curate").setValue(T_approve_curate);
        }
        
        if (state == WFSTATE_STEP3)
        {
	        // Commit to archive
	        row = table.addRow();
	        row.addCellContent(T_commit_help);
	        row.addCell().addButton("submit_approve").setValue(T_commit_submit);
        }
        
        if (state == WFSTATE_STEP1 ||
        	state == WFSTATE_STEP2)
        {
	        // Reject item
	        row = table.addRow();
	        row.addCellContent(T_reject_help);
	        row.addCell().addButton("submit_reject").setValue(T_reject_submit);
        }
        
        if (state == WFSTATE_STEP2 ||
        	state == WFSTATE_STEP3 )
        {
	        // Edit metadata
	        row = table.addRow();
	        row.addCellContent(T_edit_help);
	        row.addCell().addButton("submit_edit").setValue(T_edit_submit);
        }
        
		if (state == WFSTATE_STEP2 || 
			state == WFSTATE_STEP3)
        {
            // Move workflow item
            Collection[] collections = Collection.findAuthorized(context, null,
                    Constants.ADD);
            row = table.addRow();
            row.addCellContent(T_label_move);
            Button submitMove = row.addCell().addButton("submit_move");
            submitMove.setValue(T_submit_move);
            if (collections.length < 1
                    || (collections.length == 1 && collections[0]
                            .equals(collection)))
            {
                submitMove.setDisabled();
            }

        }
        
        if (state == WFSTATE_STEP1 ||
            state == WFSTATE_STEP2 ||
            state == WFSTATE_STEP3 )
        {
	        // Return to pool
	        row = table.addRow();
	        row.addCellContent(T_return_help);
	        row.addCell().addButton("submit_return").setValue(T_return_submit);
        }
        
        
        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue(T_cancel_submit);
        
        div.addHidden("submission-continue").setValue(knot.getId()); 
    }
}
