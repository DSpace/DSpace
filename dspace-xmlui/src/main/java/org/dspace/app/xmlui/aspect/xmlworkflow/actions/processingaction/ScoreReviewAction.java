/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow.actions.processingaction;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.xmlworkflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 *
 * User interface for the score review action
 * This action will allow multiple users to rate a certain item
 * if the mean of this score is higher then the minimum score the
 * item will be sent to the next action/step else it will be rejected
 * 
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ScoreReviewAction extends AbstractXMLUIAction {

    private static final Message T_HEAD = message("xmlui.XMLWorkflow.workflow.ScoreReviewAction.head");


    private static final Message T_cancel_submit = message("xmlui.general.cancel");
    private static final Message T_score_button = message("xmlui.XMLWorkflow.workflow.ScoreReviewAction.score.button");
    private static final Message T_score_help = message("xmlui.XMLWorkflow.workflow.ScoreReviewAction.help");


    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);


        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/xmlworkflow";

    	Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead(T_HEAD);

        addWorkflowItemInformation(div, item, request);

        Para scorePara = div.addPara();
        scorePara.addContent(T_score_help);
        Select scoreSelect = scorePara.addSelect("score");
        for(int i = 0; i <= 100; i+=10){
            scoreSelect.addOption(i, i + "%");
        }
        scorePara.addButton("submit_score").setValue(T_score_button);


        div.addPara().addButton("submit_leave").setValue(T_cancel_submit);


        div.addHidden("submission-continue").setValue(knot.getId());
    }
}
