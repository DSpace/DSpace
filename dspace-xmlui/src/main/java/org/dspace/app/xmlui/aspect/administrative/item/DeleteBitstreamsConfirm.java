/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;

/**
 * Present the user with a list of not-yet-but-soon-to-be-deleted-bitstreams.
 * 
 * @author Scott Phillips
 */
public class DeleteBitstreamsConfirm extends AbstractDSpaceTransformer   
{
	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	
	
	private static final Message T_title = message("xmlui.administrative.item.DeleteBitstreamConfirm.title");
	private static final Message T_trail = message("xmlui.administrative.item.DeleteBitstreamConfirm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.DeleteBitstreamConfirm.head1");
	private static final Message T_para1 = message("xmlui.administrative.item.DeleteBitstreamConfirm.para1");
	private static final Message T_column1 = message("xmlui.administrative.item.DeleteBitstreamConfirm.column1");
	private static final Message T_column2 = message("xmlui.administrative.item.DeleteBitstreamConfirm.column2");
	private static final Message T_column3 = message("xmlui.administrative.item.DeleteBitstreamConfirm.column3");
	private static final Message T_submit_delete = message("xmlui.general.delete");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");		

	protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		// Get all our parameters
		String idsString = parameters.getParameter("bitstreamIDs", null);

		ArrayList<Bitstream> bitstreams = new ArrayList<Bitstream>();
		for (String id : idsString.split(","))
		{
			String[] parts = id.split("/");

			if (parts.length != 2)
            {
                throw new UIException("Unable to parse id into bundle and bitstream id: " + id);
            }

			UUID bitstreamID = UUID.fromString(parts[1]);

			Bitstream bitstream = bitstreamService.find(context,bitstreamID);
			bitstreams.add(bitstream);

		}

		// DIVISION: bitstream-confirm-delete
		Division deleted = body.addInteractiveDivision("bitstreams-confirm-delete",contextPath+"/admin/item",Division.METHOD_POST,"primary administrative item");
		deleted.setHead(T_head1);
		deleted.addPara(T_para1);

		Table table = deleted.addTable("bitstreams-confirm-delete",bitstreams.size() + 1, 1);

		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		header.addCellContent(T_column2);
		header.addCellContent(T_column3);

		for (Bitstream bitstream : bitstreams) 
		{
			String format = null;
			BitstreamFormat bitstreamFormat = bitstream.getFormat(context);
			if (bitstreamFormat != null)
            {
                format = bitstreamFormat.getShortDescription();
            }

			Row row = table.addRow();
			row.addCell().addContent(bitstream.getName());
			row.addCell().addContent(bitstream.getDescription());
			row.addCell().addContent(format);
		}
		Para buttons = deleted.addPara();
		buttons.addButton("submit_confirm").setValue(T_submit_delete);
		buttons.addButton("submit_cancel").setValue(T_submit_cancel);

		deleted.addHidden("administrative-continue").setValue(knot.getId());
	}
}
