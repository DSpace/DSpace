/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.concept;

import java.sql.SQLException;
import java.util.*;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.administrative.FlowGroupUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Term;
import org.dspace.eperson.Group;

/**
 * Edit an existing Concept, display all the concept's metadata
 * along with two special options two reset the concept's
 * password and delete this user. 
 *
 * @author Alexey Maslov
 */
public class EditConceptForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_submit_save =
            message("xmlui.general.save");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");

    private static final Message T_title =
            message("xmlui.administrative.concept.EditConceptForm.title");

    private static final Message T_concept_trail =
            message("xmlui.administrative.concept.general.concept_trail");

    private static final Message T_trail =
            message("xmlui.administrative.concept.EditConceptForm.trail");

    private static final Message T_head1 =
            message("xmlui.administrative.concept.EditConceptForm.head1");

    private static final Message T_literalForm_taken =
            message("xmlui.administrative.concept.EditConceptForm.literalForm_taken");

    private static final Message T_head2 =
            message("xmlui.administrative.concept.EditConceptForm.head2");

    private static final Message T_error_literalForm_unique =
            message("xmlui.administrative.concept.EditConceptForm.error_literalForm_unique");

    private static final Message T_error_literalForm =
            message("xmlui.administrative.concept.EditConceptForm.error_literalForm");

    private static final Message T_error_status =
            message("xmlui.administrative.concept.EditConceptForm.error_status");

    private static final Message T_error_source =
            message("xmlui.administrative.concept.EditConceptForm.error_source");

    private static final Message T_move_term =
            message("xmlui.administrative.concept.EditConceptForm.move_term");

    private static final Message T_move_term_help =
            message("xmlui.administrative.concept.EditConceptForm.move_term_help");

    private static final Message T_submit_delete =
            message("xmlui.administrative.concept.EditConceptForm.submit_delete");

    private static final Message T_submit_login_as =
            message("xmlui.administrative.concept.EditConceptForm.submit_login_as");

    /** Language string used: */

    private static final Message T_literalForm_address =
            message("xmlui.Concept.EditProfile.literalForm_address");

    private static final Message T_status =
            message("xmlui.Concept.EditProfile.status");

    private static final Message T_top_concept =
            message("xmlui.Concept.EditProfile.topConcept");

    private static final Message T_identifier =
            message("xmlui.Concept.EditProfile.identifier");
    private static final Message T_lang =
            message("xmlui.Concept.EditProfile.language");

    // How many results to show on a page.
    private static final int RESULTS_PER_PAGE = 5;

    /** The maximum size of a collection name allowed */
    private static final int MAX_COLLECTION_NAME = 25;



    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        int conceptID = parameters.getParameterAsInteger("concept",-1);
        try{
            Concept concept = Concept.find(context, conceptID);
            if(concept!=null)
            {
                pageMeta.addTrailLink(contextPath + "/concept/"+concept.getID(), concept.getLabel());
            }
        }catch (Exception e)
        {
            return;
        }
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        boolean admin = AuthorizeManager.isAdmin(context);

        Request request = ObjectModelHelper.getRequest(objectModel);

        // Get our parameters;
        int conceptID = parameters.getParameterAsInteger("concept",-1);
        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

        // Grab the person in question
        Concept concept = Concept.find(context, conceptID);

        if (concept == null)
        {
            throw new UIException("Unable to find concept for id:" + conceptID);
        }

        String status = concept.getStatus();
        Boolean topConcept  = concept.getTopConcept();
        String identifier = concept.getIdentifier();
        String language = concept.getLang();

        if (request.getParameter("status") != null)
        {
            status = request.getParameter("status");
        }
        if (request.getParameter("topConcept") != null)
        {
            topConcept    = (request.getParameter("topConcept") == null)  ? false : true;
        }

        if (request.getParameter("lang") != null)
        {
            language = request.getParameter("lang");
        }
        String formUrl = contextPath+"/admin/concept";
        String schemeId = request.getParameter("schemeID");
        if(schemeId!=null&&schemeId.length()>0)
        {
            formUrl =contextPath + "/admin/scheme";
        }

        // DIVISION: concept-edit
        Division edit = body.addInteractiveDivision("concept-edit",formUrl,Division.METHOD_POST,"primary administrative concept");
        edit.setHead(T_head1);


        if (errors.contains("concept_literalForm_key")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_literalForm_taken);
        }


        List identity = edit.addList("form",List.TYPE_FORM);
        identity.setHead(T_head2.parameterize(concept.getID()));
        identity.addItem().addHidden("conceptId").setValue(concept.getID());
        if (admin)
        {
            identity.addLabel("Status");
            Select statusSelect=identity.addItem().addSelect("status");
            statusSelect.addOption(Concept.Status.ACCEPTED.name(),"Accepted");
            statusSelect.addOption(Concept.Status.CANDIDATE.name(),"Candidate");
            statusSelect.addOption(Concept.Status.WITHDRAWN.name(),"Withdraw");
            if(status!=null)
            {
                if(status.equals(Concept.Status.CANDIDATE.name()))
                {
                    statusSelect.setOptionSelected(Concept.Status.CANDIDATE.name());
                }
                else if(status.equals(Concept.Status.WITHDRAWN.name()))
                {
                    statusSelect.setOptionSelected(Concept.Status.WITHDRAWN.name());
                }
                else if(status.equals(Concept.Status.ACCEPTED.name()))
                {
                    statusSelect.setOptionSelected(Concept.Status.ACCEPTED.name());
                }
            }
        }
        else
        {
            identity.addLabel(T_status);
            identity.addItem(status);
        }

        if (admin)
        {
            CheckBox topConceptBox = identity.addItem().addCheckBox("topConcept");
            topConceptBox.setLabel(T_top_concept);
            topConceptBox.addOption(topConcept, "yes");
        }
        else
        {
            identity.addLabel(T_top_concept);
            identity.addItem();
        }


        identity.addLabel(T_identifier);
        identity.addItem(identifier);


        if (admin)
        {
            Text langText = identity.addItem().addText("lang");
            langText.setLabel(T_lang);
            langText.setValue(language);
        }
        else
        {
            identity.addLabel(T_lang);
            identity.addItem(language);
        }

//        // Get search parameters
//        String query = decodeFromURL(parameters.getParameter("query",null));
//        int page     = parameters.getParameterAsInteger("page",0);
//        String type  = parameters.getParameter("type",null);
//
//
//
//        // Get list of member groups
//        String memberConceptIDsString = parameters.getParameter("memberConceptIDs",null);
//        java.util.List<Integer> memberConceptIDs = new ArrayList<Integer>();
//        if (memberConceptIDsString != null)
//        {
//            for (String id : memberConceptIDsString.split(","))
//            {
//                if (id.length() > 0)
//                {
//                    memberConceptIDs.add(Integer.valueOf(id));
//                }
//            }
//        }
//
//
//        Para searchBoxes = edit.addPara();
//        searchBoxes.addContent("Search members to add: ");
//        Text queryField = searchBoxes.addText("query");
//        queryField.setValue(query);
//        queryField.setSize(15);
//        searchBoxes.addButton("submit_search_terms").setValue("search terms");
//        searchBoxes.addButton("submit_search_concepts").setValue("search concepts");
//
//        if (query != null)
//        {
//            if ("term".equals(type))
//            {
//                searchBoxes.addButton("submit_clear").setValue("clear terms");
//                addTermSearch(edit, query, page, concept, memberConceptIDs);
//            }
//            else if ("concept".equals(type))
//            {
//                searchBoxes.addButton("submit_clear").setValue("clear concepts");
//                addConceptSearch(edit, concept, query, page, concept, memberConceptIDs);
//            }
//        }



        Item buttons = identity.addItem();
        if (admin)
        {
            buttons.addButton("submit_save").setValue(T_submit_save);
            buttons.addButton("submit_delete").setValue("Delete");
        }
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        edit.addHidden("administrative-continue").setValue(knot.getId());
    }



    /**
     * Search for concept to add to this group.
     */
    private void addTermSearch(Division div, String query, int page, Concept concept, java.util.List<Integer> memberconceptIDs) throws SQLException, WingException
    {
        String conceptId = null;
        if(concept!=null)
        {
            conceptId = Integer.toString(concept.getID());
        }
        int resultCount = Term.searchResultCount(context, query, conceptId);


        Term[] terms = Term.search(context, query, page * RESULTS_PER_PAGE, RESULTS_PER_PAGE, conceptId);

        Division results = div.addDivision("results");

        if (resultCount > RESULTS_PER_PAGE)
        {
            // If there are enough results then paginate the results
            String baseURL = contextPath +"/admin/concept?administrative-continue="+knot.getId();
            int firstIndex = page*RESULTS_PER_PAGE+1;
            int lastIndex = page*RESULTS_PER_PAGE + terms.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / RESULTS_PER_PAGE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            results.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
        }

        /* Set up a table with search results (if there are any). */
        Table table = results.addTable("group-edit-search-eperson",terms.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent("1");
        header.addCell().addContent("2");
        header.addCell().addContent("3");
        header.addCell().addContent("4");

        for (Term term : terms)
        {
            String termId = String.valueOf(term.getID());
            String literalForm = term.getLiteralForm();
            String lang = term.getLang();
            String url = contextPath+"/admin/term?administrative-continue="+knot.getId()+"&submit_edit_term&termID="+termId;



            Row personData = table.addRow();

            personData.addCell().addContent(term.getID());
            personData.addCell().addXref(url, literalForm);
            personData.addCell().addXref(url, lang);

        }

        if (terms.length <= 0) {
            table.addRow().addCell(1, 4).addContent("no results");
        }
    }

    /**
     * Search for groups to add to this group.
     */
    private void addConceptSearch(Division div, Concept sourceConcept, String query, int page, Concept parent, java.util.List<Integer> memberGroupIDs) throws WingException, SQLException
    {
        String conceptId = null;
        if(sourceConcept!=null)
        {
            conceptId = Integer.toString(sourceConcept.getID());
        }

        int resultCount = Concept.searchResultCount(context, query, conceptId);
        Concept[] concepts = Concept.search(context, query, page * RESULTS_PER_PAGE, RESULTS_PER_PAGE, conceptId);

        Division results = div.addDivision("results");

        if (resultCount > RESULTS_PER_PAGE)
        {
            // If there are enough results then paginate the results
            String baseURL = contextPath +"/admin/concept?administrative-continue="+knot.getId();
            int firstIndex = page*RESULTS_PER_PAGE+1;
            int lastIndex = page*RESULTS_PER_PAGE + concepts.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / RESULTS_PER_PAGE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            results.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
        }

        Table table = results.addTable("roup-edit-search-group",concepts.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent("1");
        header.addCell().addContent("2");
        header.addCell().addContent("3");
        header.addCell().addContent("4");
        header.addCell().addContent("5");

        for (Concept concept : concepts)
        {
            String conceptID = String.valueOf(concept.getID());
            String name = concept.getName();
            String url = contextPath+"/admin/concept?administrative-continue="+knot.getId()+"&submit_edit_concept&conceptID="+conceptID;
            int memberCount = concept.getChildConcepts().length + concept.getChildConcepts().length;

            Row row = table.addRow();

            row.addCell().addContent(conceptId);
            if (AuthorizeManager.isAdmin(context))
            // Only administrators can edit other groups.
            {
                row.addCell().addXref(url, name);
            }
            else
            {
                row.addCell().addContent(name);
            }



            row.addCell().addContent(memberCount == 0 ? "-" : String.valueOf(memberCount));

            Cell cell = row.addCell();
            if (FlowGroupUtils.getCollectionId(concept.getName()) > -1)
            {
                org.dspace.content.Collection collection = org.dspace.content.Collection.find(context, FlowGroupUtils.getCollectionId(concept.getName()));
                if (collection != null)
                {
                    String collectionName = collection.getMetadata("name");

                    if (collectionName == null)
                    {
                        collectionName = "";
                    }
                    else if (collectionName.length() > MAX_COLLECTION_NAME)
                    {
                        collectionName = collectionName.substring(0, MAX_COLLECTION_NAME - 3) + "...";
                    }

                    cell.addContent(collectionName+" ");

                    Highlight highlight = cell.addHighlight("fade");
                    highlight.addContent("[");
                    highlight.addXref(contextPath+"/handle/"+collection.getHandle(), "link");
                    highlight.addContent("]");
                }
            }

            //todo:circle detector
//            else if (isDescendant(concepts, concept, concepts))
//            {
//                row.addCellContent("circle");
//            }
            else
            {
                row.addCell().addButton("submit_add_group_"+conceptID).setValue("add");
            }

        }
        if (concepts.length <= 0) {
            table.addRow().addCell(1, 4).addContent("no results");
        }
    }
    /**
     * Method to extensively check whether the first group has the second group as a distant
     * parent. This is used to avoid creating cycles like A->B, B->C, C->D, D->A which leads
     * all the groups involved to essentially include themselves.
     */
    private boolean isDescendant(Concept descendant, Concept ancestor, java.util.List<Integer> memberGroupIDs) throws SQLException
    {
        Queue<Group> toVisit = new LinkedList<Group>();
        Group currentGroup;

        //toVisit.offer(ancestor);

        // Initialize by adding a list of our current list of group members.
        for (Integer groupid : memberGroupIDs)
        {
            Group member = Group.find(context,groupid);
            toVisit.offer(member);
        }

        while (!toVisit.isEmpty()) {
            // 1. Grab a group from the queue
            currentGroup = toVisit.poll();

            // 2. See if it's the descendant we're looking for
            if (currentGroup.equals(descendant)) {
                return true;
            }

            // 3. If not, add that group's children to the queue
            for (Group nextBatch : currentGroup.getMemberGroups()) {
                toVisit.offer(nextBatch);
            }
        }
        return false;
    }

}
