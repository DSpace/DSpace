/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.scheme;

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
import org.dspace.content.authority.Scheme;

/**
 * The manage scheme page is the starting point page for managing
 * scheme. From here the user is able to browse or search for scheme,
 * once identified the user can selected them for deletion by selecting
 * the checkboxes and clicking delete or click their name to edit the
 * scheme.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageSchemeMain extends AbstractDSpaceTransformer
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ManageSchemeMain.class);

    /** Language Strings */
    private static final Message T_title =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.title");

    private static final Message T_scheme_trail =
            message("xmlui.aspect.authority.scheme.general.scheme_trail");

    private static final Message T_main_head =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.main_head");

    private static final Message T_actions_head =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.actions_head");

    private static final Message T_actions_create =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.actions_create");

    private static final Message T_actions_create_link =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.actions_create_link");

    private static final Message T_actions_browse =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.actions_browse");

    private static final Message T_actions_browse_link =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.actions_browse_link");

    private static final Message T_actions_search =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.actions_search");

    private static final Message T_search_help =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_help");

    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_go =
            message("xmlui.general.go");

    private static final Message T_search_head =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_head");

    private static final Message T_search_column1 =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_column1");

    private static final Message T_search_column2 =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_column2");

    private static final Message T_search_column3 =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_column3");

    private static final Message T_search_column4 =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_column4");

    private static final Message T_submit_delete =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.submit_delete");

    private static final Message T_no_results =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.no_results");

    private static final Message T_administrative_term_metadata 		= message("xmlui.administrative.Navigation.administrative_term_registries");
    private static final Message T_administrative_concept_metadata 			= message("xmlui.administrative.Navigation.administrative_concept_registries");
    private static final Message T_administrative_relation_metadata 			= message("xmlui.administrative.Navigation.administrative_relation_registries");
    private static final Message T_administrative_metadata_concept 			= message("xmlui.administrative.Navigation.administrative_metadata_concept");
    private static final Message T_administrative_metadata_term 			= message("xmlui.administrative.Navigation.administrative_metadata_term");
    private static final Message T_administrative_metadata_concept_2_term_relation 			= message("xmlui.administrative.Navigation.administrative_metadata_concept_2_term_relation");


    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 15;


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null,T_scheme_trail);
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
        String baseURL    = contextPath+"/admin/scheme?administrative-continue="+knot.getId();
        int resultCount   = Scheme.searchResultCount(context, query);
        Scheme[] schemes = Scheme.search(context, query, page * PAGE_SIZE, PAGE_SIZE);


        // DIVISION: scheme-main
        Division main = body.addInteractiveDivision("scheme-main", contextPath
                + "/admin/scheme", Division.METHOD_POST,
                "primary administrative scheme");
        main.setHead(T_main_head);

        // DIVISION: scheme-actions
        Division actions = main.addDivision("scheme-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(baseURL+"&submit_add", T_actions_create_link);
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL+"&query&submit_search",
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

        // DIVISION: scheme-search
        Division search = main.addDivision("scheme-search");
        search.setHead(T_search_head);

        // If there are more than 10 results the paginate the division.
        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page*PAGE_SIZE+1;
            int lastIndex = page*PAGE_SIZE + schemes.length;

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

        Table table = search.addTable("scheme-search-table", schemes.length + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        boolean isSystemAdmin = AuthorizeManager.isAdmin(this.context);
        if(isSystemAdmin)
        {
            header.addCell().addContent(T_search_column1);
        }
        header.addCell().addContent("ID");
        header.addCell().addContent("Creat Date");
        header.addCell().addContent("Identifier");

        if(isSystemAdmin)
        {
            header.addCell().addContent("actions");
        }

        CheckBox selectMScheme;
        for (Scheme scheme : schemes)
        {
            String schemeID = String.valueOf(scheme.getID());
            String url = baseURL+"&submit_edit&schemeId="+schemeID;
            //java.util.List<String> deleteConstraints = scheme.getDeleteConstraints();

            Row row;
            if (scheme.getID() == highlightID)
            {
                // This is a highlighted scheme
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }
            if(isSystemAdmin)
            {
                selectMScheme = row.addCell().addCheckBox("select_schemes");


                selectMScheme.setLabel(schemeID);
                selectMScheme.addOption(schemeID);
            }
//            if (deleteConstraints != null && deleteConstraints.size() > 0)
//            {
//                selectEPerson.setDisabled();
//            }

            row.addCellContent(schemeID);
            if(isSystemAdmin){
                row.addCell().addXref("/scheme/"+scheme.getID(), scheme.getCreated().toString());
                row.addCell().addXref("/scheme/"+scheme.getID(), scheme.getIdentifier());
                Cell actionCell = row.addCell() ;
                actionCell.addXref(url, "Edit");
            }
            else
            {
                row.addCell().addContent(scheme.getCreated().toString());
                row.addCell().addContent(scheme.getIdentifier());
            }
        }

        if (schemes.length <= 0)
        {
            Cell cell = table.addRow().addCell(1, 4);
            cell.addHighlight("italic").addContent(T_no_results);
        }
        else
        {
            search.addPara().addButton("submit_delete").setValue(T_submit_delete);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());

    }

    public void addOptions(org.dspace.app.xmlui.wing.element.Options options) throws org.xml.sax.SAXException, org.dspace.app.xmlui.wing.WingException, org.dspace.app.xmlui.utils.UIException, java.sql.SQLException, java.io.IOException, org.dspace.authorize.AuthorizeException {

        /* Create skeleton menu structure to ensure consistent order between aspects,
        * even if they are never used
        */
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
            //authority.addItemXref(contextPath+"/admin/metadata-concept-relation-registry",T_administrative_relation_metadata);
//            authority.addItemXref(contextPath+"/admin/schema//concept",T_administrative_metadata_concept);
//            authority.addItemXref(contextPath+"/admin/term",T_administrative_metadata_term);
            //authority.addItemXref(contextPath+"/admin/metadata-concept-term",T_administrative_metadata_concept_2_term_relation);
        }
    }

}
