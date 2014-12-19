/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.concept;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;

/**
 * The manage concept page is the starting point page for managing
 * concept. From here the user is able to browse or search for concept,
 * once identified the user can selected them for deletion by selecting 
 * the checkboxes and clicking delete or click their name to edit the 
 * concept.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageConceptMain extends AbstractDSpaceTransformer
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ManageConceptMain.class);

    /** Language Strings */
    private static final Message T_title =
            message("xmlui.aspect.authority.concept.ManageConceptMain.title");

    private static final Message T_concept_trail =
            message("xmlui.aspect.authority.concept.general.concept_trail");

    private static final Message T_main_head =
            message("xmlui.aspect.authority.concept.ManageConceptMain.main_head");

    private static final Message T_actions_head =
            message("xmlui.aspect.authority.concept.ManageConceptMain.actions_head");

    private static final Message T_actions_create =
            message("xmlui.aspect.authority.concept.ManageConceptMain.actions_create");

    private static final Message T_actions_create_link =
            message("xmlui.aspect.authority.concept.ManageConceptMain.actions_create_link");

    private static final Message T_actions_browse =
            message("xmlui.aspect.authority.concept.ManageConceptMain.actions_browse");

    private static final Message T_actions_browse_link =
            message("xmlui.aspect.authority.concept.ManageConceptMain.actions_browse_link");

    private static final Message T_actions_search =
            message("xmlui.aspect.authority.concept.ManageConceptMain.actions_search");

    private static final Message T_search_help =
            message("xmlui.aspect.authority.concept.ManageConceptMain.search_help");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_go =
            message("xmlui.general.go");

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

    private static final Message T_administrative_authority 	= message("xmlui.administrative.Navigation.administrative_authority_control");
    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 15;


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        String schemeId = parameters.getParameter("scheme",null);
        Scheme scheme = null;
        if(schemeId!=null)
        {
            try{
                if(context==null) {
                    context = ContextUtil.obtainContext(objectModel);
                }
                scheme = Scheme.find(context, Integer.parseInt(schemeId));
            }
            catch (Exception e)
            {

            }
        }
        if(scheme!=null)
        {
            pageMeta.addTrailLink("scheme/"+scheme.getID(),"scheme/"+scheme.getID());
        }
        pageMeta.addTrailLink(null,T_concept_trail);
    }


    public void addBody(Body body) throws WingException, SQLException
    {
        /* Get and setup our parameters */
        int page          = parameters.getParameterAsInteger("page",0);
        int highlightID   = parameters.getParameterAsInteger("highlightID",-1);
        String query_1      = parameters.getParameter("query", null);
        String query = "";
        try{
            query= URLDecoder.decode(query_1, Constants.DEFAULT_ENCODING);
        }catch (UnsupportedEncodingException e)
        {
            log.error("decode error:"+e);
        }
        String schemeId = parameters.getParameter("scheme",null);
        Scheme scheme = null;
        if(schemeId!=null)
        {
            try{
                if(context==null) {
                    context = ContextUtil.obtainContext(objectModel);
                }
                scheme = Scheme.find(context, Integer.parseInt(schemeId));
            }
            catch (Exception e)
            {

            }
        }
        String baseURL    = contextPath+"/admin/concept?administrative-continue="+knot.getId();
        if(scheme!=null){
            baseURL    = "?administrative-continue="+knot.getId();
        }
        int resultCount=0;
        Concept[] concepts = null;

        if(scheme==null)
        {
            resultCount = Concept.searchResultCount(context, query, null);
            concepts = Concept.search(context, query, page * PAGE_SIZE, PAGE_SIZE, null);

        }
        else
        {
            resultCount = Concept.searchResultCount(context, query, Integer.toString(scheme.getID()));
            concepts = Concept.search(context, query, page * PAGE_SIZE, PAGE_SIZE, Integer.toString(scheme.getID()));
        }

        // DIVISION: concept-main
        Division main = null;
        String mainURL =  "/admin/concept";
        String conceptURL = baseURL;
        if(scheme!=null)
        {
            mainURL = "/admin/scheme?administrative-continue="+knot.getId()+"&schemeID=1"+scheme.getID();
            conceptURL = "/admin/scheme?administrative-continue="+knot.getId()+"&schemeID=1"+scheme.getID();
        }

        main = body.addInteractiveDivision("concept-main", contextPath
                + mainURL , Division.METHOD_POST,
                "primary administrative concept");


        if(scheme!=null)
        {

            main.setHead("Concept for Metadata Scheme : "+scheme.getID());

        }
        else
        {
            main.setHead(T_main_head);
        }
        // DIVISION: concept-actions
        Division actions = main.addDivision("concept-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel("Create New Metadata Scheme");
        actionsList.addItemXref(conceptURL+"&submit_add", T_actions_create_link);


        actionsList.addLabel("Browse All Metadata Scheme");
        actionsList.addItemXref(conceptURL+"&query&submit_search",
                T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");
        // queryField.setAutofocus("autofocus");
        if (query != null)
        {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: concept-search
        Division search = main.addDivision("concept-search");
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
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent("Actions");
        CheckBox selectConcept;
        for (Concept concept : concepts)
        {
            String conceptID = String.valueOf(concept.getID());
            String url = "";
            if(scheme!=null){
                url = conceptURL+"/"+conceptID;
            }
            else
            {
                url = "/concept/"+conceptID;
            }

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

            selectConcept = row.addCell().addCheckBox("select_concepts");
            selectConcept.setLabel(conceptID);
            selectConcept.addOption(conceptID);
//            if (deleteConstraints != null && deleteConstraints.size() > 0)
//            {
//                selectConcept.setDisabled();
//            }

            row.addCellContent(conceptID);
            row.addCell().addXref("/concept/"+concept.getID(), concept.getCreated().toString());
            row.addCell().addXref("/concept/"+concept.getID(), concept.getLabel());
            row.addCell().addXref(conceptURL+"&submit_edit_concept&conceptID="+concept.getID(),"edit");
        }

        if (concepts.length <= 0)
        {
            Cell cell = table.addRow().addCell(1, 4);
            cell.addHighlight("italic").addContent(T_no_results);
        }
        else
        {
            search.addPara().addButton("submit_delete").setValue(T_submit_delete);
        }
        main.addDivision("return").addPara().addButton("submit_cancel").setValue("Return");
        main.addHidden("administrative-continue").setValue(knot.getId());

    }


    public void addOptions(org.dspace.app.xmlui.wing.element.Options options) throws org.xml.sax.SAXException, org.dspace.app.xmlui.wing.WingException, org.dspace.app.xmlui.utils.UIException, java.sql.SQLException, java.io.IOException, org.dspace.authorize.AuthorizeException
    {
        String schemeId = parameters.getParameter("scheme",null);
        Scheme scheme = null;
        if(schemeId!=null)
        {
            try{
                if(context==null) {
                    context = ContextUtil.obtainContext(objectModel);
                }
                scheme = Scheme.find(context, Integer.parseInt(schemeId));
            }
            catch (Exception e)
            {

            }
        }

        if(scheme==null)
        {
            return;
        }

        options.addList("browse");
        List account = options.addList("account");
        List context = options.addList("context");
        List admin = options.addList("administrative");


        //Check if a system administrator
        boolean isSystemAdmin = AuthorizeManager.isAdmin(this.context);


        // System Administrator options!
        if (isSystemAdmin)
        {

            List authority = admin.addList("authority");
            authority.setHead(T_administrative_authority);
            authority.addItemXref(contextPath+"/scheme/"+scheme.getID(),"Back to scheme");
        }
    }
}
