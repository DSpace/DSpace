/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.xmlui.wing.Message;

/**
 * This class represents the results that may be generated during a flow script
 * in this administrative section. Typically, some method performs an operation
 * and returns an object of type FlowResult, then the flow script can inspect
 * the results object to determine what the next course of action is.
 * 
 * <p>Basically, this results object stores all the errors and continuation states
 * that need to be represented. There are four types of information stored:
 * 
 * <p>1) Continuation, this is a simple boolean variable that indicates whether
 * the required operation is complete and the user may continue on to the next step.
 * 
 * <p>2) Notice information, this is a simple encoding of a notice message to be displayed
 * to the user on their next step. There are four parts: outcome, header, message, and
 * characters. See each field for more description on each part. Note: either a message
 * or characters are required.
 * 
 * <p>3) Errors, this is a list of errors that were encountered during processing.
 * Typically, it just consists of a list of errored fields. However occasionally there 
 * may be other specialized errors listed.
 * 
 * <p>4) Parameters, this is a map of attached parameters that may be relevant to the
 * result. This should be used for things such as generated id's when objects are newly
 * created.
 * 
 * @author Scott Phillips
 */
public class FlowResult {

	/**
	 * Determine whether the operation has been completed enough that the user
	 * may successfully continue on to the next step.
	 */
	private boolean continuep;
	
	/**
	 * Notice parameters:
	 * 
	 * Outcome: The outcome of the notice, may be either success, failure, or neutral.
	 * 
	 * Header: The notice's label, an i18n dictionary key.
	 * 
	 * message: The main body of the notice, an i18n dictionary key.
	 * 
	 * characters: Supplemental information for the notice, plain text. This is 
	 * typically used for exceptions.
	 *
	 */
	private enum Outcome{ SUCCESS, FAILURE, NEUTRAL};
	private Outcome outcome = Outcome.NEUTRAL;
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
     * @param continuep true if should continue.
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
	 * the default outcome is neutral, once an outcome is set the
	 * neutral outcome can never be attained again.
	 * 
	 * @param success True for success, false for failure.
	 */
	public void setOutcome(boolean success)
	{
		if (success)
        {
            outcome = Outcome.SUCCESS;
        }
		else
        {
            outcome = Outcome.FAILURE;
        }
	}
	
	/**
	 * Get the notice outcome in string form, either success 
	 * or failure. If the outcome is neutral then null is returned.
     * @return the outcome.
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
     * @param header the header.
	 */
	public void setHeader(Message header)
	{
		this.header = header;
	}
	
	/**
	 * Return the notice header.
     * @return the header.
	 */
	public String getHeader()
	{
		if (this.header != null)
        {
            return this.header.getKey();
        }
		return null;
	}
	
	/**
	 * Set the notice message.
	 * 
	 * This must be an i18n dictionary key.
     * @param message the message.
	 */
	public void setMessage(Message message)
	{
		this.message = message;
	}
	
	/**
	 * return the notice message.
     * @return the notice.
	 */
	public String getMessage()
	{
		if (this.message != null)
        {
            return this.message.getKey();
        }
		return null;
	}
	
	/**
	 * Set the notice characters.
     * @param characters the notice.
	 */
	public void setCharacters(String characters)
	{
		this.characters = characters;
	}
	
	/**
	 * Return the notice characters
     * @return the notice.
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
        {
            this.errors = new ArrayList<>();
        }
		
		this.errors.add(newError);
	}
	
	/**
	 * Return the current list of errors.
     * @return a list of errors.
	 */
	public List<String> getErrors()
	{
		return this.errors;
	}
	
	/**
	 * Return the list of errors in string form, i.e. a comma-separated list
	 * of errors. If there are no errors then null is returned.
     * @return a list of errors.
	 */
	public String getErrorString()
	{
		if (errors == null || errors.isEmpty())
        {
            return null;
        }
		
		String errorString = null;
		for (String error : errors)
		{
			if (errorString == null)
            {
                errorString = error;
            }
			else
            {
                errorString += "," + error;
            }
		}
		return errorString;
	}
	
	
	/**
	 * Attach a new parameter to this result object with the specified
	 * name and value.
	 *
	 * @param name The parameter's name
	 * @param value The parameter's value.
	 */
	public void setParameter(String name, Object value)
	{
		if (this.parameters == null)
        {
            this.parameters = new HashMap<>();
        }
		
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
        {
            return null;
        }
		
		return this.parameters.get(name);
	}
}
