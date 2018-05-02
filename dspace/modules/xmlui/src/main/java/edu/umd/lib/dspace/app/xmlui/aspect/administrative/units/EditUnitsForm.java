/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */

package edu.umd.lib.dspace.app.xmlui.aspect.administrative.units;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.UnitService;

import edu.umd.lib.dspace.app.xmlui.aspect.administrative.FlowUnitsUtils;

/**
 * Present the user with the unit's current state. The user may select to change
 * the unit's name, OR search for new units to add, OR select current unit
 * members for removal.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class EditUnitsForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_unit_trail = message("xmlui.administrative.units.general.unit_trail");

    private static final Message T_title = message("xmlui.administrative.units.EditUnitsForm.title");

    private static final Message T_trail = message("xmlui.administrative.units.EditUnitsForm.trail");

    private static final Message T_main_head = message("xmlui.administrative.units.EditUnitsForm.main_head");

    private static final Message T_label_name = message("xmlui.administrative.units.EditUnitsForm.label_name");

    private static final Message T_label_search = message("xmlui.administrative.units.EditUnitsForm.label_search");

    // private static final Message T_submit_search_people =
    // message("xmlui.administrative.units.EditUnitsForm.submit_search_people");

    private static final Message T_submit_search_groups = message("xmlui.administrative.units.EditUnitsForm.submit_search_groups");

    private static final Message T_no_results = message("xmlui.administrative.units.EditUnitsForm.no_results");

    private static final Message T_main_head_new = message("xmlui.administrative.units.EditUnitsForm.main_head_new");

    private static final Message T_submit_clear = message("xmlui.administrative.units.EditUnitsForm.submit_clear");

    private static final Message T_submit_save = message("xmlui.general.save");

    private static final Message T_submit_cancel = message("xmlui.general.cancel");

    private static final Message T_member = message("xmlui.administrative.units.EditUnitsForm.member");

    private static final Message T_pending = message("xmlui.administrative.units.EditUnitsForm.pending");

    private static final Message T_pending_warn = message("xmlui.administrative.units.EditUnitsForm.pending_warn");

    private static final Message T_submit_add = message("xmlui.administrative.units.EditUnitsForm.submit_add");

    private static final Message T_submit_remove = message("xmlui.administrative.units.EditUnitsForm.submit_remove");

    // Group Search
    private static final Message T_groups_column1 = message("xmlui.administrative.units.EditUnitsForm.units_column1");

    private static final Message T_groups_column2 = message("xmlui.administrative.units.EditUnitsForm.units_column2");

    // Members
    private static final Message T_members_head = message("xmlui.administrative.units.EditUnitsForm.members_head");

    private static final Message T_members_column1 = message("xmlui.administrative.units.EditUnitsForm.members_column1");

    private static final Message T_members_column2 = message("xmlui.administrative.units.EditUnitsForm.members_column2");

    private static final Message T_members_pending = message("xmlui.administrative.units.EditUnitsForm.members_pending");

    private static final Message T_members_none = message("xmlui.administrative.units.EditUnitsForm.members_none");

    private static final Message T_faculty_only = message("xmlui.administrative.units.EditUnitsForm.faculty_only");

    // How many results to show on a page.
    private static final int RESULTS_PER_PAGE = 5;

    /** The maximum size of a collection name allowed */
    private static final int MAX_COLLECTION_NAME = 25;

    private static UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

    private static GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/units", T_unit_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

    @Override
    public void addBody(Body body) throws WingException, SQLException
    {
        // Find the unit in question
        String unitID = parameters.getParameter("unitID", null);
        String currentName = decodeFromURL(parameters.getParameter("unitName",
                null));
        if (currentName == null || currentName.length() == 0)
        {
            currentName = FlowUnitsUtils.getName(context, UUID.fromString(unitID));
        }

        Unit unit = null;
        if (unitID != null && !unitID.isEmpty())
        {
            unit = unitService.find(context, UUID.fromString(unitID));

        }
        boolean facultyOnlyValue = true;
        if (unit != null)
        {
            facultyOnlyValue = unit.getFacultyOnly();
        }
        // Get list of member groups from url
        List<UUID> memberGroupIDs = new ArrayList<UUID>();
        String memberGroupIDsString = parameters.getParameter("memberGroupIDs",
                null);
        if (memberGroupIDsString != null)
        {
            for (String id : memberGroupIDsString.split(","))
            {
                if (id.length() > 0)
                {
                    memberGroupIDs.add(UUID.fromString(id));
                }
            }
        }

        String highlightGroupID = parameters.getParameter("highlightGroupID", null);

        // Get search parameters
        String query = decodeFromURL(parameters.getParameter("query", null));
        int page = parameters.getParameterAsInteger("page", 0);

        // Get any errors
        String errorString = parameters.getParameter("errors", null);
        List<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

        // DIVISION: unit-edit
        Division main = body.addInteractiveDivision("unit-edit", contextPath
                + "/admin/units", Division.METHOD_POST,
                "primary administrative units");
        if (unit == null)
        {
            main.setHead(T_main_head_new);
        }
        else
        {
            main.setHead(T_main_head.parameterize(unit.getName(), unitID));
        }

        // DIVISION: unit-actions
        Division actions = main.addDivision("unit-edit-actions");
        Para unitName = actions.addPara();
        unitName.addContent(T_label_name);
        Text unitText = unitName.addText("unit_name");
        unitText.setValue(currentName);

        if (errors.contains("unit_name")
                || errors.contains("unit_name_duplicate"))
        {
            unitText.addError("");
        }
        org.dspace.app.xmlui.wing.element.List facultyOnlyList = actions
                .addList("form",
                        org.dspace.app.xmlui.wing.element.List.TYPE_FORM);
        CheckBox facultyOnlyField = facultyOnlyList.addItem().addCheckBox(
                "faculty_only");
        facultyOnlyField.setLabel(T_faculty_only);
        facultyOnlyField.addOption(facultyOnlyValue, "true");

        Para searchBoxes = actions.addPara();
        searchBoxes.addContent(T_label_search);
        Text queryField = searchBoxes.addText("query");
        queryField.setValue(query);
        queryField.setSize(MAX_COLLECTION_NAME);
        searchBoxes.addButton("submit_search_group").setValue(
                T_submit_search_groups);

        if (query != null)
        {
            searchBoxes.addButton("submit_clear").setValue(T_submit_clear);
            addGroupsSearch(main, query, page, unit, memberGroupIDs);
        }

        boolean changes = false;
        if (unit != null)
        {
            changes = addMemberList(main, unit, memberGroupIDs,
                    highlightGroupID);
        }

        Para buttons = main.addPara();
        buttons.addButton("submit_save").setValue(T_submit_save);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        if (changes)
        {
            main.addPara().addHighlight("warn").addContent(T_pending_warn);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());
    }

    /**
     * Search for groups to add to this unit.
     */
    private void addGroupsSearch(Division div, String query, int page,
            Unit unit, List<UUID> memberGroupIDs) throws SQLException,
            WingException
    {
        List<Group> groups = groupService.search(context, query, page * RESULTS_PER_PAGE,
                RESULTS_PER_PAGE);
        int resultCount = groupService.searchResultCount(context, query);

        Division results = div.addDivision("results");

        if (resultCount > RESULTS_PER_PAGE)
        {
            // If there are enough results then paginate the results
            String baseURL = contextPath
                    + "/admin/units?administrative-continue=" + knot.getId();
            int firstIndex = page * RESULTS_PER_PAGE + 1;
            int lastIndex = page * RESULTS_PER_PAGE + groups.size();

            String nextURL = null, prevURL = null;
            if (page < (resultCount / RESULTS_PER_PAGE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            results.setSimplePagination(resultCount, firstIndex, lastIndex,
                    prevURL, nextURL);
        }

        /* Set up a table with search results (if there are any). */
        Table table = results.addTable("group-edit-search-groups",
                groups.size() + 1, 2);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_groups_column1);
        header.addCell().addContent(T_groups_column2);

        for (Group group : groups)
        {
            String groupID = String.valueOf(group.getID());
            String name = group.getName();

            Row groupData = table.addRow();

            groupData.addCell().addContent(groupID);
            groupData.addCell().addContent(name);

            // check if they are already a member of the group
            if (memberGroupIDs.contains(group.getID()))
            {
                // Check if they really members or just pending members
                if (unit != null && unit.isMember(group))
                {
                    groupData.addCellContent(T_member);
                }
                else
                {
                    groupData.addCell().addHighlight("warn")
                    .addContent(T_pending);
                }
            }
            else
            {
                groupData.addCell().addButton("submit_add_group_" + groupID)
                .setValue(T_submit_add);
            }
        }

        if (groups.size() <= 0)
        {
            table.addRow().addCell(1, 2).addContent(T_no_results);
        }
    }

    /**
     * Add a table with all the current unit's member groups to the specified
     * division.
     *
     * @throws SQLException
     */

    private boolean addMemberList(Division div, Unit parent,
            List<UUID> memberGroupIDs, String highlightGroupID)
            throws WingException, SQLException
    {
        // Flag to remember if there are any pending changes.
        boolean changes = false;

        Division members = div.addDivision("unit-edit-members");
        members.setHead(T_members_head);

        Table table = members.addTable("unit-edit-members-table",
                memberGroupIDs.size() + 1, 2);

        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_members_column1);
        header.addCell().addContent(T_members_column2);

        // get all member groups, pend or actual
        // the cast is correct
        List<UUID> allMemberGroupIDs = new ArrayList<UUID>(memberGroupIDs);

        for (Group group : parent.getGroups())
        {
            if (!allMemberGroupIDs.contains(group.getID()))
            {
                allMemberGroupIDs.add(group.getID());
            }
        }
        // Sort them to a consistent ordering
        Collections.sort(allMemberGroupIDs);

        for (UUID groupID : allMemberGroupIDs)
        {
            Group group = groupService.find(context, groupID);
            boolean highlight = (group.getID().toString().equals(highlightGroupID));
            boolean pendingAddition = !parent.isMember(group);
            boolean pendingRemoval = !memberGroupIDs.contains(groupID);
            addMemberRow(table, group, highlight, pendingAddition,
                    pendingRemoval);

            if (pendingAddition || pendingRemoval)
            {
                changes = true;
            }
        }

        if (allMemberGroupIDs.size() <= 0)
        {
            table.addRow().addCell(1, 2).addContent(T_members_none);
        }

        return changes;
    }

    /**
     * Add a single member row for groups.
     *
     * @param table
     *            The table to add a row too.
     * @param group
     *            The group being displayed
     * @param highlight
     *            Should this group be highlighted?
     * @param pendingAddition
     *            Is this group pending addition?
     * @param pendingRemoval
     *            Is this group pending removal?
     */
    private void addMemberRow(Table table, Group group, boolean highlight,
            boolean pendingAddition, boolean pendingRemoval)
                    throws WingException, SQLException
    {
        String fullName = group.getName();

        Row groupData = table
                .addRow(null, null, highlight ? "highlight" : null);

        groupData.addCell().addContent(group.getID().toString());

        Cell nameCell = groupData.addCell();
        nameCell.addContent(fullName);
        if (pendingAddition)
        {
            nameCell.addContent(" ");
            nameCell.addHighlight("warn").addContent(T_members_pending);
        }

        if (pendingRemoval)
        {
            groupData.addCell().addHighlight("warn").addContent(T_pending);
        }
        else
        {
            groupData.addCell()
            .addButton("submit_remove_group_" + group.getID())
            .setValue(T_submit_remove);
        }
    }
}
