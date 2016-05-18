/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * This class will add a simple notification div the DRI document. Typically 
 * this transformer is used after an action has been performed to let the
 * user know if an operation succeeded or failed.
 * 
 * <p>The possible parameters are:
 *
 * <p>outcome: The outcome determines whether the notice is positive or negative.
 * Possible values are: "success", "failure", or "neutral". If no values are 
 * supplied then neutral is assumed.
 *
 * <p>header: An i18n dictionary key referencing the text that should be used
 * as a header for this notice.
 * 
 * <p>message: An i18n dictionary key referencing the text that should be used as
 * the content for this notice. 
 * 
 * <p>characters: Plain text string that should be used as the content for this
 * notice. Normally, all messages should be i18n dictionary keys, however this
 * parameter is useful for error messages that are not necessarily translated.
 * 
 * <p>All parameters are optional but you must supply at least the message or the
 * characters.
 *
 * <p>Example:
 * <pre>
 * {@code
 * <map:transformer type="notice">
 *   <map:parameter name="outcome" value="success"/>
 *   <map:parameter name="message" value="xmlui.<aspect>.<class>.<type>"/>
 * </map:transformer>
 * }
 * </pre>
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
	 */
    @Override
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException 
	{
		String outcome = parameters.getParameter("outcome",null);
		String header = parameters.getParameter("header",null);
		String message = parameters.getParameter("message",null);
		String characters = parameters.getParameter("characters",null);
			    
		if ((message    == null || message.length() <= 0) && 
			(characters == null || characters.length() <= 0))
        {
            throw new WingException("No message found.");
        }
		
		String rend = "notice";
		if ("neutral".equals(outcome))
        {
            rend += " neutral";
        }
		else if ("success".equals(outcome))
        {
            rend += " success";
        }
		else if ("failure".equals(outcome))
        {
            rend += " failure";
        }
		
		Division div = body.addDivision("general-message",rend);
		if ((header != null) && (!"".equals(header)))
        {
            div.setHead(message(header));
        }
		else
        {
            div.setHead(T_head);
        }

		if (message != null && message.length() > 0)
        {
            div.addPara(message(message));
        }
		
		if (characters != null && characters.length() > 0)
        {
            div.addPara(characters);
        }
	}
}
