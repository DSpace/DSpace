/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.versioning.Version;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;

import java.sql.SQLException;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class RestoreVersionForm extends AbstractDSpaceTransformer
{
      /** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");

	private static final Message T_title = message("xmlui.aspect.versioning.RestoreVersionForm.title");
	private static final Message T_trail = message("xmlui.aspect.versioning.RestoreVersionForm.trail");
	private static final Message T_head1 = message("xmlui.aspect.versioning.RestoreVersionForm.head1");
	private static final Message T_para1 = message("xmlui.aspect.versioning.RestoreVersionForm.para1");
	private static final Message T_column1 = message("xmlui.aspect.versioning.RestoreVersionForm.column1");
	private static final Message T_column2 = message("xmlui.aspect.versioning.RestoreVersionForm.column2");
	private static final Message T_column3 = message("xmlui.aspect.versioning.RestoreVersionForm.column3");
    private static final Message T_column4 = message("xmlui.aspect.versioning.RestoreVersionForm.column4");

    private static final Message T_submit_restore = message("xmlui.aspect.versioning.RestoreVersionForm.restore");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");

    protected VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();

	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, AuthorizeException, SQLException
    {
    	Division main = createMainDivision(body);
		createTable(main);
        addButtons(main);
		main.addHidden("versioning-continue").setValue(knot.getId());
	}


    private Division createMainDivision(Body body) throws WingException
    {
        Division main = body.addInteractiveDivision("restore-version", contextPath+"/item/versionhistory", Division.METHOD_POST, "restore version");
		main.setHead(T_head1);
		main.addPara(T_para1);
        return main;
    }


    private void createTable(Division main) throws WingException, SQLException
    {
        // Get all our parameters
		String id = parameters.getParameter("versionID", null);

        Table table = main.addTable("version", 1, 1);

		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		header.addCellContent(T_column2);
		header.addCellContent(T_column3);
        header.addCellContent(T_column4);

        Version version = versioningService.getVersion(context, Integer.parseInt(id));

        Row row = table.addRow();
        row.addCell().addContent(version.getVersionNumber());
        row.addCell().addContent(version.getEPerson().getEmail());
        row.addCell().addContent(new DCDate(version.getVersionDate()).toString());
        row.addCell().addContent(version.getSummary());


        List fields = main.addList("fields", List.TYPE_FORM);
        Composite addComposite = fields.addItem().addComposite("summary");
        addComposite.setLabel(T_column4);
        addComposite.addTextArea("summary");
    }

    private void addButtons(Division main) throws WingException
    {
        Para buttons = main.addPara();
		buttons.addButton("submit_restore").setValue(T_submit_restore);
		buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    }
}

