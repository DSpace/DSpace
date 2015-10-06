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
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User interface for an action where an assigned user can
 * assign another user to review the item
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SelectReviewerAction extends AbstractXMLUIAction {

    private static final Message T_HEAD = message("xmlui.XMLWorkflow.workflow.SelectReviewerAction.head");


    // EPeople Search
    private static final Message T_epeople_column1 =
        message("xmlui.administrative.group.EditGroupForm.epeople_column1");

    private static final Message T_epeople_column2 =
        message("xmlui.administrative.group.EditGroupForm.epeople_column2");

    private static final Message T_epeople_column3 =
        message("xmlui.administrative.group.EditGroupForm.epeople_column3");

    private static final Message T_epeople_column4 =
        message("xmlui.administrative.group.EditGroupForm.epeople_column4");


    protected static final Message T_submit_cancel = message("xmlui.general.cancel");
    private static final Message T_search_reviewer_label = message("xmlui.XMLWorkflow.workflow.SelectReviewerAction.search.label");
    private static final Message T_search_reviewer_button = message("xmlui.XMLWorkflow.workflow.SelectReviewerAction.search.button");
    private static final Message T_select_reviewer_button = message("xmlui.XMLWorkflow.workflow.SelectReviewerAction.select-reviewer.button");
    private static final Message T_task_help = message("xmlui.XMLWorkflow.workflow.SelectReviewerAction.help");

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);

        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/xmlworkflow";
        Division viewItemDiv = body.addInteractiveDivision("view-item", actionURL, Division.METHOD_POST, "primary workflow");

        viewItemDiv.setHead(T_HEAD);


        addWorkflowItemInformation(viewItemDiv, item, request);

        viewItemDiv.addHidden("submission-continue").setValue(knot.getId());


        Division actionsDiv = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");

        actionsDiv.addPara().addContent(T_task_help);

        Para selectUserPara = actionsDiv.addPara();

        selectUserPara.addContent(T_search_reviewer_label);
        Text queryBox = selectUserPara.addText("query");
        if(request.getParameter("query") != null){
            queryBox.setValue(request.getParameter("query"));
        }

        selectUserPara.addButton("submit_search").setValue(T_search_reviewer_button);

        //Retrieve our pagenumber
        int page = org.dspace.xmlworkflow.state.actions.processingaction.SelectReviewerAction.MAIN_PAGE;
        if(request.getAttribute("page") != null){
            page = Integer.parseInt(request.getAttribute("page").toString());
        }

        if(page == org.dspace.xmlworkflow.state.actions.processingaction.SelectReviewerAction.SEARCH_RESULTS_PAGE){
            renderSearchResults(request, actionURL, actionsDiv);
        }



        Para actionsPara = actionsDiv.addPara();
        actionsPara.addButton("submit_cancel").setValue(T_submit_cancel);

        actionsDiv.addHidden("submission-continue").setValue(knot.getId());
    }

    private void renderSearchResults(Request request, String actionUrl, Division div) throws WingException {
        //We need to show our search results
        //Retrieve em
        java.util.List<EPerson> epeople = (java.util.List<EPerson>) request.getAttribute("eperson-results");
        int resultCount = (Integer) request.getAttribute("eperson-result-count");
        int page = (Integer) request.getAttribute("result-page");
        Division results = div.addDivision("results");

        if (resultCount > org.dspace.xmlworkflow.state.actions.processingaction.SelectReviewerAction.RESULTS_PER_PAGE)
        {
            // If there are enough results then paginate the results
            String baseURL = actionUrl + "?submission-continue="+knot.getId() + "&submit_search=search";
            baseURL += "&query=" + request.getParameter("query");
            int firstIndex = page* org.dspace.xmlworkflow.state.actions.processingaction.SelectReviewerAction.RESULTS_PER_PAGE+1;
            int lastIndex = page* org.dspace.xmlworkflow.state.actions.processingaction.SelectReviewerAction.RESULTS_PER_PAGE + epeople.size();

            String nextURL = null, prevURL = null;
            if (page < (resultCount / org.dspace.xmlworkflow.state.actions.processingaction.SelectReviewerAction.RESULTS_PER_PAGE))
            {
                nextURL = baseURL + "&result-page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&result-page=" + (page - 1);
            }

            results.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
        }


        /* Set up a table with search results (if there are any). */
        Table table = results.addTable("group-edit-search-eperson",epeople.size() + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_epeople_column1);
        header.addCell().addContent(T_epeople_column2);
        header.addCell().addContent(T_epeople_column3);
        header.addCell().addContent(T_epeople_column4);

        for (EPerson person : epeople)
        {
            String epersonID = String.valueOf(person.getID());
            String fullName = person.getFullName();
            String email = person.getEmail();

            Row personData = table.addRow();

            personData.addCell().addContent(person.getID().toString());
            personData.addCell().addContent(fullName);
            personData.addCell().addContent(email);

            personData.addCell().addButton("submit_select_reviewer_"+epersonID).setValue(T_select_reviewer_button);
        }
    }
}
