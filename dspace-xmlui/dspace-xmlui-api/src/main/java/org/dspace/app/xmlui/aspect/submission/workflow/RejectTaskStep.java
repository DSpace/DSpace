/*
 * RejectTaskStep.java
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
package org.dspace.app.xmlui.aspect.submission.workflow;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * This step is used when the user has selected to
 * reject the item. Here they are asked to enter
 * a reason why the item should be rejected.
 * 
 * @author Scott Phillips
 */
public class RejectTaskStep extends AbstractStep
{
  
	/** Language Strings **/
    protected static final Message T_info1 = 
        message("xmlui.Submission.workflow.RejectTaskStep.info1");
    protected static final Message T_reason = 
        message("xmlui.Submission.workflow.RejectTaskStep.reason");
    protected static final Message T_reason_required = 
        message("xmlui.Submission.workflow.RejectTaskStep.reason_required");
    protected static final Message T_submit_reject = 
        message("xmlui.Submission.workflow.RejectTaskStep.submit_reject");
    protected static final Message T_submit_cancel = 
        message("xmlui.general.cancel");
	
	
	
	
	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public RejectTaskStep()
	{
		this.requireWorkflow = true;
	}
	
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow";
    	
    	Request request = ObjectModelHelper.getRequest(objectModel);
		String showfull = request.getParameter("showfull");
		
		// if the user selected showsimple, remove showfull.
		if (showfull != null && request.getParameter("showsimple") != null)
			showfull = null;
		
		

    	Division div = body.addInteractiveDivision("reject-task", actionURL, Division.METHOD_POST, "primary workflow");
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
        

        List form = div.addList("reject-workflow",List.TYPE_FORM);
        
        form.addItem(T_info1);
        
        TextArea reason = form.addItem().addTextArea("reason");
        reason.setLabel(T_reason);
        reason.setRequired();
        reason.setSize(15, 50);
        if (this.errorFields.contains("reason"))
        	reason.addError(T_reason_required);
    	
        org.dspace.app.xmlui.wing.element.Item actions = form.addItem();
        actions.addButton("submit_reject").setValue(T_submit_reject);
        actions.addButton("submit_cancel").setValue(T_submit_cancel);
        
        div.addHidden("submission-continue").setValue(knot.getId()); 
    }
    
    
   
}
