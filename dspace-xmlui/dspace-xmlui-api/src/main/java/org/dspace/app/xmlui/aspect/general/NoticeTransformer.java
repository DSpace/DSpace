/*
 * NoticeTransformer.java
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
package org.dspace.app.xmlui.aspect.general;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;

/**
 * This class will add a simple notification div the DRI document. Typicaly 
 * this transformer is used after an action has been preformed to let the
 * user know if an operation succeded or failed.
 * 
 * The possible paramaters are:
 * 
 * outcome: The outcome determines whether the notice is positive or negative. 
 * Possible values are: "success", "failure", or "netural". If no values are 
 * supplied then netural is assumed.
 * 
 * header: An i18n dictionary key referencing the text that should be used
 * as a header for this notice.
 * 
 * message: An i18n dictionary key refrencing the text that should be used as
 * the content for this notice. 
 * 
 * characters: Plain text string that should be used as the content for this
 * notice. Normaly all messages should be i18n dictionary keys however this
 * parameter is usefull for error messages that are not nessasarly translated.
 * 
 * All parameters are optional but you must supply at least the message or the 
 * characters
 *
 *
 * 
 * Examlpe:
 * <map:transformer type="notice">
 *   <map:parameter name="outcome" value="success"/>
 *   <map:parameter name="message" value="xmlui.<aspect>.<class>.<type>"/>
 * </map:transformer>
 * 
 * @author Scott Phillips
 * @author Alexey Maslov
 */
public class NoticeTransformer extends AbstractDSpaceTransformer   
{
	
	/** Language Strings */
	private static final Message T_head =
		message("xmlui.general.notice.default_head");
	
	/**
	 * Add the notice div to the body.
	 */
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException 
	{
		String outcome = parameters.getParameter("outcome",null);
		String header = parameters.getParameter("header",null);
		String message = parameters.getParameter("message",null);
		String characters = parameters.getParameter("characters",null);
			    
		if ((message    == null || message.length() <= 0) && 
			(characters == null || characters.length() <= 0))
			throw new WingException("No message found.");
		
		String rend = "notice";
		if ("netural".equals(outcome))
			rend += " netural";
		else if ("success".equals(outcome))
			rend += " success";
		else if ("failure".equals(outcome))
			rend += " failure";
		
		Division div = body.addDivision("general-message",rend);
		if ((header != null) && (!"".equals(header)))
			div.setHead(message(header));
		else
			div.setHead(T_head);

		if (message != null && message.length() > 0)
			div.addPara(message(message));
		
		if (characters != null && characters.length() > 0)
			div.addPara(characters);
	}
}
