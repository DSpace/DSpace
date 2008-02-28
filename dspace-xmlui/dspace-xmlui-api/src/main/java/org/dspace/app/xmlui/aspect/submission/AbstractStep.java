/*
 * AbstractStep.java
 *
 * Version: $Revision: 1.3 $
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

package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;

/**
 * This abstract class represents an abstract page in the 
 * submission or workflow processes. This class provides a place 
 * for common resources to be shared such as i18n tags, progress bars, 
 * and a common setup.
 * 
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
abstract public class AbstractStep extends AbstractDSpaceTransformer 
{
	private static Logger log = Logger.getLogger(AbstractStep.class);

    /** General Language Strings */
    protected static final Message T_submission_title = 
        message("xmlui.Submission.general.submission.title");
    protected static final Message T_submission_trail = 
        message("xmlui.Submission.general.submission.trail");
    protected static final Message T_submission_head = 
        message("xmlui.Submission.general.submission.head");
    protected static final Message T_previous = 
        message("xmlui.Submission.general.submission.previous");
    protected static final Message T_save = 
        message("xmlui.Submission.general.submission.save");
    protected static final Message T_next = 
        message("xmlui.Submission.general.submission.next");
    protected static final Message T_complete = 
        message("xmlui.Submission.general.submission.complete");
    protected static final Message T_dspace_home = 
        message("xmlui.general.dspace_home");  
    protected static final Message T_workflow_title = 
        message("xmlui.Submission.general.workflow.title");
    protected static final Message T_workflow_trail = 
        message("xmlui.Submission.general.workflow.trail");
    protected static final Message T_workflow_head = 
        message("xmlui.Submission.general.workflow.head");
    protected static final Message T_showfull = 
        message("xmlui.Submission.general.showfull");
    protected static final Message T_showsimple = 
        message("xmlui.Submission.general.showsimple");
    
    protected static final Message T_default_title = 
        message("xmlui.Submission.general.default.title");
    protected static final Message T_default_trail = 
        message("xmlui.Submission.general.default.trail");
    
    
    /** Progress Bar Language Strings */
    protected static final Message T_initial_questions = 
        message("xmlui.Submission.general.progress.initial_questions");
    protected static final Message T_describe = 
        message("xmlui.Submission.general.progress.describe");
    protected static final Message T_upload = 
        message("xmlui.Submission.general.progress.upload");
    protected static final Message T_review = 
        message("xmlui.Submission.general.progress.review");
    protected static final Message T_creative_commons = 
        message("xmlui.Submission.general.progress.creative_commons");
    protected static final Message T_license = 
        message("xmlui.Submission.general.progress.license");
    
	
    /** 
     * The id of the currently active workspace or workflow, this contains 
     * the incomplete DSpace item 
     */
	protected String id;
	
    /**
     * The current DSpace SubmissionInfo
     */
    protected SubmissionInfo submissionInfo;
    
	/**
	 * The in progress submission, if one is available, this may be either
	 * a workflowItem or a workspaceItem.
	 */
	protected InProgressSubmission submission;
	
	/**
	 * The current step and page's numeric value that it is at currently. This 
	 * number is dynamic between submissions and is a double where the integer
	 * value is the step #, and the decimal value is the page # 
	 * (e.g. 1.2 is page 2 of step 1)
	 */
	protected double stepAndPage;
	
	/**
	 * The handle being processed by the current step.
	 */
	protected String handle;
	
	/**
	 * The error flag which was returned by the processing of this step
	 */
	protected int errorFlag;
	
	/**
	 * A list of fields that may be in error, not all stages support 
	 * errored fields but if they do then this is where a list of all 
	 * fields in error may be found.
	 */
	protected java.util.List<String> errorFields;


	/** The parameters that are required by this submissions / workflow step */
	protected boolean requireSubmission = false;
	protected boolean requireWorkflow = false;
	protected boolean requireWorkspace = false;
	protected boolean requireStep = false;
	protected boolean requireHandle = false;
	

	/**
	 * Grab all the page's parameters from the sitemap. This includes 
	 * workspaceID, step, and a list of errored fields.
	 * 
	 * If the implementer set any required parameters then insure that 
	 * they are all present.
	 */
	public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) 
	throws ProcessingException, SAXException, IOException
	{ 
		super.setup(resolver,objectModel,src,parameters);

		try {
			this.id = parameters.getParameter("id",null);
			this.stepAndPage = Double.valueOf(parameters.getParameter("step","-1"));
			this.handle = parameters.getParameter("handle",null);
			this.errorFlag = Integer.valueOf(parameters.getParameter("error", String.valueOf(AbstractProcessingStep.STATUS_COMPLETE)));
			this.errorFields = getErrorFields(parameters);
			
			//load in-progress submission
			if (this.id != null)
            {    
                this.submissionInfo = FlowUtils.obtainSubmissionInfo(objectModel, this.id);
				this.submission = submissionInfo.getSubmissionItem();
            }
			
			// Check required error conditions
			if (this.requireSubmission && this.submission == null)
				throw new ProcessingException("Unable to find submission for id: "+this.id);
			
			if (this.requireWorkflow && !(submission instanceof WorkflowItem))
				throw new ProcessingException("The submission is not a workflow, "+this.id);
			
			if (this.requireWorkspace && !(submission instanceof WorkspaceItem))
				throw new ProcessingException("The submission is not a workspace, "+this.id);
			
			if (this.requireStep && stepAndPage < 0)
				throw new ProcessingException("Step is a required parameter.");
			
			if (this.requireHandle && handle == null)
				throw new ProcessingException("Handle is a required parameter.");
			
		} 
		catch (SQLException sqle) 
		{
			throw new ProcessingException("Unable to find submission.",sqle);
		}
	}


	/** 
	 * Base pageMeta that is added to ALL submission stages 
	 */
	public void addPageMeta(PageMeta pageMeta) throws SAXException,
	WingException, UIException, SQLException, IOException,
	AuthorizeException
	{
		if (submission instanceof WorkspaceItem)
		{
			pageMeta.addMetadata("title").addContent(T_submission_title);
	
			Collection collection = submission.getCollection();
			
	        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
	        HandleUtil.buildHandleTrail(collection,pageMeta,contextPath);
	        pageMeta.addTrail().addContent(T_submission_trail);
		}
		else if (submission instanceof WorkflowItem)
		{
			pageMeta.addMetadata("title").addContent(T_workflow_title);
			
			Collection collection = submission.getCollection();
			
	        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
	        HandleUtil.buildHandleTrail(collection,pageMeta,contextPath);
	        pageMeta.addTrail().addContent(T_workflow_trail);
		}
		else
		{
			// defaults for pages that don't have a workspace item or workflow 
			// item such as the submission complete page where the object is in transition.
			pageMeta.addMetadata("title").addContent(T_default_title);
			
			pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
	        pageMeta.addTrail().addContent(T_default_trail);
		}
	}


	/**
	 * Add a submission progress list to the current div for this step. 
	 * 
	 * @param div The division to add the list to.
	 */
	public void addSubmissionProgressList(Division div) throws WingException
	{
		//each entry in progress bar is placed under this "submit-progress" div
		List progress = div.addList("submit-progress",List.TYPE_PROGRESS);
		
		//get Map of progress bar information
		//key: entry # (i.e. step & page), 
		//value: entry name key (i.e. display name)
		Map progBarInfo = this.submissionInfo.getProgressBarInfo();
		
		Iterator i = progBarInfo.keySet().iterator();
		
		//add each entry to progress bar
		while(i.hasNext())
		{
			String entryNum = (String) i.next();
			
			//Since we are using XML-UI, we need to prepend the heading key with "xmlui.Submission."
			String entryNameKey = "xmlui.Submission." + (String) progBarInfo.get(entryNum);
			
			//the value of entryNum is current step & page 
			//(e.g. 1.2 is page 2 of step 1) 
			double currentStepAndPage = Double.valueOf(entryNum).doubleValue();
			
            //add a button to progress bar for this step & page
            addJumpButton(progress, message(entryNameKey), currentStepAndPage);
		}
		
	}
    
    /**
     * Adds a single "jump-to" button, which when clicked will
     * jump the user directly to a particular step within the
     * submission process.
     * <P>
     * This method is used by the addSubmissionProgressList() method
     * to create the progress bar at the top of the submission process.
     * <P>
     * This method is also used by the ReviewStep to add buttons to
     * jump back to a particular step in the submission process
     *
     * @param list
     *          The List which the button will be added to
     * @param buttonText
     *          The text to be displayed on the button
     * @param stepAndPage
     *          The step and page (a double of the form 'step.page', e.g. 1.2)
     *          which this button will jump back to
     */
    public void addJumpButton(List list, Message buttonText, double stepAndPage)
        throws WingException
    {
        //Only add the button if we have button text and a valid step & page!
        if(buttonText!=null && stepAndPage>0.0)
        {    
            //add a Jump To button for this section
            Button jumpButton = list.addItem("step_" + stepAndPage, renderJumpButton(stepAndPage))
                                    .addButton(AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage);
            jumpButton.setValue(buttonText);
        }
    }
    
    /**
     * Adds the "<-Previous", "Save/Cancel" and "Next->" buttons 
     * to a given form.  This method ensures that the same
     * default control/paging buttons appear on each submission page.
     * <P>
     * Note: A given step may define its own buttons as necessary,
     * and not call this method (since it must be explicitly envoked by
     * the step's addBody() method)
     *
     * @param controls
     *          The List which will contain all control buttons
     */
    public void addControlButtons(List controls)
        throws WingException
    {
        Item actions = controls.addItem();
        
        //only have "<-Previous" button if not first step
        if(!isFirstStep())
            actions.addButton(AbstractProcessingStep.PREVIOUS_BUTTON).setValue(T_previous);
        
        //always show "Save/Cancel"
        actions.addButton(AbstractProcessingStep.CANCEL_BUTTON).setValue(T_save);
        
        //If last step, show "Complete Submission"
        if(isLastStep())
            actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(T_complete);
        else //otherwise, show "Next->"
            actions.addButton(AbstractProcessingStep.NEXT_BUTTON).setValue(T_next);
    }
    
	
	/**
	 * Get current step number
	 *
	 * @return step number
	 */
	public int getStep()
	{
		return FlowUtils.getStep(this.stepAndPage);
	}
	
	
	/**
	 * Get number of the current page within the current step
	 *
	 * @return page number (within current step)
	 */
	public int getPage()
	{
		return FlowUtils.getPage(this.stepAndPage);
	}
    
    /**
     * Return whether this is the first step in
     * the submission process (the first step is
     * currently considered the first that appears
     * in the progress bar)
     *
     * @return true if first step
     */
    public boolean isFirstStep()
    {
        Set submissionPagesSet = submissionInfo.getProgressBarInfo().keySet();
        String[] submissionPages = (String[]) submissionPagesSet.toArray(new String[submissionPagesSet.size()]);
        
        double firstStepAndPage = Double.valueOf(submissionPages[0]).doubleValue();
        
        if((FlowUtils.getStep(firstStepAndPage)==getStep())
                && (FlowUtils.getPage(firstStepAndPage)==getPage()))
        {
            return true;
        }
        else
            return false;
    }
    
    
    /**
     * Return whether this is the last step in
     * the submission process (the last step is
     * currently considered the one directly *before*
     * the Complete step in the progress bar)
     *
     * @return true if last step
     */
    public boolean isLastStep()
    {
        boolean inWorkflow = this.submissionInfo.isInWorkflow();
        
        Set submissionPagesSet = submissionInfo.getProgressBarInfo().keySet();
        String[] submissionPages = (String[]) submissionPagesSet.toArray(new String[submissionPagesSet.size()]);
        
        double lastStepAndPage;
        
        if(!inWorkflow)
        {
            // If not in Workflow,
            // Last step is considered the one *before* the Complete Step
            lastStepAndPage = Double.valueOf(submissionPages[submissionPages.length-2]).doubleValue();
        }
        else
        {   
            lastStepAndPage = Double.valueOf(submissionPages[submissionPages.length-1]).doubleValue();
        }
        
        if((FlowUtils.getStep(lastStepAndPage)==getStep())
                && (FlowUtils.getPage(lastStepAndPage)==getPage()))
        {
            return true;
        }
        else
            return false;
    }
    
    /**
     * Find the maximum step and page that the user has 
     * reached in the submission processes. 
     * If this submission is a workflow then return max-int.
     * 
     * @param context The current DSpace content
     * @param id The unique ID of the current workflow/workspace
     */
    public double getMaxStepAndPageReached() throws SQLException {
        
        if (this.submission instanceof WorkspaceItem)
        {
            WorkspaceItem workspaceItem = (WorkspaceItem) submission;
            
            int step = workspaceItem.getStageReached();
            if(step<0) 
                step=0;
            
            int page = workspaceItem.getPageReached();
            if (page < 0)
                page = 0;
            
            return Double.valueOf(step + "." + page).doubleValue();
        }
        
        // This is a workflow, return infinity.
        return Double.MAX_VALUE;
    }
	
	/**
	 * Retreive error fields from the list of parameters
	 * and return a List of all fields which had errors
	 *
	 * @return java.util.List of field names with errors
	 */
	public java.util.List<String> getErrorFields(Parameters parameters)
	{
		java.util.List<String> fields = new ArrayList<String>();
		
		String errors = parameters.getParameter("error_fields","");
		
		if (errors!=null && errors.length() > 0)
		{	
			if(errors.indexOf(",") > 0)
				fields = Arrays.asList(errors.split(","));
			else//only one error field
				fields.add(errors);
		}
		
		return fields;
	}
	
	/**
	 * A simple method to determine how the Jump to button
     * for a given step and page should be rendered.
     * <P>
     * If the given step and page corresponds to the current
     * step and page, render it with "current" style.
     * <P>
     * If the given step and page is greater than the max,
     * render it with "disabled" style.
	 * 
	 * @param givenStepAndPage 
     *        This given step & page (e.g. "1.2")
	 * @return
     *        render style for this button
	 */
	private String renderJumpButton(double givenStepAndPage)
	{
        try
        {
            if (Double.compare(givenStepAndPage, this.stepAndPage)==0)
    			return "current";
            else if (Double.compare(givenStepAndPage, getMaxStepAndPageReached())>0)
                return "disabled";
            else
    			return null;
        }
        catch(Exception e)
        {
            return null;
        }  
	}
	
	
	/**
	 * Recycle
	 */
	public void recycle() 
	{
		this.id = null;
		this.submission = null;
		this.stepAndPage = -1;
		this.handle = null;
		this.errorFlag = 0;
		this.errorFields = null;
		super.recycle();
	}
}
