/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */
package edu.umd.lib.dspace.app.xmlui.aspect.administrative.etd_departments;

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
import org.dspace.content.EtdUnit;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EtdUnitService;

/**
 * Present the user with a list of soon-to-be-deleted ETD Departments. If the
 * user clicks confirm deletion then they will be deleted otherwise they will be
 * spared the wrath of deletion.
 *
 * @author Scott Phillips
 */
public class DeleteETDDepartmentsConfirm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_etd_department_trail = message("xmlui.administrative.etd_departments.general.etd_department_trail");
    private static final Message T_title = message("xmlui.administrative.etd_departments.DeleteETDDepartmentsConfirm.title");
    private static final Message T_trail = message("xmlui.administrative.etd_departments.DeleteETDDepartmentsConfirm.trail");
    private static final Message T_head = message("xmlui.administrative.etd_departments.DeleteETDDepartmentsConfirm.head");
    private static final Message T_para = message("xmlui.administrative.etd_departments.DeleteETDDepartmentsConfirm.para");
    private static final Message T_column1 = message("xmlui.administrative.etd_departments.DeleteETDDepartmentsConfirm.column1");
    private static final Message T_column2 = message("xmlui.administrative.etd_departments.DeleteETDDepartmentsConfirm.column2");
    
    private static final Message T_submit_confirm = message("xmlui.general.delete");

    private static final Message T_submit_cancel = message("xmlui.general.cancel");

    private static EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/etd_departments",
                T_etd_department_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

    @Override
    public void addBody(Body body) throws WingException, SQLException,
            AuthorizeException
    {
        String idsString = parameters.getParameter("etd_departmentIDs", null);

        ArrayList<EtdUnit> etd_departments = new ArrayList<EtdUnit>();
        for (String id : idsString.split(","))
        {
            EtdUnit etd_department = etdunitService.find(context, UUID.fromString(id));
            etd_departments.add(etd_department);
        }

        Division deleted = body.addInteractiveDivision(
                "etd_department-confirm-delete", contextPath
                        + "/admin/etd_departments", Division.METHOD_POST,
                "primary administrative etd_departments");
        deleted.setHead(T_head);
        deleted.addPara(T_para);

        Table table = deleted.addTable("etd_departments-list",
                etd_departments.size() + 1, 3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);

        for (EtdUnit etd_department : etd_departments)
        {
            Row row = table.addRow();
            row.addCell().addContent(etd_department.getID().toString());
            row.addCell().addContent(etd_department.getName());
        }

        Para buttons = deleted.addPara();
        buttons.addButton("submit_confirm").setValue(T_submit_confirm);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
