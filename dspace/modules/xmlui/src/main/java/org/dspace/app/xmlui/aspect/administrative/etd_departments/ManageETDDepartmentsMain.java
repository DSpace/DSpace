/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.etd_departments;

import java.sql.SQLException;

import org.dspace.app.xmlui.aspect.administrative.FlowGroupUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.EtdUnit;

/**
 * Manage etd_departments page is the entry point for etd_department management. From here the user
 * may browse/search a the list of etd_departments, they may also add new etd_departments or select
 * existing etd_departments to edit or delete.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageETDDepartmentsMain extends AbstractDSpaceTransformer
{

    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    private static final Message T_etd_departments_trail =
            message("xmlui.administrative.etd_departments.general.etd_department_trail");
    private static final Message T_title =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.title");
    private static final Message T_main_head =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.main_head");
    private static final Message T_actions_head =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.actions_head");
    private static final Message T_actions_create =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.actions_create");
    private static final Message T_actions_create_link =
            message(
            "xmlui.administrative.etd_departments.ManageETDDepartmentsMain.actions_create_link");
    private static final Message T_actions_browse =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.actions_browse");
    private static final Message T_actions_browse_link =
            message(
            "xmlui.administrative.etd_departments.ManageETDDepartmentsMain.actions_browse_link");
    private static final Message T_actions_search =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.actions_search");
    private static final Message T_search_help =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.search_help");
    private static final Message T_go =
            message("xmlui.general.go");
    private static final Message T_search_head =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.search_head");
    private static final Message T_search_column1 =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.search_column1");
    private static final Message T_search_column2 =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.search_column2");
    private static final Message T_search_column3 =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.search_column3");
    private static final Message T_search_column4 =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.search_column4");
    private static final Message T_search_column5 =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.search_column5");
    private static final Message T_collection_link =
            message(
            "xmlui.administrative.etd_departments.ManageETDDepartmentsMain.collection_link");
    private static final Message T_submit_delete =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.submit_delete");
    private static final Message T_no_results =
            message("xmlui.administrative.etd_departments.ManageETDDepartmentsMain.no_results");
    /** The number of results to show on one page. */
    private static final int PAGE_SIZE = 15;
    /** The maximum size of a collection or community name allowed */
    private static final int MAX_COLLECTION_OR_COMMUNITY_NAME = 30;

    public void addPageMeta(PageMeta pageMeta)
            throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null, T_etd_departments_trail);
    }

    public void addBody(Body body)
            throws WingException, SQLException
    {
        // Get all our parameters
        String baseURL = contextPath + "/admin/etd_departments?administrative-continue="
                + knot.getId();
        String query = decodeFromURL(parameters.getParameter("query", ""));
        int page = parameters.getParameterAsInteger("page", 0);
        int highlightID = parameters.getParameterAsInteger("highlightID", -1);
        int resultCount = EtdUnit.searchResultCount(context, query);
        EtdUnit[] etd_departments = EtdUnit.search(context, query, page * PAGE_SIZE,
                PAGE_SIZE);



        // DIVISION: etd_departments-main
        Division main = body.addInteractiveDivision("etd_departments-main", contextPath
                + "/admin/etd_departments", Division.METHOD_POST,
                "primary administrative etd_departments");
        main.setHead(T_main_head);




        // DIVISION: etd_department-actions
        Division actions = main.addDivision("etd_department-actions");
        actions.setHead(T_actions_head);

        // Browse Epeople
        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(baseURL + "&submit_add", T_actions_create_link);
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL + "&query&submit_search",
                T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");
        queryField.setAutofocus("autofocus");
        if (query != null)
        {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: etd_department-search
        Division search = main.addDivision("etd_department-search");
        search.setHead(T_search_head);


        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page * PAGE_SIZE + 1;
            int lastIndex = page * PAGE_SIZE + etd_departments.length;

            String nextURL = null, prevURL = null;
            if (page < (resultCount / PAGE_SIZE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            search.setSimplePagination(resultCount, firstIndex, lastIndex,
                    prevURL, nextURL);
        }


        Table table = search.addTable("etd_departments-search-table", etd_departments.length + 1,
                1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
//        header.addCell().addContent(T_search_column4);
//        header.addCell().addContent(T_search_column5);

        for (EtdUnit etd_department : etd_departments)
        {
            Row row;
            if (etd_department.getID() == highlightID)
            {
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            if (etd_department.getID() >= 1)
            {
                CheckBox select = row.addCell().addCheckBox("select_etd_department");
                select.setLabel(Integer.valueOf(etd_department.getID()).toString());
                select.addOption(Integer.valueOf(etd_department.getID()).toString());
            }
            else
            {
                // Don't allow the user to remove the administrative (id:1) or
                // anonymous etd_department (id:0)
                row.addCell();
            }

            row.addCell().addContent(etd_department.getID());
            row.addCell().addXref(baseURL + "&submit_edit&etd_departmentID="
                    + etd_department.getID(), etd_department.getName());

            //int memberCount = etd_department.getMembers().length
            //        + etd_department.getMemberGroups().length;
            //row.addCell().addContent(memberCount == 0 ? "-" : String.valueOf(
            //        memberCount));

            Cell cell = row.addCell();
            String etd_departmentName = etd_department.getName();
            DSpaceObject collectionOrCommunity = null;
            String collectionOrCommunityName = null;
            int id;
            id = FlowGroupUtils.getCollectionId(etd_departmentName);
            if (id > -1)
            {
                Collection collection = Collection.find(context, id);
                if (collection != null)
                {
                    collectionOrCommunityName = collection.getMetadata("name");
                    collectionOrCommunity = collection;
                }
            }
            else
            {
                id = FlowGroupUtils.getCommunityId(etd_departmentName);
                if (id > -1)
                {
                    Community community = Community.find(context, id);
                    if (community != null)
                    {
                        collectionOrCommunityName = community.getMetadata("name");
                        collectionOrCommunity = community;
                    }
                }
            }
            if (collectionOrCommunity != null)
            {
                if (collectionOrCommunityName == null)
                {
                    collectionOrCommunityName = "";
                }
                else if (collectionOrCommunityName.length()
                        > MAX_COLLECTION_OR_COMMUNITY_NAME)
                {
                    collectionOrCommunityName = collectionOrCommunityName.substring(
                            0, MAX_COLLECTION_OR_COMMUNITY_NAME - 3) + "...";
                }

                cell.addContent(collectionOrCommunityName + " ");

                Highlight highlight = cell.addHighlight("fade");

                highlight.addContent("[");
                highlight.addXref(contextPath + "/handle/"
                        + collectionOrCommunity.getHandle(), T_collection_link);
                highlight.addContent("]");
            }

        }

        if (etd_departments.length <= 0)
        {
            Cell cell = table.addRow().addCell(1, 5);
            cell.addHighlight("italic").addContent(T_no_results);
        }
        else
        {
            search.addPara().addButton("submit_delete").setValue(T_submit_delete);
        }

        search.addHidden("administrative-continue").setValue(knot.getId());
    }
}
