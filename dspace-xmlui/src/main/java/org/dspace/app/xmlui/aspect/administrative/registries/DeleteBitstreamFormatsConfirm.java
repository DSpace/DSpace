/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.registries;

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
import org.dspace.content.BitstreamFormat;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;

/**
 * Confirm the deletion of bitstream formats by listing to-be-deleted
 * formats and asking the user for confirmation.
 * 
 * @author Scott Phillips
 */
public class DeleteBitstreamFormatsConfirm extends AbstractDSpaceTransformer   
{
	
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	private static final Message T_submit_delete =
		message("xmlui.general.delete");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	private static final Message T_title =
		message("xmlui.administrative.registries.DeleteBitstreamFormatsConfirm.title");
	private static final Message T_format_registry_trail =
		message("xmlui.administrative.registries.general.format_registry_trail");
	private static final Message T_trail =
		message("xmlui.administrative.registries.DeleteBitstreamFormatsConfirm.trail");
	private static final Message T_head =
		message("xmlui.administrative.registries.DeleteBitstreamFormatsConfirm.head");
	private static final Message T_para1 =
		message("xmlui.administrative.registries.DeleteBitstreamFormatsConfirm.para1");
	private static final Message T_column1 =
		message("xmlui.administrative.registries.DeleteBitstreamFormatsConfirm.column1");
	private static final Message T_column2 =
		message("xmlui.administrative.registries.DeleteBitstreamFormatsConfirm.column2");
	private static final Message T_column3 =
		message("xmlui.administrative.registries.DeleteBitstreamFormatsConfirm.column3");


	protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/format-registry",T_format_registry_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		// Get all our parameters
		String idsString = parameters.getParameter("formatIDs", null);
		
		ArrayList<BitstreamFormat> formats = new ArrayList<BitstreamFormat>();
		for (String id : idsString.split(","))
		{
			BitstreamFormat format = bitstreamFormatService.find(context,Integer.valueOf(id));
			formats.add(format);
		}
 
		// DIVISION: bitstream-format-confirm-delete
    	Division deleted = body.addInteractiveDivision("bitstream-format-confirm-delete",contextPath+"/admin/format-registry",Division.METHOD_POST,"primary administrative format-registry");
    	deleted.setHead(T_head);
    	deleted.addPara(T_para1);
    	
    	Table table = deleted.addTable("format-confirm-delete",formats.size() + 1, 3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
    	
    	for (BitstreamFormat format : formats) 
    	{
    		if (format == null)
            {
                continue;
            }
    		
    		String formatID = String.valueOf(format.getID());
    		String mimetype = format.getMIMEType();
    		String name = format.getShortDescription();

    		
    		Row row = table.addRow();
    		row.addCell().addContent(formatID);
        	row.addCell().addContent(mimetype);
        	row.addCell().addContent(name);
	    }
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_delete);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
