/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.workflow;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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

    protected BasicWorkflowService basicWorkflowService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowService();
    protected BasicWorkflowItemService basicWorkflowItemService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowItemService();


    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
	WingException, UIException, SQLException, IOException,
	AuthorizeException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/submissions",T_trail);
	}

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        Division div = body.addInteractiveDivision("submissions", contextPath+"/submissions", Division.METHOD_POST,"primary");
        div.setHead(T_head);

        this.addWorkflowTasks(div);
//        this.addUnfinishedSubmissions(div);
        this.addSubmissionsInWorkflow(div);
//        this.addPreviousSubmissions(div);
    }


    /**
     * If the user has any workflow tasks, either assigned to them or in an
     * available pool of tasks, then build two tables listing each of these queues.
     *
     * If the user doesn't have any workflows then don't do anything.
     *
     * @param division The division to add the two queues too.
     */
    private void addWorkflowTasks(Division division) throws SQLException, WingException
    {
    	@SuppressWarnings("unchecked") // This cast is correct
    	List<BasicWorkflowItem> ownedItems = basicWorkflowService.getOwnedTasks(context, context
                .getCurrentUser());
    	@SuppressWarnings("unchecked") // This cast is correct.
    	List<BasicWorkflowItem> pooledItems = basicWorkflowService.getPooledTasks(context, context
                .getCurrentUser());

    	if (!(ownedItems.size() > 0 || pooledItems.size() > 0))
    		// No tasks, so don't show the table.
        {
            return;
        }


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
        	for (BasicWorkflowItem owned : ownedItems)
        	{
        		int workflowItemID = owned.getID();
        		String collectionUrl = contextPath + "/handle/" + owned.getCollection().getHandle();
        		String ownedWorkflowItemUrl = contextPath + "/handle/" + owned.getCollection().getHandle() + "/workflow?workflowID=" + workflowItemID;
        		String title = owned.getItem().getName();
        		String collectionName = owned.getCollection().getName();
        		EPerson submitter = owned.getSubmitter();
        		String submitterName = submitter.getFullName();
        		String submitterEmail = submitter.getEmail();

        		Message state = getWorkflowStateMessage(owned);

        		Row row = table.addRow();

        		CheckBox remove = row.addCell().addCheckBox("workflowID");
	        	remove.setLabel("selected");
	        	remove.addOption(workflowItemID);

        		// The task description
	        	row.addCell().addXref(ownedWorkflowItemUrl, state);

        		// The item description
        		if (StringUtils.isNotBlank(title))
        		{
        			String displayTitle = title;
        			if (displayTitle.length() > 50)
                    {
                        displayTitle = displayTitle.substring(0, 50) + " ...";
                    }
        			row.addCell().addXref(ownedWorkflowItemUrl, displayTitle);
        		}
        		else
                {
                    row.addCell().addXref(ownedWorkflowItemUrl, T_untitled);
                }

        		// Submitted too
        		row.addCell().addXref(collectionUrl, collectionName);

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

        	for (BasicWorkflowItem pooled : pooledItems)
        	{
        		int workflowItemID = pooled.getID();
        		String collectionUrl = contextPath + "/handle/" + pooled.getCollection().getHandle();
        		String pooledItemUrl = contextPath + "/handle/" + pooled.getCollection().getHandle() + "/workflow?workflowID=" + workflowItemID;
        		String title = pooled.getItem().getName();
        		String collectionName = pooled.getCollection().getName();
        		EPerson submitter = pooled.getSubmitter();
        		String submitterName = submitter.getFullName();
        		String submitterEmail = submitter.getEmail();

        		Message state = getWorkflowStateMessage(pooled);


        		Row row = table.addRow();

        		CheckBox remove = row.addCell().addCheckBox("workflowID");
	        	remove.setLabel("selected");
	        	remove.addOption(workflowItemID);

        		// The task description
	        	row.addCell().addXref(pooledItemUrl, state);

        		// The item description
        		if (StringUtils.isNotBlank(title))
        		{
        			String displayTitle = title;
        			if (displayTitle.length() > 50)
                    {
                        displayTitle = displayTitle.substring(0, 50) + " ...";
                    }

        			row.addCell().addXref(pooledItemUrl, displayTitle);
        		}
        		else
                {
                    row.addCell().addXref(pooledItemUrl, T_untitled);
                }

        		// Submitted too
        		row.addCell().addXref(collectionUrl, collectionName);

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
        division.addInteractiveDivision("unfinished-submisions", contextPath+"/submit", Division.METHOD_POST);

    }

    /**
     * This section lists all the submissions that this user has submitted which are currently under review.
     *
     * If the user has none, this nothing is displayed.
     */
    private void addSubmissionsInWorkflow(Division division) throws SQLException, WingException
    {
    	List<BasicWorkflowItem> inprogressItems = basicWorkflowItemService.findBySubmitter(context, context.getCurrentUser());

    	// If there is nothing in progress then don't add anything.
    	if (!(inprogressItems.size() > 0))
        {
            return;
        }

    	Division inprogress = division.addDivision("submissions-inprogress");
    	inprogress.setHead(T_p_head1);
    	inprogress.addPara(T_p_info1);


    	Table table = inprogress.addTable("submissions-inprogress",inprogressItems.size()+1,3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_p_column1);
        header.addCellContent(T_p_column2);
        header.addCellContent(T_p_column3);


        for (BasicWorkflowItem workflowItem : inprogressItems)
        {
            String title = workflowItem.getItem().getName();
            String collectionName = workflowItem.getCollection().getName();
            Message state = getWorkflowStateMessage(workflowItem);


            Row row = table.addRow();

            // Add the title column
            if (StringUtils.isNotBlank(title))
            {
                String displayTitle = title;
    	        if (displayTitle.length() > 50)
                {
                    displayTitle = displayTitle.substring(0, 50) + " ...";
                }
                row.addCellContent(displayTitle);
            }
            else
            {
                row.addCellContent(T_untitled);
            }

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
    private Message getWorkflowStateMessage(BasicWorkflowItem workflowItem)
    {
		switch (workflowItem.getState())
		{
			case BasicWorkflowService.WFSTATE_SUBMIT:
				return T_status_0;
			case BasicWorkflowService.WFSTATE_STEP1POOL:
				return T_status_1;
    		case BasicWorkflowService.WFSTATE_STEP1:
    			return T_status_2;
    		case BasicWorkflowService.WFSTATE_STEP2POOL:
    			return T_status_3;
    		case BasicWorkflowService.WFSTATE_STEP2:
    			return T_status_4;
    		case BasicWorkflowService.WFSTATE_STEP3POOL:
    			return T_status_5;
    		case BasicWorkflowService.WFSTATE_STEP3:
    			return T_status_6;
    		case BasicWorkflowService.WFSTATE_ARCHIVE:
    			return T_status_7;
   			default:
   				return T_status_unknown;
		}
    }

    /**
     * Show the user's completed submissions.
     *
     * If the user has no completed submissions, display nothing.
     * If 'displayAll' is true, then display all user's archived submissions.
     * Otherwise, default to only displaying 50 archived submissions.
     *
     * @param division div to put archived submissions in
     */
    private void addPreviousSubmissions(Division division)
            throws SQLException,WingException
    {
        division.addDivision("completed-submissions");

    }
}
