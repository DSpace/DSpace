package org.dspace.app.xmlui.aspect.authority.concept;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Concept2ConceptRole;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Concept2ConceptRole;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * User: lantian @ atmire . com
 * Date: 5/16/14
 * Time: 10:20 AM
 */
public class AddConcept2ConceptRelationForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_concept_trail =
            message("xmlui.administrative.concept.general.epeople_trail");

    private static final Message T_title =
            message("xmlui.administrative.concept.AddConceptForm.title");

    private static final Message T_trail =
            message("xmlui.administrative.concept.AddConceptForm.trail");

    private static final Message T_head1 =
            message("xmlui.administrative.concept.AddConceptForm.head1");

    private static final Message T_email_taken =
            message("xmlui.administrative.concept.AddConceptForm.email_taken");

    private static final Message T_head2 =
            message("xmlui.administrative.concept.AddConceptForm.head2");


    private static final Message T_top_concept =
            message("xmlui.administrative.concept.AddConceptForm.top_concept");

    private static final Message T_submit_create =
            message("xmlui.administrative.concept.AddConceptForm.submit_create");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");
    private static final int PAGE_SIZE = 15;

    /** Language string used from other aspects: */

    private static final Message T_email_address =
            message("xmlui.Concept.EditProfile.email_address");

    private static final Message T_first_name =
            message("xmlui.Concept.EditProfile.first_name");

    private static final Message T_last_name =
            message("xmlui.Concept.EditProfile.last_name");

    private static final Message T_telephone =
            message("xmlui.Concept.EditProfile.telephone");

    private static final Message T_search_head =
            message("xmlui.aspect.authority.concept.ManageConceptMain.search_head");

    private static final Message T_search_column1 =
            message("xmlui.aspect.authority.concept.ManageConceptMain.search_column1");

    private static final Message T_search_column2 =
            message("xmlui.aspect.authority.concept.ManageConceptMain.search_column2");

    private static final Message T_search_column3 =
            message("xmlui.aspect.authority.concept.ManageConceptMain.search_column3");

    private static final Message T_search_column4 =
            message("xmlui.aspect.authority.concept.ManageConceptMain.search_column4");

    private static final Message T_submit_delete =
            message("xmlui.aspect.authority.concept.ManageConceptMain.submit_delete");

    private static final Message T_no_results =
            message("xmlui.aspect.authority.concept.ManageConceptMain.no_results");

    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/concept",T_concept_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        Request request = ObjectModelHelper.getRequest(objectModel);

        int page          = parameters.getParameterAsInteger("page",0);
        int highlightID   = parameters.getParameterAsInteger("highlightID",-1);
        String query      = decodeFromURL(parameters.getParameter("query",null));

        String baseURL    = contextPath+"/admin/concept?administrative-continue="+knot.getId();

        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }
        Boolean topConcept = (request.getParameter("topConcept") == null)  ? false : true;
        String language = request.getParameter("language");
        String status = request.getParameter("status");
        String identifier = request.getParameter("identifier");
        String formUrl = baseURL;
        String schemeId = request.getParameter("schemeID");
        if(schemeId!=null&&schemeId.length()>0)
        {
            formUrl =contextPath + "/admin/scheme";
        }
        int resultCount = Concept.searchResultCount(context, query, null);
        Concept[] concepts = Concept.search(context, query, page * PAGE_SIZE, PAGE_SIZE, null);

        // DIVISION: concept-add
        Division add = body.addInteractiveDivision("concept-add",formUrl,Division.METHOD_POST,"primary administrative concept");

        add.setHead("Add Child Concept");

        if(errors!=null&&errors.size()>0)
        {
            for ( String e:errors)
            {
                add.addPara(e);
            }

        }

        List secondConcept =  add.addList("second-concept");
        secondConcept.addLabel("Search a Concept");
        //secondConcept.addItem().addText("second-concept");
        Text queryField = secondConcept.addItem().addText("query");
        if (query != null)
        {
            queryField.setValue(query);
        }
        secondConcept.addItem().addButton("submit_search").setValue("Search");
        List role =  add.addList("role-id");
        role.addLabel("Select a Role");
        Item roleItem = role.addItem();
        Concept2ConceptRole[] concept2Concepts = Concept2ConceptRole.findAll(context);
        Select select = roleItem.addSelect("roleId");
        for(Concept2ConceptRole concept2ConceptRole:concept2Concepts)
        {
            select.addOption(concept2ConceptRole.getRelationID(),concept2ConceptRole.getLabel());
        }


        // DIVISION: concept-search
        Division search = add.addDivision("concept-search");
        search.setHead(T_search_head);

        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page*PAGE_SIZE+1;
            int lastIndex = page*PAGE_SIZE + concepts.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / PAGE_SIZE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            search.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
        }

        Table table = search.addTable("concept-search-table", concepts.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent("Selected");
        CheckBox selectConcept;
        for (Concept concept : concepts)
        {
            String conceptID = String.valueOf(concept.getID());
            String url = "/concept/"+conceptID;


            //java.util.List<String> deleteConstraints = concept.getDeleteConstraints();

            Row row;
            if (concept.getID() == highlightID)
            {
                // This is a highlighted concept
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            row.addCellContent(conceptID);
            row.addCell().addXref("/concept/"+concept.getID(), concept.getCreated().toString());
            row.addCell().addXref("/concept/"+concept.getID(), concept.getIdentifier());
            row.addCell().addXref("/concept/"+concept.getID(), concept.getLabel());
            selectConcept = row.addCell().addCheckBox("select_concepts");
            selectConcept.setLabel(conceptID);
            selectConcept.addOption(conceptID);
        }

        if (concepts.length <= 0)
        {
            Cell cell = table.addRow().addCell(1, 4);
            cell.addHighlight("italic").addContent(T_no_results);
        }


        add.addHidden("administrative-continue").setValue(knot.getId());


        Item buttons = role.addItem();
        buttons.addButton("submit_add").setValue("Add");
        buttons.addButton("submit_return").setValue("Return");


        add.addHidden("administrative-continue").setValue(knot.getId());
    }

}
