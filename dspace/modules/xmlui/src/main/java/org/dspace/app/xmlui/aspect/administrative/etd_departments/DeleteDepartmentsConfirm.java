/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.etd_departments;

import java.sql.SQLException;
import java.util.ArrayList;

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

/**
 * Present the user with a list of soon-to-be-deleted Groups.
 * If the user clicks confirm deletion then they will be
 * deleted otherwise they will be spared the wrath of deletion.
 * @author Scott Phillips
 */
public class DeleteDepartmentsConfirm extends AbstractDSpaceTransformer
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	private static final Message T_department_trail =
		message("xmlui.administrative.departments.general.department_trail");
	private static final Message T_title =
		message("xmlui.administrative.departments.DeleteDepartmentsConfirm.title");
	private static final Message T_trail =
		message("xmlui.administrative.departments.DeleteDepartmentsConfirm.trail");
	private static final Message T_head =
		message("xmlui.administrative.departments.DeleteDepartmentsConfirm.head");
	private static final Message T_para =
		message("xmlui.administrative.departments.DeleteDepartmentsConfirm.para");
	private static final Message T_column1 =
		message("xmlui.administrative.departments.DeleteDepartmentsConfirm.column1");
	private static final Message T_column2 =
		message("xmlui.administrative.departments.DeleteDepartmentsConfirm.column2");
	private static final Message T_column3 =
		message("xmlui.administrative.departments.DeleteDepartmentsConfirm.column3");
	private static final Message T_column4 =
		message("xmlui.administrative.departments.DeleteDepartmentsConfirm.column4");
	private static final Message T_submit_confirm =
		message("xmlui.general.delete");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");


	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/departments",T_department_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		String idsString = parameters.getParameter("departmentIDs", null);

		ArrayList<EtdUnit> departments = new ArrayList<EtdUnit>();
		for (String id : idsString.split(","))
		{
			EtdUnit department = EtdUnit.find(context,Integer.valueOf(id));
			departments.add(department);
		}

    	Division deleted = body.addInteractiveDivision("department-confirm-delete",
    			contextPath+"/admin/departments",Division.METHOD_POST,"primary administrative departments");
    	deleted.setHead(T_head);
    	deleted.addPara(T_para);

    	Table table = deleted.addTable("departments-list",departments.size() + 1, 3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
        header.addCell().addContent(T_column4);

    	for (EtdUnit department : departments)
    	{
    		Row row = table.addRow();
    		row.addCell().addContent(department.getID());
        	row.addCell().addContent(department.getName());
        	//row.addCell().addContent(department.getMembers().length);
        	//row.addCell().addContent(department.getMemberGroups().length);
	    }

    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_confirm);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);

    	deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
