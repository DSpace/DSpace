/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.group;

import java.sql.SQLException;
import java.util.UUID;

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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * Manage groups page is the entry point for group management. From here the user
 * may browse/search a the list of groups, they may also add new groups or select
 * existing groups to edit or delete.
 * 
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageGroupsMain
        extends AbstractDSpaceTransformer
{

    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    private static final Message T_group_trail =
            message("xmlui.administrative.group.general.group_trail");
    private static final Message T_title =
            message("xmlui.administrative.group.ManageGroupsMain.title");
    private static final Message T_main_head =
            message("xmlui.administrative.group.ManageGroupsMain.main_head");
    private static final Message T_actions_head =
            message("xmlui.administrative.group.ManageGroupsMain.actions_head");
    private static final Message T_actions_create =
            message("xmlui.administrative.group.ManageGroupsMain.actions_create");
    private static final Message T_actions_create_link =
            message(
            "xmlui.administrative.group.ManageGroupsMain.actions_create_link");
    private static final Message T_actions_browse =
            message("xmlui.administrative.group.ManageGroupsMain.actions_browse");
    private static final Message T_actions_browse_link =
            message(
            "xmlui.administrative.group.ManageGroupsMain.actions_browse_link");
    private static final Message T_actions_search =
            message("xmlui.administrative.group.ManageGroupsMain.actions_search");
    private static final Message T_search_help =
            message("xmlui.administrative.group.ManageGroupsMain.search_help");
    private static final Message T_go =
            message("xmlui.general.go");
    private static final Message T_search_head =
            message("xmlui.administrative.group.ManageGroupsMain.search_head");
    private static final Message T_search_column1 =
            message("xmlui.administrative.group.ManageGroupsMain.search_column1");
    private static final Message T_search_column2 =
            message("xmlui.administrative.group.ManageGroupsMain.search_column2");
    private static final Message T_search_column3 =
            message("xmlui.administrative.group.ManageGroupsMain.search_column3");
    private static final Message T_search_column4 =
            message("xmlui.administrative.group.ManageGroupsMain.search_column4");
    private static final Message T_search_column5 =
            message("xmlui.administrative.group.ManageGroupsMain.search_column5");
    private static final Message T_collection_link =
            message(
            "xmlui.administrative.group.ManageGroupsMain.collection_link");
    private static final Message T_submit_delete =
            message("xmlui.administrative.group.ManageGroupsMain.submit_delete");
    private static final Message T_no_results =
            message("xmlui.administrative.group.ManageGroupsMain.no_results");
    /** The number of results to show on one page. */
    private static final int PAGE_SIZE = 15;
    /** The maximum size of a collection or community name allowed */
    private static final int MAX_COLLECTION_OR_COMMUNITY_NAME = 30;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
   	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    public void addPageMeta(PageMeta pageMeta)
            throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null, T_group_trail);
    }

    public void addBody(Body body)
            throws WingException, SQLException
    {
        // Get all our parameters
        String baseURL = contextPath + "/admin/groups?administrative-continue="
                + knot.getId();
        String query = decodeFromURL(parameters.getParameter("query", ""));
        int page = parameters.getParameterAsInteger("page", 0);
        String highlightID = parameters.getParameter("highlightID", null);
        int resultCount = groupService.searchResultCount(context, query);
        java.util.List<Group> groups = groupService.search(context, query, page * PAGE_SIZE,
                PAGE_SIZE);



        // DIVISION: groups-main
        Division main = body.addInteractiveDivision("groups-main", contextPath
                + "/admin/groups", Division.METHOD_POST,
                "primary administrative groups");
        main.setHead(T_main_head);




        // DIVISION: group-actions
        Division actions = main.addDivision("group-actions");
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

        // DIVISION: group-search
        Division search = main.addDivision("group-search");
        search.setHead(T_search_head);


        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page * PAGE_SIZE + 1;
            int lastIndex = page * PAGE_SIZE + groups.size();

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


        Table table = search.addTable("groups-search-table", groups.size() + 1,
                1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent(T_search_column5);

        for (Group group : groups)
        {
            Row row;
            if (group.getID().toString().equals(highlightID))
            {
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            if (group.getID() != null)
            {
                CheckBox select = row.addCell().addCheckBox("select_group");
                select.setLabel(group.getID().toString());
                select.addOption(group.getID().toString());
            }
            else
            {
                // Don't allow the user to remove the administrative (id:1) or 
                // anonymous group (id:0) 
                row.addCell();
            }

            row.addCell().addContent(group.getID().toString());
            row.addCell().addXref(baseURL + "&submit_edit&groupID="
                    + group.getID(), group.getName());

            int memberCount = group.getMembers().size()
                    + group.getMemberGroups().size();
            row.addCell().addContent(memberCount == 0 ? "-" : String.valueOf(
                    memberCount));

            Cell cell = row.addCell();
            String groupName = group.getName();
            DSpaceObject collectionOrCommunity = null;
            String collectionOrCommunityName = null;
            UUID id;
            id = FlowGroupUtils.getCollectionId(context, groupName);
            if (id != null)
            {
                Collection collection = collectionService.find(context, id);
                if (collection != null)
                {
                    collectionOrCommunityName = collection.getName();
                    collectionOrCommunity = collection;
                }
            }
            else
            {
                id = FlowGroupUtils.getCommunityId(context, groupName);
                if (id != null)
                {
                    Community community = communityService.find(context, id);
                    if (community != null)
                    {
                        collectionOrCommunityName = community.getName();
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

        if (groups.size() <= 0)
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
