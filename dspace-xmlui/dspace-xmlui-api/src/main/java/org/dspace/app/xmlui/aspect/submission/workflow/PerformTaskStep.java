/*
 * PerformTaskStep.java
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.submission.workflow;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.xml.sax.SAXException;

/**
 * 
 * This step displays a workfrow item to the user and and presents several
 * possible actions that they may preform on the task.
 * 
 * General the user may, accept the item, reject the item, or edit the item's
 * metadata before accepting or rejecting. The user is also given the option
 * of taking the task or returning it to the pool.
 * 
 * @author Scott Phillips
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
	
	
	/** Copy the workflow manager's state values so that we can refrence them easier. */
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
		
		// if the user selected showsimple, remove showfull.
		if (showfull != null && request.getParameter("showsimple") != null)
			showfull = null;
		
		
        // Generate a from asking the user two questions: multiple 
        // titles & published before.
    	Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead(T_workflow_head);
    	
    	
        if (showfull == null)
        {
	        ReferenceSet referenceSet = div.addReferenceSet("narf",ReferenceSet.TYPE_SUMMARY_VIEW);
	        referenceSet.addReference(item);
	        div.addPara().addButton("showfull").setValue(T_showfull);
        } 
        else
        {
            ReferenceSet referenceSet = div.addReferenceSet("narf",ReferenceSet.TYPE_DETAIL_VIEW);
            referenceSet.addReference(item);
            div.addPara().addButton("showsimple").setValue(T_showsimple);
            
            div.addHidden("showfull").setValue("true");
        }
      
		   
        //FIXME: set the correct table size.
        Table table = div.addTable("workflow-actions", 1, 1);
        table.setHead(T_info1);
        
        // Header
        Row row;

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
        
        if (state == WFSTATE_STEP2)
        {
	        // Edit metadata
	        row = table.addRow();
	        row.addCellContent(T_edit_help);
	        row.addCell().addButton("submit_edit").setValue(T_edit_submit);
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
