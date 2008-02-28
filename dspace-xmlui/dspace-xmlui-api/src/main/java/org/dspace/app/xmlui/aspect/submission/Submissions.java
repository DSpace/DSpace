/*
 * Submissions.java
 *
 * Version: $Revision: 1.21 $
 *
 * Date: $Date: 2006/07/27 18:24:34 $
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
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.SupervisedItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.xml.sax.SAXException;

/**
 * @author Scott Phillips
 */
public class Submissions extends AbstractDSpaceTransformer
{
	/** General Language Strings */
    protected static final Message T_title = 
        message("xmlui.Submission.Submissions.title");
    protected static final Message T_dspace_home = 
        message("xmlui.general.dspace_home"); 
    protected static final Message T_trail = 
        message("xmlui.Submission.Submissions.trail");
    protected static final Message T_head = 
        message("xmlui.Submission.Submissions.head");
    protected static final Message T_untitled = 
        message("xmlui.Submission.Submissions.untitled");
    protected static final Message T_email = 
        message("xmlui.Submission.Submissions.email");
    
    // used by the workflow section
    protected static final Message T_w_head1 = 
        message("xmlui.Submission.Submissions.workflow_head1");    
    protected static final Message T_w_info1 = 
        message("xmlui.Submission.Submissions.workflow_info1"); 
    protected static final Message T_w_head2 = 
        message("xmlui.Submission.Submissions.workflow_head2"); 
    protected static final Message T_w_column1 = 
        message("xmlui.Submission.Submissions.workflow_column1"); 
    protected static final Message T_w_column2 = 
        message("xmlui.Submission.Submissions.workflow_column2"); 
    protected static final Message T_w_column3 = 
        message("xmlui.Submission.Submissions.workflow_column3"); 
    protected static final Message T_w_column4 = 
        message("xmlui.Submission.Submissions.workflow_column4"); 
    protected static final Message T_w_column5 = 
        message("xmlui.Submission.Submissions.workflow_column5"); 
    protected static final Message T_w_submit_return = 
        message("xmlui.Submission.Submissions.workflow_submit_return"); 
    protected static final Message T_w_info2 = 
        message("xmlui.Submission.Submissions.workflow_info2"); 
    protected static final Message T_w_head3 = 
        message("xmlui.Submission.Submissions.workflow_head3"); 
    protected static final Message T_w_submit_take = 
        message("xmlui.Submission.Submissions.workflow_submit_take"); 
    protected static final Message T_w_info3 = 
        message("xmlui.Submission.Submissions.workflow_info3"); 
   
    // used by the unfinished submissions section
    protected static final Message T_s_head1 = 
        message("xmlui.Submission.Submissions.submit_head1"); 
    protected static final Message T_s_info1a = 
        message("xmlui.Submission.Submissions.submit_info1a"); 
    protected static final Message T_s_info1b = 
        message("xmlui.Submission.Submissions.submit_info1b"); 
    protected static final Message T_s_info1c = 
        message("xmlui.Submission.Submissions.submit_info1c"); 
    protected static final Message T_s_head2 = 
        message("xmlui.Submission.Submissions.submit_head2"); 
    protected static final Message T_s_info2a = 
        message("xmlui.Submission.Submissions.submit_info2a"); 
    protected static final Message T_s_info2b = 
        message("xmlui.Submission.Submissions.submit_info2b"); 
    protected static final Message T_s_info2c = 
        message("xmlui.Submission.Submissions.submit_info2c"); 
    protected static final Message T_s_column1 = 
        message("xmlui.Submission.Submissions.submit_column1"); 
    protected static final Message T_s_column2 = 
        message("xmlui.Submission.Submissions.submit_column2"); 
    protected static final Message T_s_column3 = 
        message("xmlui.Submission.Submissions.submit_column3"); 
    protected static final Message T_s_column4 = 
        message("xmlui.Submission.Submissions.submit_column4"); 
    protected static final Message T_s_head3 = 
        message("xmlui.Submission.Submissions.submit_head3"); 
    protected static final Message T_s_info3 = 
        message("xmlui.Submission.Submissions.submit_info3"); 
    protected static final Message T_s_head4 = 
        message("xmlui.Submission.Submissions.submit_head4"); 
    protected static final Message T_s_submit_remove = 
        message("xmlui.Submission.Submissions.submit_submit_remove"); 
    
	// Used in the in progress section 
    protected static final Message T_p_head1 = 
        message("xmlui.Submission.Submissions.progress_head1"); 
    protected static final Message T_p_info1 = 
        message("xmlui.Submission.Submissions.progress_info1"); 
    protected static final Message T_p_column1 = 
        message("xmlui.Submission.Submissions.progress_column1"); 
    protected static final Message T_p_column2 = 
        message("xmlui.Submission.Submissions.progress_column2"); 
    protected static final Message T_p_column3 = 
        message("xmlui.Submission.Submissions.progress_column3"); 

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

	
	public void addPageMeta(PageMeta pageMeta) throws SAXException,
	WingException, UIException, SQLException, IOException,
	AuthorizeException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/submissions",T_trail);
	}
	
  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	        
        Division div = body.addInteractiveDivision("submissions", contextPath+"/submissions", Division.METHOD_POST,"primary");
        div.setHead(T_head);
        
        this.addWorkflowTasks(div);
        this.addUnfinishedSubmissions(div);
        this.addSubmissionsInWorkflow(div);
    }
    
    
    /**
     * If the user has any workflow tasks, either assigned to them or in an 
     * available pool of tasks, then build two tables listing each of these queues.
     * 
     * If the user dosn't have any workflows then don't do anything.
     * 
     * @param division The division to add the two queues too.
     */
    private void addWorkflowTasks(Division division) throws SQLException, WingException
    {
    	@SuppressWarnings("unchecked") // This cast is correct
    	java.util.List<WorkflowItem> ownedItems = WorkflowManager.getOwnedTasks(context, context
                .getCurrentUser());
    	@SuppressWarnings("unchecked") // This cast is correct.
    	java.util.List<WorkflowItem> pooledItems = WorkflowManager.getPooledTasks(context, context
                .getCurrentUser());
    	
    	if (!(ownedItems.size() > 0 || pooledItems.size() > 0))
    		// No tasks, so don't show the table.
    		return;
    	
    	
    	Division workflow = division.addDivision("workflow-tasks");
    	workflow.setHead(T_w_head1);
    	workflow.addPara(T_w_info1);
    	
    	// Tasks you own
    	Table table = workflow.addTable("workflow-tasks",ownedItems.size() + 2,5);
        table.setHead(T_w_head2);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_w_column1);
        header.addCellContent(T_w_column2);
        header.addCellContent(T_w_column3);
        header.addCellContent(T_w_column4);
        header.addCellContent(T_w_column5);
    	
        if (ownedItems.size() > 0)
        {
        	for (WorkflowItem owned : ownedItems)
        	{
        		int workflowItemID = owned.getID();
        		String url = contextPath+"/handle/"+owned.getCollection().getHandle()+"/workflow?workflowID="+workflowItemID;
        		DCValue[] titles = owned.getItem().getDC("title", null, Item.ANY);
        		String collectionName = owned.getCollection().getMetadata("name");
        		EPerson submitter = owned.getSubmitter();
        		String submitterName = submitter.getFullName();
        		String submitterEmail = submitter.getEmail();
        		
        		Message state = getWorkflowStateMessage(owned);

        		Row row = table.addRow();
        		
        		CheckBox remove = row.addCell().addCheckBox("workflowID");
	        	remove.setLabel("selected");
	        	remove.addOption(workflowItemID);
        		
        		// The task description
	        	row.addCell().addXref(url,state);

        		// The item description
        		if (titles != null && titles.length > 0)
        		{
        			String displayTitle = titles[0].value;
        			if (displayTitle.length() > 50)
        				displayTitle = displayTitle.substring(0,50)+ " ...";
        			row.addCell().addXref(url,displayTitle);
        		}
        		else
        			row.addCell().addXref(url,T_untitled);

        		// Submitted too
        		row.addCell().addXref(url,collectionName);

        		// Submitted by
	        	Cell cell = row.addCell();
	        	cell.addContent(T_email);
	        	cell.addXref("mailto:"+submitterEmail,submitterName);
        	}
        	
        	Row row = table.addRow();
 	    	row.addCell(0,5).addButton("submit_return_tasks").setValue(T_w_submit_return);
        	
        }
        else
        {
        	Row row = table.addRow();
        	row.addCell(0,5).addHighlight("italic").addContent(T_w_info2);
        }
       
        
        
        
        // Tasks in the pool
        table = workflow.addTable("workflow-tasks",pooledItems.size()+2,5);
        table.setHead(T_w_head3);
        
        header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_w_column1);
        header.addCellContent(T_w_column2);
        header.addCellContent(T_w_column3);
        header.addCellContent(T_w_column4);
        header.addCellContent(T_w_column5);
        
        if (pooledItems.size() > 0)
        {

        	for (WorkflowItem pooled : pooledItems)
        	{
        		int workflowItemID = pooled.getID();
        		String url = contextPath+"/handle/"+pooled.getCollection().getHandle()+"/workflow?workflowID="+workflowItemID;
        		DCValue[] titles = pooled.getItem().getDC("title", null, Item.ANY);
        		String collectionName = pooled.getCollection().getMetadata("name");
        		EPerson submitter = pooled.getSubmitter();
        		String submitterName = submitter.getFullName();
        		String submitterEmail = submitter.getEmail();

        		Message state = getWorkflowStateMessage(pooled);
        		
        		
        		Row row = table.addRow();
        		
        		CheckBox remove = row.addCell().addCheckBox("workflowID");
	        	remove.setLabel("selected");
	        	remove.addOption(workflowItemID);
        		
        		// The task description
	        	row.addCell().addXref(url,state);

        		// The item description
        		if (titles != null && titles.length > 0)
        		{
        			String displayTitle = titles[0].value;
        			if (displayTitle.length() > 50)
        				displayTitle = displayTitle.substring(0,50)+ " ...";
        			
        			row.addCell().addXref(url,displayTitle);
        		}
        		else
        			row.addCell().addXref(url,T_untitled);

        		// Submitted too
        		row.addCell().addXref(url,collectionName);

        		// Submitted by
        		Cell cell = row.addCell();
	        	cell.addContent(T_email);
	        	cell.addXref("mailto:"+submitterEmail,submitterName);
        		
        	}
        	Row row = table.addRow();
 	    	row.addCell(0,5).addButton("submit_take_tasks").setValue(T_w_submit_take);
        }
        else
        {
        	Row row = table.addRow();
        	row.addCell(0,5).addHighlight("italic").addContent(T_w_info3);
        }
    }
    
    
    
    /**
     * There are two options, the user has some unfinished submissions 
     * or the user does not.
     * 
     * If the user does not, then we just display a simple paragraph 
     * explaining that the user may submit new items to dspace.
     * 
     * If the user does have unfinisshed submissions then a table is 
     * presented listing all the unfinished submissions that this user has.
     * 
     */
    private void addUnfinishedSubmissions(Division division) throws SQLException, WingException
    {
    	
        // User's WorkflowItems
    	WorkspaceItem[] unfinishedItems = WorkspaceItem.findByEPerson(context,context.getCurrentUser());
    	SupervisedItem[] supervisedItems = SupervisedItem.findbyEPerson(context, context.getCurrentUser());

    	if (unfinishedItems.length <= 0 && supervisedItems.length <= 0)
    	{
    		Collection[] collections = Collection.findAuthorized(context, null, Constants.ADD);
    		
    		if (collections.length > 0)
    		{
    			Division start = division.addDivision("start-submision");
	        	start.setHead(T_s_head1);
	        	Para p = start.addPara();
	        	p.addContent(T_s_info1a);
	        	p.addXref(contextPath+"/submit",T_s_info1b);
	        	p.addContent(T_s_info1c);
	        	return;
    		}
    	}
    	
    	Division unfinished = division.addInteractiveDivision("unfinished-submisions", contextPath+"/submit", Division.METHOD_POST);
    	unfinished.setHead(T_s_head2);
    	Para p = unfinished.addPara();
    	p.addContent(T_s_info2a);
    	p.addHighlight("bold").addXref(contextPath+"/submit",T_s_info2b);
    	p.addContent(T_s_info2c);
    	
    	// Calculate the number of rows.
    	// Each list pluss the top header and bottom row for the button.
    	int rows = unfinishedItems.length + supervisedItems.length + 2;
    	if (supervisedItems.length > 0 && unfinishedItems.length > 0)
    		rows++; // Authoring heading row
    	if (supervisedItems.length > 0)
    		rows++; // Supervising heading row
    	
    	
    	Table table = unfinished.addTable("unfinished-submissions",rows,5);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_s_column1);
        header.addCellContent(T_s_column2);
        header.addCellContent(T_s_column3);
        header.addCellContent(T_s_column4);
        
        if (supervisedItems.length > 0 && unfinishedItems.length > 0)
        {
            header = table.addRow();
            header.addCell(null,Cell.ROLE_HEADER,0,5,null).addContent(T_s_head3);
        }
        
        if (unfinishedItems.length > 0)
        {
	        for (WorkspaceItem workspaceItem : unfinishedItems) 
	        {
	        	DCValue[] titles = workspaceItem.getItem().getDC("title", null, Item.ANY);
	        	EPerson submitterEPerson = workspaceItem.getItem().getSubmitter();
	        	
	        	int workspaceItemID = workspaceItem.getID();
	        	String url = contextPath+"/submit?workspaceID="+workspaceItemID;
	        	String submitterName = submitterEPerson.getFullName();
	        	String submitterEmail = submitterEPerson.getEmail();
	        	String collectionName = workspaceItem.getCollection().getMetadata("name");
	
	        	Row row = table.addRow(Row.ROLE_DATA);
	        	CheckBox remove = row.addCell().addCheckBox("workspaceID");
	        	remove.setLabel("remove");
	        	remove.addOption(workspaceItemID);
	        	
	        	if (titles.length > 0)
	        	{
	        		String displayTitle = titles[0].value;
        			if (displayTitle.length() > 50)
        				displayTitle = displayTitle.substring(0,50)+ " ...";
	        		row.addCell().addXref(url,displayTitle);
	        	}
	        	else
	        		row.addCell().addXref(url,T_untitled);
	        	row.addCell().addXref(url,collectionName);
	        	Cell cell = row.addCell();
	        	cell.addContent(T_email);
	        	cell.addXref("mailto:"+submitterEmail,submitterName);
	        }
        } 
        else
        {
        	header = table.addRow();
        	header.addCell(0,5).addHighlight("italic").addContent(T_s_info3);
        }
        
        if (supervisedItems.length > 0)
        {
            header = table.addRow();
            header.addCell(null,Cell.ROLE_HEADER,0,5,null).addContent(T_s_head4);
        }
        
        for (WorkspaceItem workspaceItem : supervisedItems) 
        {
        	
        	DCValue[] titles = workspaceItem.getItem().getDC("title", null, Item.ANY);
        	EPerson submitterEPerson = workspaceItem.getItem().getSubmitter();
        	
        	int workspaceItemID = workspaceItem.getID();
        	String url = contextPath+"/submit?workspaceID="+workspaceItemID;
        	String submitterName = submitterEPerson.getFullName();
        	String submitterEmail = submitterEPerson.getEmail();
        	String collectionName = workspaceItem.getCollection().getMetadata("name");
        	
        	
        	Row row = table.addRow(Row.ROLE_DATA);
        	CheckBox selected = row.addCell().addCheckBox("workspaceID");
        	selected.setLabel("select");
        	selected.addOption(workspaceItemID);
        	
        	if (titles.length > 0)
        	{
        		String displayTitle = titles[0].value;
    			if (displayTitle.length() > 50)
    				displayTitle = displayTitle.substring(0,50)+ " ...";
        		row.addCell().addXref(url,displayTitle);
        	}
        	else
        		row.addCell().addXref(url,T_untitled);
        	row.addCell().addXref(url,collectionName);
        	Cell cell = row.addCell();
        	cell.addContent(T_email);
        	cell.addXref("mailto:"+submitterEmail,submitterName);
        }
        
        
        header = table.addRow();
        Cell lastCell = header.addCell(0,5);
        if (unfinishedItems.length > 0 || supervisedItems.length > 0)
        	lastCell.addButton("submit_submissions_remove").setValue(T_s_submit_remove);
    }
    
    
    /**
     * This section lists all the submissions that this user has submitted which are currently under review.
     * 
     * If the user has none, this nothing is displayed.
     */
    private void addSubmissionsInWorkflow(Division division) throws SQLException, WingException
    {
    	WorkflowItem[] inprogressItems = WorkflowItem.findByEPerson(context,context.getCurrentUser());

    	// If there is nothing in progress then don't add anything.
    	if (!(inprogressItems.length > 0))
    			return;
    	
    	Division inprogress = division.addDivision("submissions-inprogress");
    	inprogress.setHead(T_p_head1);
    	inprogress.addPara(T_p_info1);
    	
    	
    	Table table = inprogress.addTable("submissions-inprogress",inprogressItems.length+1,3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_p_column1);
        header.addCellContent(T_p_column2);
        header.addCellContent(T_p_column3);
        
        
        for (WorkflowItem workflowItem : inprogressItems)
        {
        	DCValue[] titles = workflowItem.getItem().getDC("title", null, Item.ANY);
        	String collectionName = workflowItem.getCollection().getMetadata("name");
        	Message state = getWorkflowStateMessage(workflowItem);
        	
        	
        	Row row = table.addRow();
        	
        	// Add the title column
        	if (titles.length > 0)
        	{
        		String displayTitle = titles[0].value;
    			if (displayTitle.length() > 50)
    				displayTitle = displayTitle.substring(0,50)+ " ...";
        		row.addCellContent(displayTitle);
        	}
        	else
        		row.addCellContent(T_untitled);
        	
        	// Collection name column
        	row.addCellContent(collectionName);
        	
        	// Status column
        	row.addCellContent(state);
        }
    }
    
    
    
    
    
    /**
     * Determine the correct message that describes this workflow item's state.
     * 
     * FIXME: change to return type of message;
     */
    private Message getWorkflowStateMessage(WorkflowItem workflowItem)
    {
		switch (workflowItem.getState())
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
    
    
    
}
