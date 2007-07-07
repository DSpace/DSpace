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
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.license.CreativeCommons;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;

/**
 * This abstract class represents an abstract step in the 
 * submission or workflow processes. This class provides a place 
 * for common resources to be shared such as i18n tags, progress bars, 
 * and a common setup.
 * 
 * 
 * @author Scott Phillips
 */
abstract public class AbstractStep extends AbstractDSpaceTransformer {


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
	 * The in progress submission, if one is available, this may be either
	 * a workflowItem or a workspaceItem.
	 */
	protected InProgressSubmission submission;
	
	/**
	 * The current step's numeric value that it is at currently. This 
	 * number is dynamic between submissions and based upon the number 
	 * of describe steps.
	 */
	protected int step;
	
	/**
	 * The handle being processed by the current step.
	 */
	protected String handle;
	
	/**
	 * A list of fields that may be in error, not all stages support 
	 * errored fields but if they do then this is where a list of all 
	 * fields in error may be found.
	 */
	protected java.util.List<String> errors;


	/** The parameters that are required by this submissions / workflow step */
	protected boolean requireSubmission = false;
	protected boolean requireWorkflow = false;
	protected boolean requireWorkspace = false;
	protected boolean requireStep = false;
	protected boolean requireHandle = false;
	

	/**
	 * Grab all the step's parameters from the sitemap. This includes 
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
			this.step = parameters.getParameterAsInteger("step",-1);
			this.handle = parameters.getParameter("handle",null);
			
			if (this.id != null)
				this.submission = FlowUtils.findSubmission(context, this.id);
			
			
			// Check required error conditions
			if (this.requireSubmission && this.submission == null)
				throw new ProcessingException("Unable to find submission for id: "+this.id);
			
			if (this.requireWorkflow && !(submission instanceof WorkflowItem))
				throw new ProcessingException("The submission is not a workflow, "+this.id);
			
			if (this.requireWorkspace && !(submission instanceof WorkspaceItem))
				throw new ProcessingException("The submission is not a workspace, "+this.id);
			
			if (this.requireStep && step < 0)
				throw new ProcessingException("Step is a required parameter.");
			
			if (this.requireHandle && handle == null)
				throw new ProcessingException("Handle is a required parameter.");
			
		} 
		catch (SQLException sqle) 
		{
			throw new ProcessingException("Unable to find submission.",sqle);
		}

		String errors = parameters.getParameter("errors","");
		if (errors.length() > 0)
			this.errors = Arrays.asList(errors.split(","));
		else
			this.errors = new ArrayList<String>();
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
			pageMeta.addMetadata("title").addContent("Workflow Task");
			
			Collection collection = submission.getCollection();
			
	        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
	        HandleUtil.buildHandleTrail(collection,pageMeta,contextPath);
	        pageMeta.addTrail().addContent("Workflow Task");
		}
	}


	/**
	 * Add a submission progress list to the current div for this step. 
	 * 
	 * @param div The division to add the list to.
	 */
	public void addSubmissionProgressList(Division div) throws WingException
	{
		int numberOfDescribePages = FlowUtils.getNumberOfDescribePages(context, this.id);
		
		// Progress list for normal submissions
		if (this.submission instanceof WorkspaceItem)
		{	
			List progress = div.addList("submit-progress",List.TYPE_PROGRESS);
			
			progress.addItem("submit-initial-questions", render(step, 0)).addButton("submit_jump_0").setValue(T_initial_questions);

			for (int i=0; i<numberOfDescribePages; i++)
			{
				progress.addItem("submit-describe", render(step, i+1)).addButton("submit_jump_"+(i+1)).setValue(T_describe);
			}

			progress.addItem("submit-upload", render(step, numberOfDescribePages+1)).addButton("submit_jump_"+(numberOfDescribePages+1)).setValue(T_upload);

			progress.addItem("submit-review", render(step, numberOfDescribePages+2)).addButton("submit_jump_"+(numberOfDescribePages+2)).setValue(T_review);

			if (CreativeCommons.isEnabled())
			{
				progress.addItem("submit-creative-commons", render(step, numberOfDescribePages+3)).addButton("submit_jump_"+(numberOfDescribePages+3)).setValue(T_creative_commons);
			}

			progress.addItem("submit-license", render(step, numberOfDescribePages+4)).addButton("submit_jump_"+(numberOfDescribePages+4)).setValue(T_license);
			
		}
		// Progress list for the edit metadata step of workflows.
		else if (this.submission instanceof WorkflowItem)
		{
			List progress = div.addList("submit-progress",List.TYPE_PROGRESS);

			progress.addItem("submit-initial-questions", render(step, 0)).addButton("submit_jump_0").setValue(T_initial_questions);

			for (int i=0; i<numberOfDescribePages; i++)
			{
				progress.addItem("submit-describe", render(step, i+1)).addButton("submit_jump_"+(i+1)).setValue(T_describe);
			}
			
			progress.addItem("submit-upload", render(step, numberOfDescribePages+1)).addButton("submit_jump_"+(numberOfDescribePages+1)).setValue(T_upload);

			progress.addItem("submit-review", render(step, numberOfDescribePages+2)).addButton("submit_jump_"+(numberOfDescribePages+2)).setValue(T_review);
		}
		
	}

	/**
	 * A simple method to determine if the this is the current step or not.
	 * 
	 * @param givenStep This item's step
	 * @param step The current step
	 * @return
	 */
	private static String render(int givenStage, int stage)
	{
		if (givenStage == stage)
			return "current";
		else
			return null;
	}

	/**
	 * Recycle
	 */
	public void recycle() 
	{
		this.id = null;
		this.submission = null;
		this.errors = null;
		super.recycle();
	}
}
