/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
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

    private static final Logger log = Logger.getLogger(Submissions.class);

    protected ClaimedTaskService claimedTaskService = XmlWorkflowServiceFactory.getInstance().getClaimedTaskService();
    protected PoolTaskService poolTaskService = XmlWorkflowServiceFactory.getInstance().getPoolTaskService();
    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
    protected XmlWorkflowFactory workflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();


	public void addPageMeta(PageMeta pageMeta) throws SAXException,
	WingException, SQLException, IOException,
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
//        this.addUnfinishedSubmissions(div);
        this.addSubmissionsInWorkflow(div);
//        this.addPreviousSubmissions(div);
    }

    private void addWorkflowTasksDiv(Division division) throws SQLException, WingException, AuthorizeException, IOException {
    	division.addDivision("start-submision");
    }

    /**
     * If the user has any workflow tasks, either assigned to them or in an
     * available pool of tasks, then build two tables listing each of these queues.
     *
     * If the user doesn't have any workflows then don't do anything.
     *
     * @param division The division to add the two queues too.
     */
    private void addWorkflowTasks(Division division) throws SQLException, WingException, AuthorizeException, IOException {
    	@SuppressWarnings("unchecked") // This cast is correct
    	java.util.List<ClaimedTask> ownedItems = claimedTaskService.findByEperson(context, context.getCurrentUser());
    	@SuppressWarnings("unchecked") // This cast is correct.
    	java.util.List<PoolTask> pooledItems = poolTaskService.findByEperson(context, context.getCurrentUser());

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

        //Only show our return to pool button if we have a task that CAN be returned to a pool
        boolean showReturnToPoolButton = false;
        if (ownedItems.size() > 0)
        {
        	for (ClaimedTask owned : ownedItems)
        	{
                String stepID = owned.getStepID();
                String actionID = owned.getActionID();
                XmlWorkflowItem item = owned.getWorkflowItem();
                try {
                    Workflow wf = workflowFactory.getWorkflow(item.getCollection());
                    Step step = wf.getStep(stepID);
                    WorkflowActionConfig action = step.getActionConfig(actionID);
                    String url = contextPath+"/handle/"+item.getCollection().getHandle()+"/xmlworkflow?workflowID="+item.getID()+"&stepID="+stepID+"&actionID="+actionID;
                    String title = item.getItem().getName();
                    String collectionName = item.getCollection().getName();
                    EPerson submitter = item.getSubmitter();
                    String submitterName = submitter.getFullName();
                    String submitterEmail = submitter.getEmail();

    //        		Message state = getWorkflowStateMessage(owned);

                    boolean taskHasPool = step.getUserSelectionMethod().getProcessingAction().usesTaskPool();
                    if(taskHasPool){
                        //We have a workflow item that uses a pool, ensure we see the return to pool button
                        showReturnToPoolButton = true;
                    }

                    Row row = table.addRow();

                    Cell firstCell = row.addCell();
                    if(taskHasPool){
                        CheckBox remove = firstCell.addCheckBox("workflowandstepID");
                        remove.setLabel("selected");
                        remove.addOption(item.getID() + ":" + step.getId());
                    }

                    // The task description
                    row.addCell().addXref(url,message("xmlui.XMLWorkflow." + wf.getID() + "." + stepID + "." + actionID));

                    // The item description
                    if (title != null && title.length() > 0)
                    {
                        String displayTitle = title;
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
                } catch (WorkflowConfigurationException e) {
                    Row row = table.addRow();
                    row.addCell().addContent("Error: Configuration error in workflow.");
                    log.error(LogManager.getHeader(context, "Error while adding owned tasks on the submissions page", ""), e);

                } catch (Exception e) {
                    log.error(LogManager.getHeader(context, "Error while adding owned tasks on the submissions page", ""), e);
                }
            }

        	if(showReturnToPoolButton){
                Row row = table.addRow();
                row.addCell(0,5).addButton("submit_return_tasks").setValue(T_w_submit_return);
            }

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

        	for (PoolTask pooled : pooledItems)
        	{
                String stepID = pooled.getStepID();
                String actionID = pooled.getActionID();
                try {
                    XmlWorkflowItem item = pooled.getWorkflowItem();
                    Workflow wf = workflowFactory.getWorkflow(item.getCollection());
                    String url = contextPath+"/handle/"+item.getCollection().getHandle()+"/xmlworkflow?workflowID="+item.getID()+"&stepID="+stepID+"&actionID="+actionID;
                    String title = item.getItem().getName();
                    String collectionName = item.getCollection().getName();
                    EPerson submitter = item.getSubmitter();
                    String submitterName = submitter.getFullName();
                    String submitterEmail = submitter.getEmail();

    //        		Message state = getWorkflowStateMessage(pooled);


                    Row row = table.addRow();

                    CheckBox claimTask = row.addCell().addCheckBox("workflowID");
                    claimTask.setLabel("selected");
                    claimTask.addOption(item.getID());

                    // The task description
//                    row.addCell().addXref(url,message("xmlui.Submission.Submissions.claimAction"));
                    row.addCell().addXref(url,message("xmlui.XMLWorkflow." + wf.getID() + "." + stepID + "." + actionID));

                    // The item description
                    if (title != null && title.length() > 0)
                    {
                        String displayTitle = title;
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
                } catch (WorkflowConfigurationException e) {
                    Row row = table.addRow();
                    row.addCell().addContent("Error: Configuration error in workflow.");
                    log.error(LogManager.getHeader(context, "Error while adding pooled tasks on the submissions page", ""), e);
                } catch (Exception e) {
                    log.error(LogManager.getHeader(context, "Error while adding pooled tasks on the submissions page", ""), e);
                }
            }
        	Row row = table.addRow();
	    	row.addCell(0,5).addButton("submit_take_tasks").setValue(T_w_submit_take);
        }
        else
        {
        	Row row = table.addRow();
        	row.addCell(0,4).addHighlight("italic").addContent(T_w_info3);
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
    private void addSubmissionsInWorkflow(Division division) throws SQLException, WingException, AuthorizeException, IOException {
        java.util.List<XmlWorkflowItem> inprogressItems;
        try {
            inprogressItems = xmlWorkflowItemService.findBySubmitter(context, context.getCurrentUser());
            // If there is nothing in progress then don't add anything.
            if (!(inprogressItems.size() > 0))
                    return;

            Division inprogress = division.addDivision("submissions-inprogress");
            inprogress.setHead(T_p_head1);
            inprogress.addPara(T_p_info1);


            Table table = inprogress.addTable("submissions-inprogress",inprogressItems.size()+1,3);
            Row header = table.addRow(Row.ROLE_HEADER);
            header.addCellContent(T_p_column1);
            header.addCellContent(T_p_column2);
            header.addCellContent(T_p_column3);


            for (XmlWorkflowItem workflowItem : inprogressItems)
            {
                String title = workflowItem.getItem().getName();
                String collectionName = workflowItem.getCollection().getName();
                java.util.List<PoolTask> pooltasks = poolTaskService.find(context,workflowItem);
                java.util.List<ClaimedTask> claimedtasks = claimedTaskService.find(context, workflowItem);

                Message state = message("xmlui.XMLWorkflow.step.unknown");
                for(PoolTask task: pooltasks){
                    Workflow wf = workflowFactory.getWorkflow(workflowItem.getCollection());
                    Step step = wf.getStep(task.getStepID());
                    state = message("xmlui.XMLWorkflow." + wf.getID() + "." + step.getId() + "." + task.getActionID());
                }
                for(ClaimedTask task: claimedtasks){
                    Workflow wf = workflowFactory.getWorkflow(workflowItem.getCollection());
                    Step step = wf.getStep(task.getStepID());
                    state = message("xmlui.XMLWorkflow." + wf.getID() + "." + step.getId() + "." + task.getActionID());
                }
                Row row = table.addRow();

                // Add the title column
                if (StringUtils.isNotBlank(title))
                {
                    String displayTitle = title;
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
        }  catch (Exception e) {
            Row row = division.addTable("table0",1,1).addRow();
            row.addCell().addContent("Error: Configuration error in workflow.");

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
