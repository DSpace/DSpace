/*
 * AddEPersonForm.java
 *
 * Version: $Revision: 1.0 $
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
package org.dspace.app.xmlui.aspect.administrative.eperson;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;

/**
 * Present the user with all the eperson metadata fields so that they
 * can describe the new eperson before being created. If the user's 
 * input is incorrect in someway then they may be returning here with 
 * some fields in error. In particular there is a special case for the 
 * condition when the email-adress entered is allready in use by 
 * another user.
 * 
 * @author Alexey Maslov
 */
public class AddEPersonForm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
	private static final Message T_eperson_trail =
		message("xmlui.administrative.eperson.general.epeople_trail");
	
	private static final Message T_title =
		message("xmlui.administrative.eperson.AddEPersonForm.title");

	private static final Message T_trail =
		message("xmlui.administrative.eperson.AddEPersonForm.trail");

	private static final Message T_head1 =
		message("xmlui.administrative.eperson.AddEPersonForm.head1");

	private static final Message T_email_taken =
		message("xmlui.administrative.eperson.AddEPersonForm.email_taken");

	private static final Message T_head2 =
		message("xmlui.administrative.eperson.AddEPersonForm.head2");

	private static final Message T_error_email_unique =
		message("xmlui.administrative.eperson.AddEPersonForm.error_email_unique");

	private static final Message T_error_email =
		message("xmlui.administrative.eperson.AddEPersonForm.error_email");

	private static final Message T_error_fname =
		message("xmlui.administrative.eperson.AddEPersonForm.error_fname");

	private static final Message T_error_lname =
		message("xmlui.administrative.eperson.AddEPersonForm.error_lname");

	private static final Message T_req_certs =
		message("xmlui.administrative.eperson.AddEPersonForm.req_certs");

	private static final Message T_can_log_in =
		message("xmlui.administrative.eperson.AddEPersonForm.can_log_in");

	private static final Message T_submit_create =
		message("xmlui.administrative.eperson.AddEPersonForm.submit_create");

	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	
	
	/** Language string used from other aspects: */    
		
	private static final Message T_email_address = 
        message("xmlui.EPerson.EditProfile.email_address");
    
    private static final Message T_first_name = 
        message("xmlui.EPerson.EditProfile.first_name");
    
    private static final Message T_last_name = 
        message("xmlui.EPerson.EditProfile.last_name");
    
    private static final Message T_telephone =
        message("xmlui.EPerson.EditProfile.telephone");
    
    	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/epeople",T_eperson_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException 
	{
		// Get all our parameters
		Request request = ObjectModelHelper.getRequest(objectModel);
		
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
		{
			for (String error : errorString.split(","))
				errors.add(error);
		}
	
		String emailValue = request.getParameter("email_address");
		String firstValue = request.getParameter("first_name");
		String lastValue  = request.getParameter("last_name");
		String phoneValue = request.getParameter("phone");
		boolean canLogInValue    = (request.getParameter("can_log_in") == null)  ? false : true;
		boolean certificateValue = (request.getParameter("certificate") == null) ? false : true;
	    		 
		
		
		// DIVISION: eperson-add
	    Division add = body.addInteractiveDivision("eperson-add",contextPath+"/admin/epeople",Division.METHOD_POST,"primary administrative eperson");
	    
	    add.setHead(T_head1);
	    
	    if (errors.contains("eperson_email_key")) {
	    	Para problem = add.addPara();
	    	problem.addHighlight("bold").addContent(T_email_taken);
	    }
                
        List identity = add.addList("identity",List.TYPE_FORM);
        identity.setHead(T_head2);       
        
        Text email = identity.addItem().addText("email_address");
        email.setRequired();
        email.setLabel(T_email_address);
        email.setValue(emailValue);
        if (errors.contains("eperson_email_key")) {
        	email.addError(T_error_email_unique);
        }
        else if (errors.contains("email_address")) {
        	email.addError(T_error_email);
        }
        
        Text firstName = identity.addItem().addText("first_name");
        firstName.setRequired();
        firstName.setLabel(T_first_name);
        firstName.setValue(firstValue);
        if (errors.contains("first_name")) {
        	firstName.addError(T_error_fname);
        }
        
        Text lastName = identity.addItem().addText("last_name");
        lastName.setRequired();
        lastName.setLabel(T_last_name);
        lastName.setValue(lastValue);
        if (errors.contains("last_name")) {
        	lastName.addError(T_error_lname);
        }
        
        Text phone = identity.addItem().addText("phone");
        phone.setLabel(T_telephone);
        phone.setValue(phoneValue);
        
        CheckBox canLogIn = identity.addItem().addCheckBox("can_log_in");
        canLogIn.setLabel(T_can_log_in);
        canLogIn.addOption(canLogInValue, "yes");
        
        CheckBox certificate = identity.addItem().addCheckBox("certificate");
        certificate.setLabel(T_req_certs);
        certificate.addOption(certificateValue,"yes");
        
        Item buttons = identity.addItem();
        buttons.addButton("submit_save").setValue(T_submit_create);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);
        
        add.addHidden("administrative-continue").setValue(knot.getId());
	}
	
}
