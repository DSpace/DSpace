/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.workflow;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

public class ApproveError extends AbstractStep
{

    /** Language Strings **/
    protected static final Message T_info1=
        message("xmlui.Submission.workflow.PerformTaskStep.info1");
    protected static final Message T_cancel_submit =
        message("xmlui.general.cancel");
    protected static final Message T_confirm_note =
            message("xmlui.Submission.workflow.ApproveError.note");
    protected static final Message T_confirm =
            message("xmlui.general.perform");

	public ApproveError()
	{
		this.requireWorkflow = true;
	}
	
	
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	// Get any metadata that may be removed by unselecting one of these options.
    	Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow";

    	Request request = ObjectModelHelper.getRequest(objectModel);

        // Generate a from asking the user two questions: multiple
        // titles & published before.
        Division div = body.addInteractiveDivision("perform-task", actionURL,
                Division.METHOD_POST, "primary workflow");
        div.setHead(T_workflow_head);

        Para para = div.addPara("approve-error","alert alert-error");
        para.addContent(T_confirm_note);
        para = div.addPara();
        para.addButton("submit_force").setValue(T_confirm);
        para.addButton("submit_quit").setValue(T_cancel_submit);
        
        div.addHidden("submission-continue").setValue(knot.getId()); 
    }
}
