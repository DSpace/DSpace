/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.term;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
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
import org.dspace.content.authority.Term;

/**
 * The manage term page is the starting point page for managing
 * term. From here the user is able to browse or search for term,
 * once identified the user can selected them for deletion by selecting 
 * the checkboxes and clicking delete or click their name to edit the 
 * term.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageTermMain extends AbstractDSpaceTransformer
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ManageTermMain.class);

    /** Language Strings */
    private static final Message T_title =
            message("xmlui.aspect.authority.term.ManageTermMain.title");

    private static final Message T_term_trail =
            message("xmlui.aspect.authority.term.general.term_trail");

    private static final Message T_main_head =
            message("xmlui.aspect.authority.term.ManageTermMain.main_head");

    private static final Message T_actions_head =
            message("xmlui.aspect.authority.term.ManageTermMain.actions_head");

    private static final Message T_actions_create =
            message("xmlui.aspect.authority.term.ManageTermMain.actions_create");

    private static final Message T_actions_create_link =
            message("xmlui.aspect.authority.term.ManageTermMain.actions_create_link");

    private static final Message T_actions_browse =
            message("xmlui.aspect.authority.term.ManageTermMain.actions_browse");

    private static final Message T_actions_browse_link =
            message("xmlui.aspect.authority.term.ManageTermMain.actions_browse_link");

    private static final Message T_actions_search =
            message("xmlui.aspect.authority.term.ManageTermMain.actions_search");

    private static final Message T_search_help =
            message("xmlui.aspect.authority.term.ManageTermMain.search_help");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_go =
            message("xmlui.general.go");

    private static final Message T_search_head =
            message("xmlui.aspect.authority.term.ManageTermMain.search_head");

    private static final Message T_search_column1 =
            message("xmlui.aspect.authority.term.ManageTermMain.search_column1");

    private static final Message T_search_column2 =
            message("xmlui.aspect.authority.term.ManageTermMain.search_column2");

    private static final Message T_search_column3 =
            message("xmlui.aspect.authority.term.ManageTermMain.search_column3");

    private static final Message T_search_column4 =
            message("xmlui.aspect.authority.term.ManageTermMain.search_column4");

    private static final Message T_submit_delete =
            message("xmlui.aspect.authority.term.ManageTermMain.submit_delete");

    private static final Message T_no_results =
            message("xmlui.aspect.authority.term.ManageTermMain.no_results");

    private static final Message T_administrative_authority 	= message("xmlui.administrative.Navigation.administrative_authority_control");
    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 15;

    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        String conceptId          = parameters.getParameter("concept",null);
        if(conceptId!=null)
        {
            try{
                Concept concept = Concept.find(context, Integer.parseInt(conceptId));
                if(concept!=null)
                {
                    pageMeta.addTrailLink("/concept/"+concept.getID(),"concept/"+concept.getID());
                }
                pageMeta.addTrailLink(null,T_term_trail);
            }
            catch (Exception e)
            {

            }
        }


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
        String baseURL    = contextPath+"/admin/term?administrative-continue="+knot.getId();

        int resultCount = Term.searchResultCount(context, query, null);
        Term[] terms = Term.search(context, query, page * PAGE_SIZE, PAGE_SIZE, null);

        String conceptId          = parameters.getParameter("conceptId",null);
        String schemeId          = parameters.getParameter("schemeId",null);
        Concept concept = null;
        if(conceptId!=null)
        {
            try{
                concept = Concept.find(context, Integer.parseInt(conceptId));
            }
            catch (Exception e)
            {

            }
        }
        Scheme scheme = null;
        if(schemeId!=null)
        {
            try{
                scheme = Scheme.find(context, Integer.parseInt(schemeId));
            }
            catch (Exception e)
            {

            }
        }
        if(concept!=null){
            resultCount = Term.searchResultCount(context, query, Integer.toString(concept.getID()));
            terms = Term.search(context, query, page * PAGE_SIZE, PAGE_SIZE, Integer.toString(concept.getID()));
        }

        String mainURL = "/admin/term";
        String termURL = baseURL;
        if(concept!=null)
        {
            mainURL = "/admin/concept";
            termURL = "/admin/concept?administrative-continue="+knot.getId();
        }
        if(scheme!=null)
        {
            mainURL = "/admin/scheme";
            termURL = "/admin/scheme?administrative-continue="+knot.getId();
        }
        // DIVISION: term-main
        Division main = body.addInteractiveDivision("term-main", contextPath
                + mainURL, Division.METHOD_POST,
                "primary administrative term");
        main.setHead(T_main_head);

        // DIVISION: term-actions
        Division actions = main.addDivision("term-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(termURL + "&submit_add", T_actions_create_link);
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(termURL+"&query&submit_search",
                T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");
        //queryField.setAutofocus("autofocus");
        if (query != null)
        {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: term-search
        Division search = main.addDivision("term-search");
        search.setHead(T_search_head);

        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page*PAGE_SIZE+1;
            int lastIndex = page*PAGE_SIZE + terms.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / PAGE_SIZE))
            {
                nextURL = termURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = termURL + "&page=" + (page - 1);
            }

            search.setSimplePagination(resultCount,firstIndex,lastIndex,prevURL, nextURL);
        }

        Table table = search.addTable("term-search-table", terms.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent("Action");

        CheckBox selectMTerm;
        for (Term term : terms)
        {
            String termID = String.valueOf(term.getID());
            String url = termURL+"&submit_edit&termID="+termID;
            //java.util.List<String> deleteConstraints = term.getDeleteConstraints();

            Row row;
            if (term.getID() == highlightID)
            {
                // This is a highlighted term
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            selectMTerm = row.addCell().addCheckBox("select_terms");
            selectMTerm.setLabel(termID);
            selectMTerm.addOption(termID);
//            if (deleteConstraints != null && deleteConstraints.size() > 0)
//            {
//                selectEPerson.setDisabled();
//            }

            row.addCellContent(termID);
            row.addCell().addXref("/term/"+term.getID(), term.getCreated().toString());
            row.addCell().addXref("/term/"+term.getID(), term.getLiteralForm());
            row.addCell().addXref(url,"Edit");
        }

        if (terms.length <= 0)
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

        String conceptId          = parameters.getParameter("concept",null);
        Concept concept = null;
        if(conceptId!=null)
        {
            try{
                concept = Concept.find(context, Integer.parseInt(conceptId));
            }
            catch (Exception e)
            {

            }
        }

        if(concept==null)
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
            authority.addItemXref(contextPath+"/concept/"+concept.getID(),"Back to Concept");
        }
    }


}
