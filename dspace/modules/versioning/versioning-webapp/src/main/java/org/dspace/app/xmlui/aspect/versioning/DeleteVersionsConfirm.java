package org.dspace.app.xmlui.aspect.versioning;

import java.sql.SQLException;
import java.util.ArrayList;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.utils.DSpace;
import org.dspace.versioning.PluggableVersioningService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersioningService;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Apr 11, 2011
 * Time: 1:35:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class DeleteVersionsConfirm extends AbstractDSpaceTransformer{

      /** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_title = message("xmlui.aspect.versioning.DeleteVersionsConfirm.title");
	private static final Message T_trail = message("xmlui.aspect.versioning.DeleteVersionsConfirm.trail");
	private static final Message T_head1 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.head1");
	private static final Message T_para1 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.para1");
	private static final Message T_column1 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column1");
	private static final Message T_column2 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column2");
	private static final Message T_column3 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column3");
    private static final Message T_column4 = message("xmlui.aspect.versioning.DeleteVersionsConfirm.column4");


    private static final Message T_submit_delete = message("xmlui.general.delete");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");


	public void addPageMeta(PageMeta pageMeta) throws WingException{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		//pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, AuthorizeException{

		Division main = createMainDivision(body);

		createTable(main);

        addButtons(main);

		main.addHidden("versioning-continue").setValue(knot.getId());
	}


    private Division createMainDivision(Body body) throws WingException {
        Division main = body.addInteractiveDivision("versions-confirm-delete", contextPath+"/item/versionhistory", Division.METHOD_POST, "delete version");
		main.setHead(T_head1);
		main.addPara(T_para1);
        return main;
    }


    private void createTable(Division main) throws WingException {

        // Get all our parameters
		String idsString = parameters.getParameter("versionIDs", null);

        Table table = main.addTable("versions-confirm-delete", 1, 1);

		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		header.addCellContent(T_column2);
		header.addCellContent(T_column3);
        header.addCellContent(T_column4);


        for (String id : idsString.split(",")){
            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
            Version v=null;

            if(id!=null && !"".equals(id) && !" ".equals(id)){
                v = versioningService.getVersion(context, Integer.parseInt(id));
            }

            if(v!=null){
                Row row = table.addRow();
			    row.addCell().addContent(v.getVersionNumber());
                row.addCell().addContent(v.getEperson().getEmail());
                row.addCell().addContent(v.getVersionDate().toString());
                row.addCell().addContent(v.getSummary());
            }
		}

    }

    private void addButtons(Division main) throws WingException {
        Para buttons = main.addPara();
		buttons.addButton("submit_confirm").setValue(T_submit_delete);
		buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    }
}

