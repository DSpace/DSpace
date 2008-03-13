/*
 * FlowEPersonUtils.java
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

package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.AccountManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPersonDeletionException;

/**
 * Utility methods to processes actions on EPeople. These methods are used
 * exclusivly from the administrative flow scripts.
 * 
 * @author scott phillips
 */
public class FlowEPersonUtils {

	/** Language Strings */
	private static final Message T_add_eperson_success_notice =
		new Message("default","xmlui.administrative.FlowEPersonUtils.add_eperson_success_notice");
	
	private static final Message T_edit_eperson_success_notice =
		new Message("default","xmlui.administrative.FlowEPersonUtils.edit_eperson_success_notice");
	
	private static final Message T_reset_password_success_notice =
		new Message("default","xmlui.administrative.FlowEPersonUtils.reset_password_success_notice");
	
	private static final Message t_delete_eperson_success_notice =
		new Message("default","xmlui.administrative.FlowEPersonUtils.delete_eperson_success_notice");
	
	private static final Message t_delete_eperson_failed_notice =
		new Message("default","xmlui.administrative.FlowEPersonUtils.delete_eperson_failed_notice");
	
	/**
	 * Add a new eperson. This method will check that the email address, 
	 * first name, and last name are non empty. Also a check is preformed 
	 * to see if the requested email address is allready in use by another
	 * user.
	 * 
	 * @param context The current DSpace context
	 * @param request The HTTP request parameters
	 * @param objectModel Cocoon's object model
	 * @return A process result's object.
	 */
	public static FlowResult processAddEPerson(Context context, Request request, Map objectModel) throws SQLException, AuthorizeException 
	{
		FlowResult result = new FlowResult();
		result.setContinue(false); // default to no continue
		
		// Get all our request parameters
		String email = request.getParameter("email_address");
		String first = request.getParameter("first_name");
		String last  = request.getParameter("last_name");
		String phone = request.getParameter("phone");
		boolean login = (request.getParameter("can_log_in") != null) ? true : false;
		boolean certificate = (request.getParameter("certificate") != null) ? true : false;
		
		// If we have errors, the form needs to be resubmitted to fix those problems
	    if (email.length() == 0) 
			result.addError("email_address"); 
		if (first.length() == 0) 
			result.addError("first_name");
		if (last.length() == 0) 
			result.addError("last_name");
	    
	    
		// Check if the email address is all ready being used.	        		
    	EPerson potentialDupicate = EPerson.findByEmail(context,email);
    	if (potentialDupicate != null)
    	{
    		// special error that the front end knows about.
    		result.addError("eperson_email_key");
    	}
		
	    // No errors, so we try to create the EPerson from the data provided
	    if (result.getErrors() == null)
	    {
    		EPerson newPerson = AuthenticationUtil.createNewEperson(objectModel,email);
    		
    		newPerson.setFirstName(first);
            newPerson.setLastName(last);
            newPerson.setMetadata("phone", phone);
            newPerson.setCanLogIn(login);
            newPerson.setRequireCertificate(certificate);
            newPerson.setSelfRegistered(false);
            
            newPerson.update();
            context.commit();
            // success
            result.setContinue(true);
            result.setOutcome(true);
            result.setMessage(T_add_eperson_success_notice);
            result.setParameter("epersonID", newPerson.getID());
	    }
    	
    	return result;
	}


	/**
	 * Edit an eperson's metadata, the email address, first name, and last name are all
	 * required. The user's email address can be updated but it must remain unique, if
	 * the email address allready exists then the an error is produced.
	 * 
	 * @param context The current DSpace context
	 * @param request The HTTP request parameters
	 * @param objectModel Cocoon's object model
	 * @param epersonID The unique id of the eperson being edited.
	 * @return A process result's object.
	 */
	public static FlowResult processEditEPerson(Context context, Request request, Map ObjectModel, int epersonID) throws SQLException, AuthorizeException 
	{

		FlowResult result = new FlowResult();
		result.setContinue(false); // default to failure
		
		// Get all our request parameters
		String email = request.getParameter("email_address");
		String first = request.getParameter("first_name");
		String last  = request.getParameter("last_name");
		String phone = request.getParameter("phone");
		boolean login = (request.getParameter("can_log_in") != null) ? true : false;
		boolean certificate = (request.getParameter("certificate") != null) ? true : false;
		
		
		// If we have errors, the form needs to be resubmitted to fix those problems
	    if (email.length() == 0) 
			result.addError("email_address"); 
		if (first.length() == 0) 
			result.addError("first_name");
		if (last.length() == 0) 
			result.addError("last_name");
		
		
	    // No errors, so we edit the EPerson with the data provided
		if (result.getErrors() == null)
	    {
    		// Grab the person in question 
    		EPerson personModified = EPerson.find(context, epersonID);
        	
    		// Make sure the email address we are changing to is unique
        	if (personModified.getEmail() != email)
        	{	
        		EPerson potentialDupicate = EPerson.findByEmail(context,email);
        		
        		if (potentialDupicate == null) 
        		{
        			personModified.setEmail(email);
        		} 
        		else if (potentialDupicate != personModified) 
        		{	       
        			// set a special field in error so that the transformer can display a pretty error.
        			result.addError("eperson_email_key");
        			return result;
        		}
        	}
        	if (personModified.getFirstName() != first) {
        		personModified.setFirstName(first);
        	}
        	if (personModified.getLastName() != last) {
        		personModified.setLastName(last);
        	}
        	if (personModified.getMetadata("phone") != phone) {
        		personModified.setMetadata("phone", phone);
        	}
        	personModified.setCanLogIn(login);
        	personModified.setRequireCertificate(certificate);
        	
        	
        	personModified.update();
        	context.commit();
        	
        	result.setContinue(true);
        	result.setOutcome(true);
        	// FIXME: rename this message
        	result.setMessage(T_edit_eperson_success_notice);
	    }
		
	    // Everything was fine
	    return result;
	}
	
	/**
	 * Send the user a forgot password email message. The message will 
	 * contain a token that the user can use to login and pick a new password.
	 * 
	 * @param context The current DSpace context
	 * @param epersonID The unique id of the eperson being edited.
	 * @return A process result's object.
	 */
	public static FlowResult processResetPassword(Context context, int epersonID) throws IOException, MessagingException, SQLException, AuthorizeException 
	{	
		EPerson eperson = EPerson.find(context, epersonID);
		
		// Note, this may throw an error is the email is bad.
		AccountManager.sendForgotPasswordInfo(context,eperson.getEmail());
	
		FlowResult result = new FlowResult();
		result.setContinue(true);
    	result.setOutcome(true);
    	result.setMessage(T_reset_password_success_notice);
    	return result;
	}
	
	
	/**
	 * Delete the epeople specified by the epeopleIDs parameter. This assumes that the
	 * detetion has been confirmed.
	 * 
	 * @param context The current DSpace context
	 * @param epeopleIDs The unique id of the eperson being edited.
	 * @return A process result's object.
	 */
	public static FlowResult processDeleteEPeople(Context context, String[] epeopleIDs) throws NumberFormatException, SQLException, AuthorizeException, EPersonDeletionException
	{
		FlowResult result = new FlowResult();
		
		List<String> unableList = new ArrayList<String>();
    	for (String id : epeopleIDs) 
    	{
    		EPerson personDeleted = EPerson.find(context, Integer.valueOf(id));
    		try {
				personDeleted.delete();
    		} 
    		catch (EPersonDeletionException epde)
    		{
    			String firstName = personDeleted.getFirstName();
    			String lastName = personDeleted.getLastName();
    			String email = personDeleted.getEmail();
    			unableList.add(firstName + " " + lastName + " ("+email+")");
    		}
	    }
    	
    	if (unableList.size() > 0)
    	{
    		result.setOutcome(false);
    		result.setMessage(t_delete_eperson_failed_notice);
    			
    		String characters = null;
    		for(String unable : unableList )
    		{
    			if (characters == null)
    				characters = unable;
    			else
    				characters += ", "+unable;
    		}
    		
    		result.setCharacters(characters);
    	}
    	else
    	{
    		result.setOutcome(true);
    		result.setMessage(t_delete_eperson_success_notice);
    	}
 
    	
    	return result;
	}
}
