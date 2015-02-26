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
 * Manage departments page is the entry point for department management. From here the user
 * may browse/search a the list of departments, they may also add new departments or select
 * existing departments to edit or delete.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageDepartmentsMain extends AbstractDSpaceTransformer
{

    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    private static final Message T_departments_trail =
            message("xmlui.administrative.departments.general.department_trail");
    private static final Message T_title =
            message("xmlui.administrative.departments.ManageDepartmentsMain.title");
    private static final Message T_main_head =
            message("xmlui.administrative.departments.ManageDepartmentsMain.main_head");
    private static final Message T_actions_head =
            message("xmlui.administrative.departments.ManageDepartmentsMain.actions_head");
    private static final Message T_actions_create =
            message("xmlui.administrative.departments.ManageDepartmentsMain.actions_create");
    private static final Message T_actions_create_link =
            message(
            "xmlui.administrative.departments.ManageDepartmentsMain.actions_create_link");
    private static final Message T_actions_browse =
            message("xmlui.administrative.departments.ManageDepartmentsMain.actions_browse");
    private static final Message T_actions_browse_link =
            message(
            "xmlui.administrative.departments.ManageDepartmentsMain.actions_browse_link");
    private static final Message T_actions_search =
            message("xmlui.administrative.departments.ManageDepartmentsMain.actions_search");
    private static final Message T_search_help =
            message("xmlui.administrative.departments.ManageDepartmentsMain.search_help");
    private static final Message T_go =
            message("xmlui.general.go");
    private static final Message T_search_head =
            message("xmlui.administrative.departments.ManageDepartmentsMain.search_head");
    private static final Message T_search_column1 =
            message("xmlui.administrative.departments.ManageDepartmentsMain.search_column1");
    private static final Message T_search_column2 =
            message("xmlui.administrative.departments.ManageDepartmentsMain.search_column2");
    private static final Message T_search_column3 =
            message("xmlui.administrative.departments.ManageDepartmentsMain.search_column3");
    private static final Message T_search_column4 =
            message("xmlui.administrative.departments.ManageDepartmentsMain.search_column4");
    private static final Message T_search_column5 =
            message("xmlui.administrative.departments.ManageDepartmentsMain.search_column5");
    private static final Message T_collection_link =
            message(
            "xmlui.administrative.departments.ManageDepartmentsMain.collection_link");
    private static final Message T_submit_delete =
            message("xmlui.administrative.departments.ManageDepartmentsMain.submit_delete");
    private static final Message T_no_results =
            message("xmlui.administrative.departments.ManageDepartmentsMain.no_results");
    /** The number of results to show on one page. */
    private static final int PAGE_SIZE = 15;
    /** The maximum size of a collection or community name allowed */
    private static final int MAX_COLLECTION_OR_COMMUNITY_NAME = 30;

    public void addPageMeta(PageMeta pageMeta)
            throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null, T_departments_trail);
    }

    public void addBody(Body body)
            throws WingException, SQLException
    {
        // Get all our parameters
        String baseURL = contextPath + "/admin/departments?administrative-continue="
                + knot.getId();
        String query = decodeFromURL(parameters.getParameter("query", ""));
        int page = parameters.getParameterAsInteger("page", 0);
        int highlightID = parameters.getParameterAsInteger("highlightID", -1);
        int resultCount = EtdUnit.searchResultCount(context, query);
        EtdUnit[] departments = EtdUnit.search(context, query, page * PAGE_SIZE,
                PAGE_SIZE);



        // DIVISION: departments-main
        Division main = body.addInteractiveDivision("departments-main", contextPath
                + "/admin/departments", Division.METHOD_POST,
                "primary administrative departments");
        main.setHead(T_main_head);




        // DIVISION: department-actions
        Division actions = main.addDivision("department-actions");
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

        // DIVISION: department-search
        Division search = main.addDivision("department-search");
        search.setHead(T_search_head);


        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page * PAGE_SIZE + 1;
            int lastIndex = page * PAGE_SIZE + departments.length;

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


        Table table = search.addTable("departments-search-table", departments.length + 1,
                1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent(T_search_column5);

        for (EtdUnit department : departments)
        {
            Row row;
            if (department.getID() == highlightID)
            {
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            if (department.getID() > 1)
            {
                CheckBox select = row.addCell().addCheckBox("select_department");
                select.setLabel(Integer.valueOf(department.getID()).toString());
                select.addOption(Integer.valueOf(department.getID()).toString());
            }
            else
            {
                // Don't allow the user to remove the administrative (id:1) or
                // anonymous department (id:0)
                row.addCell();
            }

            row.addCell().addContent(department.getID());
            row.addCell().addXref(baseURL + "&submit_edit&departmentID="
                    + department.getID(), department.getName());

            //int memberCount = department.getMembers().length
            //        + department.getMemberGroups().length;
            //row.addCell().addContent(memberCount == 0 ? "-" : String.valueOf(
            //        memberCount));

            Cell cell = row.addCell();
            String departmentName = department.getName();
            DSpaceObject collectionOrCommunity = null;
            String collectionOrCommunityName = null;
            int id;
            id = FlowGroupUtils.getCollectionId(departmentName);
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
                id = FlowGroupUtils.getCommunityId(departmentName);
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

        if (departments.length <= 0)
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
