/*
 * LicenseStep.java
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

import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.xml.sax.SAXException;

/**
 * This is the last step of the item submission processes. During this
 * step the user must agree to the collection's standard distribution
 * license. If the user can not agree to the license they they may either
 * save the submission untill a later time or remove the submission completely.
 * 
 * This step will include the full license text inside the page using the
 * HTML fragment method.
 * 
 * 
 * @author Scott Phillips
 */
public class LicenseStep extends AbstractStep
{

	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.LicenseStep.head");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.LicenseStep.info1");
    protected static final Message T_info2 = 
        message("xmlui.Submission.submit.LicenseStep.info2");
    protected static final Message T_info3 = 
        message("xmlui.Submission.submit.LicenseStep.info3");
    protected static final Message T_decision_label = 
        message("xmlui.Submission.submit.LicenseStep.decision_label");
    protected static final Message T_decision_checkbox = 
        message("xmlui.Submission.submit.LicenseStep.decision_checkbox");
    protected static final Message T_decision_error = 
        message("xmlui.Submission.submit.LicenseStep.decision_error");
    protected static final Message T_submit_remove = 
        message("xmlui.Submission.submit.LicenseStep.submit_remove");
    protected static final Message T_submit_complete = 
        message("xmlui.Submission.submit.LicenseStep.submit_complete");
	
    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public LicenseStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
    
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{
		// Get the full text for the actuial licese
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit";
		String licenseText = collection.getLicense();
		
		Division div = body.addInteractiveDivision("submit-license",actionURL, Division.METHOD_POST,"primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);
		
		Division inner = div.addDivision("submit-license-inner");
		inner.setHead(T_head);
		inner.addPara(T_info1);
		inner.addPara(T_info2);
		
		
		// Add the actual text of the license:
		Division displayLicense = inner.addDivision("submit-license-standard-text","license-text");
		displayLicense.addSimpleHTMLFragment(true, licenseText);
		
		inner.addPara(T_info3);
		
		List controls = inner.addList("submit-review", List.TYPE_FORM);
		
		CheckBox decision = controls.addItem().addCheckBox("decision");
		decision.setLabel(T_decision_label);
		decision.addOption("accept",T_decision_checkbox);

		// If the field is in error then 
		if (errors.contains("decision"))
		{
			decision.addError(T_decision_error);

			controls.addItem().addButton("submit_remove").setValue(T_submit_remove);
		}
		
		
        org.dspace.app.xmlui.wing.element.Item actions = controls.addItem();
        actions.addButton("submit_previous").setValue(T_previous);
		actions.addButton("submit_save").setValue(T_save);
		actions.addButton("submit_complete").setValue(T_submit_complete);

		div.addHidden("submission-continue").setValue(knot.getId()); 

	}
}
