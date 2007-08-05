/*
 * SaveOrRemoveStep.java
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
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;


import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.xml.sax.SAXException;

/**
 * This is sort-of a step of the submission processes (not
 * an official "step", since it does not extend AbstractSubmissionStep). 
 * <P>
 * At any time during submission the user may leave the processe and either
 * leave it for later or remove the submission.
 * <P>
 * This form presents three options, 1) Go back, 2) save the work, 
 * or 3) remove it.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (small updates for Configurable Submission)
 */
public class SaveOrRemoveStep extends AbstractStep
{

	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.head");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.info1");
    protected static final Message T_submit_back = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.submit_back");
    protected static final Message T_submit_save = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.submit_save");
    protected static final Message T_submit_remove = 
        message("xmlui.Submission.submit.SaveOrRemoveStep.submit_remove");
	
    
    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public SaveOrRemoveStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
    
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{	
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit";
		
		Division div = body.addInteractiveDivision("submit-save-or-cancel",actionURL, Division.METHOD_POST,"primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);
		
		List saveOrCancel = div.addList("submit-review", List.TYPE_FORM);
	
		saveOrCancel.setHead(T_head);
		saveOrCancel.addItem(T_info1);
		
		saveOrCancel.addItem().addButton("submit_back").setValue(T_submit_back);
		
        org.dspace.app.xmlui.wing.element.Item actions = saveOrCancel.addItem();
        actions.addButton("submit_save").setValue(T_submit_save);
		actions.addButton("submit_remove").setValue(T_submit_remove);

		div.addHidden("submission-continue").setValue(knot.getId()); 

	}
}
