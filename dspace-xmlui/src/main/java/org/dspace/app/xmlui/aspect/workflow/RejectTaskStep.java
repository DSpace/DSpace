/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.workflow;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.LogManager;
import org.xml.sax.SAXException;

/**
 * This step is used when the user has selected to
 * reject the item. Here they are asked to enter
 * a reason why the item should be rejected.
 * 
 * @author Scott Phillips
 */
public class RejectTaskStep extends AbstractStep
{
    private static final Logger log = Logger.getLogger(RejectTaskStep.class);

    /** Language Strings **/
    protected static final Message T_info1 = 
        message("xmlui.Submission.workflow.RejectTaskStep.info1");
    protected static final Message T_reason = 
        message("xmlui.Submission.workflow.RejectTaskStep.reason");
    protected static final Message T_reason_required = 
        message("xmlui.Submission.workflow.RejectTaskStep.reason_required");
    protected static final Message T_submit_reject = 
        message("xmlui.Submission.workflow.RejectTaskStep.submit_reject");
    protected static final Message T_submit_cancel = 
        message("xmlui.general.cancel");
	
	
	
	
	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public RejectTaskStep()
	{
		this.requireWorkflow = true;
	}
	
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
    	Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow";
    	
    	Request request = ObjectModelHelper.getRequest(objectModel);
		String showfull = request.getParameter("showfull");
		
		// if the user selected showsimple, remove showfull.
		if (showfull != null && request.getParameter("showsimple") != null)
        {
            showfull = null;
        }
		
		

    	Division div = body.addInteractiveDivision("reject-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead(T_workflow_head);
    	
    	
        if (showfull == null)
        {
	        ReferenceSet referenceSet = div.addReferenceSet("narf",ReferenceSet.TYPE_SUMMARY_VIEW);
	        referenceSet.addReference(item);
	        div.addPara().addButton("showfull").setValue(T_showfull);
        } 
        else
        {
            ReferenceSet referenceSet = div.addReferenceSet("narf",ReferenceSet.TYPE_DETAIL_VIEW);
            referenceSet.addReference(item);
            div.addPara().addButton("showsimple").setValue(T_showsimple);
            
            div.addHidden("showfull").setValue("true");
        }
        

        List form = div.addList("reject-workflow",List.TYPE_FORM);
        
        form.addItem(T_info1);
        
        TextArea reason = form.addItem().addTextArea("reason");
        reason.setLabel(T_reason);
        reason.setRequired();
        reason.setSize(15, 50);
        if (this.errorFields.contains("reason"))
        {
            reason.addError(T_reason_required);
        }
    	
        org.dspace.app.xmlui.wing.element.Item actions = form.addItem();
        actions.addButton("submit_reject").setValue(T_submit_reject);
        actions.addButton("submit_cancel").setValue(T_submit_cancel);
        
        div.addHidden("submission-continue").setValue(knot.getId()); 

        log.info(LogManager.getHeader(context, "get_reject_reason",
                "workflow_id=" + submission.getID() + ",item_id="
                        + item.getID()));
    }
}
