/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataValue;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
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
public class DeleteVersionsConfirm extends AbstractDSpaceTransformer {

      /** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_title = message("xmlui.aspect.versioning.DeleteVersionsConfirm.title");
	private static final Message T_trail = message("xmlui.aspect.versioning.DeleteVersionsConfirm.trail");
	private static final Message T_head1 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.head1");
	private static final Message T_para1 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.para1");
	private static final Message T_para2 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.para2");
	private static final Message T_column1 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column1");
	private static final Message T_column2 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column2");
	private static final Message T_column3 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column3");
    private static final Message T_column4 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column4");
    private static final Message T_column5 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column5");


    private static final Message T_submit_delete = message("xmlui.general.delete");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");

    protected VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

	public void addPageMeta(PageMeta pageMeta) throws WingException {
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		//pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, AuthorizeException, SQLException {
		Division main = createMainDivision(body);

		createTable(main);

        addButtons(main);

		main.addHidden("versioning-continue").setValue(knot.getId());
	}


    private Division createMainDivision(Body body) throws WingException {
        Division main = body.addInteractiveDivision("versions-confirm-delete", contextPath+"/item/versionhistory", Division.METHOD_POST, "delete version");
		main.setHead(T_head1);
        Para helpPara = main.addPara();
        helpPara.addContent(T_para1);
        helpPara.addHighlight("bold").addContent(T_para2);
        return main;
    }


    private void createTable(Division main) throws WingException, SQLException {
        // Get all our parameters
		String idsString = parameters.getParameter("versionIDs", null);

        Table table = main.addTable("versions-confirm-delete", 1, 1);

		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		header.addCellContent(T_column2);
		header.addCellContent(T_column3);
        header.addCellContent(T_column4);
        header.addCellContent(T_column5);


        for (String id : idsString.split(","))
        {
            Version version = null;

            if(StringUtils.isNotBlank(id))
            {
                version = versioningService.getVersion(context, Integer.parseInt(id));
            }

            if(version!=null)
            {
                Row row = table.addRow();
			    row.addCell().addContent(version.getVersionNumber());
                addItemIdentifier(row.addCell(), version.getItem());

                EPerson editor = version.getEPerson();
                row.addCell().addXref("mailto:" + editor.getEmail(), editor.getFullName());
                row.addCell().addContent(new DCDate(version.getVersionDate()).toString());
                row.addCell().addContent(version.getSummary());
            }
		}

    }

    private void addButtons(Division main) throws WingException {
        Para buttons = main.addPara();
		buttons.addButton("submit_confirm").setValue(T_submit_delete);
		buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    }

    private void addItemIdentifier(Cell cell, org.dspace.content.Item item) throws WingException {
        String itemHandle = item.getHandle();

        java.util.List<MetadataValue> identifiers = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "identifier", null, org.dspace.content.Item.ANY);
        String itemIdentifier=null;
        if(identifiers!=null && identifiers.size() > 0)
        {
            itemIdentifier = identifiers.get(0).getValue();
        }

        if(itemIdentifier!=null)
        {
            cell.addXref(contextPath + "/resource/" + itemIdentifier, itemIdentifier);
        }else{
            cell.addXref(contextPath + "/handle/" + itemHandle, itemHandle);
        }
    }
}

