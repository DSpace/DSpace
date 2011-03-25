/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * This is a confirmation page informing the user that they have
 * successfully removed their submission.
 * <P>
 * As such, it's not a true "step" in the submission process
 * 
 * @author Scott Phillips
 */
public class RemovedStep extends AbstractStep
{

	/** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.RemovedStep.head");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.RemovedStep.info1");
    protected static final Message T_go_submissions = 
        message("xmlui.Submission.submit.RemovedStep.go_submission");
    
	
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{	
		Division div = body.addInteractiveDivision("submit-removed",contextPath+"/submit", Division.METHOD_POST,"primary submission");
		
		div.setHead(T_head);
		
		div.addPara(T_info1);
		
		div.addPara().addXref(contextPath+"/submissions",T_go_submissions);
	}

}
