/*
 * FlowResult.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.xmlui.wing.Message;

/**
 * This class represents the results that may be generated during a flow script
 * in this administrative section. Typicaly, some method preforms an operation
 * and returns an object of type FlowResult, then the flow script can inspect
 * the results object to determine what the next course of action is.
 * 
 * Basicaly, this results object stores all the errors and contiunation states
 * that need to be represented. There are four types of information stored:
 * 
 * 1) Continuation, this is a simple boolean variable that indicates wheather
 * the required operation is complete and the user may continue on to the next step.
 * 
 * 2) Notice information, this is a simple encoding of a notice message to be displayed
 * to the user on their next step. There are four parts: outcome, header, message, and
 * characters. See each field for more description on each part. Note: either a message
 * or characters are required.
 * 
 * 3) Errors, this is a list of errors that were encountered during processing. 
 * Typical, it just consists of a list of errored fields. However occasionaly there 
 * may be other specialized errors listed.
 * 
 * 4) Parameters, this is a map of attached parameters that may be relevnt to the 
 * result. This should be used for things such as generated id's when objects are newly
 * created.
 * 
 * @author Scott Phillips
 */
public class FlowResult {

	/**
	 * Determine wheather the operation has been completed enough that the user
	 * may successufully continue on to the next step.
	 */
	private boolean continuep;
	
	/**
	 * Notice parameters:
	 * 
	 * Outcome: The outcome of the notice, may be either success, failure, or netural.
	 * 
	 * Header: The notice's label, an i18n dictionary key.
	 * 
	 * message: The main body of the notice, an i18n dictionary key.
	 * 
	 * characters: Supplementaiol information for the notice, plain text. This is 
	 * typicaly used for exepctions.
	 *
	 */
	private enum Outcome{ SUCCESS, FAILURE, NETURAL};
	private Outcome outcome = Outcome.NETURAL;
	private Message header;
	private Message message;
	private String characters;
	
	/**
	 * A list of errors encountered while processing this operation.
	 */
	private List<String> errors;
	
	
	/**
	 * Any parameters that may be attached to this result.
	 */
	private Map<String,Object> parameters;
	
	/**
	 * Set the continuation parameter determining if the
	 * user should progress to the next step in the flow.
	 */
	public void setContinue(boolean continuep)
	{
		this.continuep = continuep;
	}
	
	/**
	 * Determine if the user should progress to the
	 * next step in the flow.
	 * 
	 * @return the continuation parameter
	 */
	public boolean getContinue()
	{
		return this.continuep;
	}
	
	
	
	/**
	 * Set the notice outcome to either success or failure. Note, 
	 * the default outcome is netural, once an outcome is set the
	 * netural outcome can never be atained again.
	 * 
	 * @param success True for success, false for failure.
	 */
	public void setOutcome(boolean success)
	{
		if (success)
			outcome = Outcome.SUCCESS;
		else
			outcome = Outcome.FAILURE;
	}
	
	/**
	 * Get the notice outcome in string form, either success 
	 * or failure. If the outcome is netural then null is returned.
	 */
	public String getOutcome()
	{
		if (outcome == Outcome.SUCCESS)
		{
			return "success";
		}
		else if (outcome == Outcome.FAILURE)
		{
			return "failure";
		}
		return null;
	}
	
	/**
	 * Set the notice header.
	 * 
	 * This must be an i18n dictionary key
	 */
	public void setHeader(Message header)
	{
		this.header = header;
	}
	
	/**
	 * Return the notice header
	 */
	public String getHeader()
	{
		if (this.header != null)
			return this.header.getKey();
		return null;
	}
	
	/**
	 * Set the notice message
	 * 
	 * This must be an i18n dictionary key
	 */
	public void setMessage(Message message)
	{
		this.message = message;
	}
	
	/**
	 * return the notice message
	 */
	public String getMessage()
	{
		if (this.message != null)
			return this.message.getKey();
		return null;
	}
	
	/**
	 * Set the notice characters
	 */
	public void setCharacters(String characters)
	{
		this.characters = characters;
	}
	
	/**
	 * Return the notice characters
	 */
	public String getCharacters()
	{
		return this.characters;
	}
	
	/**
	 * Set the results errors, note this will remove any previous errors.
	 * 
	 * @param errors New error list.
	 */
	public void setErrors(List<String> errors)
	{
		this.errors = errors;
	}
	
	/**
	 * Add a new single error to the error list.
	 * 
	 * @param newError New error.
	 */
	public void addError(String newError)
	{
		if (this.errors == null)
			this.errors = new ArrayList<String>();
		
		this.errors.add(newError);
	}
	
	/**
	 * Return the current list of errors.
	 */
	public List<String> getErrors()
	{
		return this.errors;
	}
	
	/**
	 * Return the list of errors in string form, i.e. a comma seperated list
	 * of errors. If there are no errors then null is returned.
	 */
	public String getErrorString()
	{
		if (errors == null || errors.size() == 0)
			return null;
		
		String errorString = null;
		for (String error : errors)
		{
			if (errorString == null)
				errorString = error;
			else
				errorString += ","+error;
		}
		return errorString;
	}
	
	
	/**
	 * Attatch a new parameter to this result object with the specified
	 * name & value.
	 * 
	 * @param name The parameter's name
	 * @param value The parameter's value.
	 */
	public void setParameter(String name, Object value)
	{
		if (this.parameters == null)
			this.parameters = new HashMap<String,Object>();
		
		this.parameters.put(name, value);
	}
	
	/**
	 * Find the attached parameter with the given name. If no parameter is 
	 * found for the specified name then null is returned.
	 * 
	 * @param name The parameter's name.
	 * @return The parameter's value.
	 */
	public Object getParameter(String name)
	{
		if (this.parameters == null)
			return null;
		
		return this.parameters.get(name);
	}
}
