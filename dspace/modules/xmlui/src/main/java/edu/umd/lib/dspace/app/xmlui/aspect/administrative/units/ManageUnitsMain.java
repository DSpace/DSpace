/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */

package edu.umd.lib.dspace.app.xmlui.aspect.administrative.units;

import java.sql.SQLException;

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
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.UnitService;

/**
 * Manage units page is the entry point for unit management. From here the user
 * may browse/search a the list of units, they may also add new units or select
 * existing units to edit or delete.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class ManageUnitsMain extends AbstractDSpaceTransformer
{

    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_unit_trail = message("xmlui.administrative.units.general.unit_trail");

    private static final Message T_title = message("xmlui.administrative.units.ManageUnitsMain.title");

    private static final Message T_main_head = message("xmlui.administrative.units.ManageUnitsMain.main_head");

    private static final Message T_actions_head = message("xmlui.administrative.units.ManageUnitsMain.actions_head");

    private static final Message T_actions_create = message("xmlui.administrative.units.ManageUnitsMain.actions_create");

    private static final Message T_actions_create_link = message("xmlui.administrative.units.ManageUnitsMain.actions_create_link");

    private static final Message T_actions_browse = message("xmlui.administrative.units.ManageUnitsMain.actions_browse");

    private static final Message T_actions_browse_link = message("xmlui.administrative.units.ManageUnitsMain.actions_browse_link");

    private static final Message T_actions_search = message("xmlui.administrative.units.ManageUnitsMain.actions_search");

    private static final Message T_search_help = message("xmlui.administrative.units.ManageUnitsMain.search_help");

    private static final Message T_go = message("xmlui.general.go");

    private static final Message T_search_head = message("xmlui.administrative.units.ManageUnitsMain.search_head");

    private static final Message T_search_column1 = message("xmlui.administrative.units.ManageUnitsMain.search_column1");

    private static final Message T_search_column2 = message("xmlui.administrative.units.ManageUnitsMain.search_column2");

    private static final Message T_search_column3 = message("xmlui.administrative.units.ManageUnitsMain.search_column3");

    private static final Message T_search_column4 = message("xmlui.administrative.units.ManageUnitsMain.search_column4");

    private static final Message T_submit_delete = message("xmlui.administrative.units.ManageUnitsMain.submit_delete");

    private static final Message T_no_results = message("xmlui.administrative.units.ManageUnitsMain.no_results");

    /** The number of results to show on one page. */
    private static final int PAGE_SIZE = 15;

    private static UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null, T_unit_trail);
    }

    @Override
    public void addBody(Body body) throws WingException, SQLException
    {
        // Get all our parameters
        String baseURL = contextPath + "/admin/units?administrative-continue="
                + knot.getId();
        String query = decodeFromURL(parameters.getParameter("query", ""));
        int page = parameters.getParameterAsInteger("page", 0);
        String highlightID = parameters.getParameter("highlightID", null);
        int resultCount = unitService.searchResultCount(context, query);
        java.util.List<Unit> units = unitService.search(context, query, page * PAGE_SIZE, PAGE_SIZE);

        // DIVISION: units-main
        Division main = body.addInteractiveDivision("units-main", contextPath
                + "/admin/units", Division.METHOD_POST,
                "primary administrative units");
        main.setHead(T_main_head);

        // DIVISION: units-actions
        Division actions = main.addDivision("units-actions");
        actions.setHead(T_actions_head);

        // Browse Epeople
        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(baseURL + "&submit_add", T_actions_create_link);
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL + "&query&submit_search",
                T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList
                .addItem();
        Text queryField = actionItem.addText("query");
        queryField.setAutofocus("autofocus");
        if (query != null)
        {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: units-search
        Division search = main.addDivision("units-search");
        search.setHead(T_search_head);

        if (resultCount > PAGE_SIZE)
        {
            // If there are enough results then paginate the results
            int firstIndex = page * PAGE_SIZE + 1;
            int lastIndex = page * PAGE_SIZE + units.size();

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

        Table table = search
                .addTable("units-search-table", units.size() + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);

        for (Unit unit : units)
        {
            Row row;
            if (unit.getID().toString().equals(highlightID))
            {
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            if (unit.getID() != null)
            {
                CheckBox select = row.addCell().addCheckBox("select_unit");
                select.setLabel(unit.getName());
                select.addOption(unit.getID().toString());
            }
            else
            {
                // Don't allow the user to remove the administrative (id:1) or
                // anonymous unit (id:0)
                row.addCell();
            }

            row.addCell().addContent(unit.getID().toString());
            row.addCell().addXref(
                    baseURL + "&submit_edit&unitID=" + unit.getID(),
                    unit.getName());

            int memberCount = unit.getGroups().size();
            // + unit.getMemberUnits().length;
            row.addCell().addContent(
                    memberCount == 0 ? "-" : String.valueOf(memberCount));

        }

        if (units.size() <= 0)
        {
            Cell cell = table.addRow().addCell(1, 5);
            cell.addHighlight("italic").addContent(T_no_results);
        }
        else
        {
            search.addPara().addButton("submit_delete")
                    .setValue(T_submit_delete);
        }

        search.addHidden("administrative-continue").setValue(knot.getId());
    }
}
