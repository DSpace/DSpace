/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */
package edu.umd.lib.dspace.app.xmlui.aspect.administrative.units;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.UnitService;

/**
 * Present the user with a list of soon-to-be-deleted Units. If the user clicks
 * confirm deletion then they will be deleted otherwise they will be spared the
 * wrath of deletion. Similar to DeleteGroupsConfirm
 *
 * @author Scott Phillips
 */
public class DeleteUnitsConfirm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_unit_trail = message("xmlui.administrative.units.general.unit_trail");
    private static final Message T_title = message("xmlui.administrative.units.DeleteUnitsConfirm.title");
    private static final Message T_trail = message("xmlui.administrative.units.DeleteUnitsConfirm.trail");
    private static final Message T_head = message("xmlui.administrative.units.DeleteUnitsConfirm.head");
    private static final Message T_para = message("xmlui.administrative.units.DeleteUnitsConfirm.para");
    private static final Message T_column1 = message("xmlui.administrative.units.DeleteUnitsConfirm.column1");
    private static final Message T_column2 = message("xmlui.administrative.units.DeleteUnitsConfirm.column2");
    private static final Message T_column3 = message("xmlui.administrative.units.DeleteUnitsConfirm.column3");
    private static final Message T_column4 = message("xmlui.administrative.units.DeleteUnitsConfirm.column4");

    private static final Message T_submit_confirm = message("xmlui.general.delete");

    private static final Message T_submit_cancel = message("xmlui.general.cancel");

    private static UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/units", T_unit_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

    @Override
    public void addBody(Body body) throws WingException, SQLException,
    AuthorizeException
    {
        String idsString = parameters.getParameter("unitIDs", null);

        ArrayList<Unit> units = new ArrayList<Unit>();
        for (String id : idsString.split(","))
        {
            Unit unit = unitService.find(context, UUID.fromString(id));
            units.add(unit);
        }

        Division deleted = body.addInteractiveDivision("unit-confirm-delete",
                contextPath + "/admin/groups", Division.METHOD_POST,
                "primary administrative units");
        deleted.setHead(T_head);
        deleted.addPara(T_para);

        Table table = deleted.addTable("groups-list", units.size() + 1, 3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
        header.addCell().addContent(T_column4);

        for (Unit unit : units)
        {
            Row row = table.addRow();
            row.addCell().addContent(unit.getID().toString());
            row.addCell().addContent(unit.getName());
            row.addCell().addContent(unit.getGroups().size());
            // row.addCell().addContent(unit.getMemberGroups().length);
        }

        Para buttons = deleted.addPara();
        buttons.addButton("submit_confirm").setValue(T_submit_confirm);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
