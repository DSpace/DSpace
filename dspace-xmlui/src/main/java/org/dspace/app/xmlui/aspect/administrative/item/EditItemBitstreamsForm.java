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

import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;

/**
 * Show a list of the item's bitstreams allowing the user to delete them, 
 * edit them, or upload new bitstreams.
 * 
 * based on class by Jay Paz and Scott Phillips
 * modified for LINDAT/CLARIN
 */

public class EditItemBitstreamsForm extends AbstractDSpaceTransformer {
	
	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");

	private static final Message T_title = message("xmlui.administrative.item.EditItemBitstreamsForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.EditItemBitstreamsForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.EditItemBitstreamsForm.head1");
	private static final Message T_column1 = message("xmlui.administrative.item.EditItemBitstreamsForm.column1");
	private static final Message T_column2 = message("xmlui.administrative.item.EditItemBitstreamsForm.column2");
	private static final Message T_column3 = message("xmlui.administrative.item.EditItemBitstreamsForm.column3");
	private static final Message T_column4 = message("xmlui.administrative.item.EditItemBitstreamsForm.column4");
	private static final Message T_column5 = message("xmlui.administrative.item.EditItemBitstreamsForm.column5");
	private static final Message T_column6 = message("xmlui.administrative.item.EditItemBitstreamsForm.column6");
	private static final Message T_column7 = message("xmlui.administrative.item.EditItemBitstreamsForm.column7");
	private static final Message T_bundle_label = message("xmlui.administrative.item.EditItemBitstreamsForm.bundle_label");
	private static final Message T_primary_label = message("xmlui.administrative.item.EditItemBitstreamsForm.primary_label");
	private static final Message T_view_link = message("xmlui.administrative.item.EditItemBitstreamsForm.view_link");
	private static final Message T_submit_add = message("xmlui.administrative.item.EditItemBitstreamsForm.submit_add");
	private static final Message T_submit_delete = message("xmlui.administrative.item.EditItemBitstreamsForm.submit_delete");

	private static final Message T_no_upload = message("xmlui.administrative.item.EditItemBitstreamsForm.no_upload");
	private static final Message T_no_remove = message("xmlui.administrative.item.EditItemBitstreamsForm.no_remove");
    private static final Message T_submit_reorder = message("xmlui.administrative.item.EditItemBitstreamsForm.submit_reorder");
    private static final Message T_order_up = message("xmlui.administrative.item.EditItemBitstreamsForm.order_up");
    private static final Message T_order_down = message("xmlui.administrative.item.EditItemBitstreamsForm.order_down");

    public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	
	public void addBody(Body body) throws SQLException, WingException
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		Item item = Item.find(context, itemID);
		String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();


		// DIVISION: main div
		Division main = body.addInteractiveDivision("edit-item-status", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative item");
		main.setHead(T_option_head);

		
		String tabLink = baseURL + "&submit_bitstreams";
		// LIST: options
		List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
		ViewItem.add_options(context, eperson, options, baseURL, ViewItem.T_option_bitstreams, tabLink);

		
		
		// TABLE: Bitstream summary
		
		Division div = main.addDivision("bitstream-form", "well well-light");
		
		Table files = div.addTable("editItemBitstreams", 1, 6);

		files.setHead(T_head1);

		Row header = files.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		Cell hcell = header.addCell();
		hcell.addContent(T_column2);
		hcell.addContent( "/assetstore id");
		header.addCellContent(T_column3);
		header.addCellContent(T_column4);
		header.addCellContent(T_column5);
		header.addCellContent(T_column6);
		//header.addCellContent(T_column7);

		Bundle[] bundles = item.getBundles();

        boolean showBitstreamUpdateOrderButton = false;
		for (Bundle bundle : bundles)
		{

			Cell bundleCell = files.addRow("bundle_head_" + bundle.getID(), Row.ROLE_DATA, "").addCell(1, 6);
			bundleCell.addContent(T_bundle_label.parameterize(bundle.getName()));

			Bitstream[] bitstreams = bundle.getBitstreams();
            ArrayList<Integer> bitstreamIdOrder = new ArrayList<Integer>();
            for (Bitstream bitstream : bitstreams) {
                bitstreamIdOrder.add(bitstream.getID());
            }

            for (int bitstreamIndex = 0; bitstreamIndex < bitstreams.length; bitstreamIndex++) {
                Bitstream bitstream = bitstreams[bitstreamIndex];
                boolean primary = (bundle.getPrimaryBitstreamID() == bitstream.getID());
                String name = bitstream.getName();
                String assetstore_name = String.format(
                		"\n%s\n", bitstream.get_internal_id());

                if (name != null && name.length() > 50) {
                    // If the fiel name is too long the shorten it so that it will display nicely.
                    String shortName = name.substring(0, 15);
                    shortName += " ... ";
                    shortName += name.substring(name.length() - 25, name.length());
                    name = shortName;
                }

                String description = bitstream.getDescription();
                String format = null;
                BitstreamFormat bitstreamFormat = bitstream.getFormat();
                if (bitstreamFormat != null) {
                    format = bitstreamFormat.getShortDescription();
                }
                String editURL = contextPath + "/admin/item?administrative-continue=" + knot.getId() + "&bitstreamID=" + bitstream.getID() + "&submit_edit";
                String viewURL = contextPath + "/bitstream/id/" + bitstream.getID() + "/" + bitstream.getName();


                Row row = files.addRow("bitstream_row_" + bitstream.getID(), Row.ROLE_DATA, "");
                CheckBox remove = row.addCell().addCheckBox("remove");
                remove.setLabel("remove");
                remove.addOption(bundle.getID() + "/" + bitstream.getID());
                if (!AuthorizeManager.authorizeActionBoolean(context, item, Constants.REMOVE)) {
                    remove.setDisabled();
                }

                if (AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.WRITE)) {
                    // The user can edit the bitstream give them a link.
                    Cell cell = row.addCell();
                    cell.addXref(editURL, name);
                    if (primary) {
                        cell.addXref(editURL, T_primary_label);
                    }
                    cell.addContent(assetstore_name);

                    row.addCell().addXref(editURL, description);
                    row.addCell().addXref(editURL, format);
                } else {
                    // The user can't edit the bitstream just show them it.
                    Cell cell = row.addCell();
                    cell.addContent(name);
                    if (primary) {
                        cell.addContent(T_primary_label);
                    }
                    cell.addContent(assetstore_name);

                    row.addCell().addContent(description);
                    row.addCell().addContent(format);
                }

                Highlight highlight = row.addCell().addHighlight("fade");
                highlight.addContent("[");
                highlight.addXref(viewURL, T_view_link);
                highlight.addContent("]");

                if (AuthorizeManager.authorizeActionBoolean(context, bundle, Constants.WRITE)) {
                    Cell cell = row.addCell("bitstream_order_" + bitstream.getID(), Cell.ROLE_DATA, "");
                    //Add the +1 to make it more human readable
                    cell.addHidden("order_" + bitstream.getID()).setValue(String.valueOf(bitstreamIndex + 1));
                    showBitstreamUpdateOrderButton = true;
                    Button upButton = cell.addButton("submit_order_" + bundle.getID() + "_" + bitstream.getID() + "_up", ((bitstreamIndex == 0) ? "disabled" : "") + " icon-button arrowUp ");
                    if((bitstreamIndex == 0)){
                        upButton.setDisabled();
                    }
                    upButton.setValue("▲");
                    upButton.setHelp(T_order_up);
                    Button downButton = cell.addButton("submit_order_" + bundle.getID() + "_" + bitstream.getID() + "_down", (bitstreamIndex == (bitstreams.length - 1) ? "disabled" : "") + " icon-button arrowDown ");
                    if(bitstreamIndex == (bitstreams.length - 1)){
                        downButton.setDisabled();
                    }
                    downButton.setValue("▼");
                    downButton.setHelp(T_order_down);

                    //These values will only be used IF javascript is disabled or isn't working
                    cell.addHidden(bundle.getID() + "_" + bitstream.getID() + "_up_value").setValue(retrieveOrderUpButtonValue((java.util.List<Integer>) bitstreamIdOrder.clone(), bitstreamIndex));
                    cell.addHidden(bundle.getID() + "_" + bitstream.getID() + "_down_value").setValue(retrieveOrderDownButtonValue((java.util.List<Integer>) bitstreamIdOrder.clone(), bitstreamIndex));
                }else{
                    row.addCell().addContent(String.valueOf(bitstreamIndex));
                }
            }
		}

		if (AuthorizeManager.authorizeActionBoolean(context, item, Constants.ADD))
		{
			Cell cell = files.addRow().addCell(1, 6);
			cell.addXref(contextPath+"/admin/item?administrative-continue="+knot.getId()+"&submit_add",T_submit_add);
		}
		else
		{
			Cell cell = files.addRow().addCell(1, 6);
			cell.addHighlight("fade").addContent(T_no_upload);
		}

		
		
		// PARA: actions
		Para actions = div.addPara("editItemActionsP","editItemActionsP" );
        if (showBitstreamUpdateOrderButton) {
            //Add a button to submit the new order (this button is hidden & will be displayed by the javascript)
            //Should javascript be disabled for some reason this button isn't used.
            actions.addButton("submit_update_order", "hidden").setValue(T_submit_reorder);
        }

        // Only System Administrators can delete bitstreams
		if (AuthorizeManager.authorizeActionBoolean(context, item, Constants.REMOVE))
        {
            actions.addButton("submit_delete").setValue(T_submit_delete);
        }
		else
		{
			Button button = actions.addButton("submit_delete");
			button.setValue(T_submit_delete);
			button.setDisabled();
			
			div.addPara().addHighlight("fade").addContent(T_no_remove);
		}
		actions.addButton("submit_return").setValue(T_submit_return);


		main.addHidden("administrative-continue").setValue(knot.getId());

	}

    private String retrieveOrderUpButtonValue(java.util.List<Integer> bitstreamIdOrder, int bitstreamIndex) {
        if(0 != bitstreamIndex){
            //We don't have the first button, so create a value where the current bitstreamId moves one up
            Integer temp = bitstreamIdOrder.get(bitstreamIndex);
            bitstreamIdOrder.set(bitstreamIndex, bitstreamIdOrder.get(bitstreamIndex - 1));
            bitstreamIdOrder.set(bitstreamIndex - 1, temp);
        }
        return StringUtils.join(bitstreamIdOrder.toArray(new Integer[bitstreamIdOrder.size()]), ",");
    }

    private String retrieveOrderDownButtonValue(java.util.List<Integer> bitstreamIdOrder, int bitstreamIndex) {
        if(bitstreamIndex < (bitstreamIdOrder.size()) -1){
            //We don't have the first button, so create a value where the current bitstreamId moves one up
            Integer temp = bitstreamIdOrder.get(bitstreamIndex);
            bitstreamIdOrder.set(bitstreamIndex, bitstreamIdOrder.get(bitstreamIndex + 1));
            bitstreamIdOrder.set(bitstreamIndex + 1, temp);
        }
        return StringUtils.join(bitstreamIdOrder.toArray(new Integer[bitstreamIdOrder.size()]), ",");
    }
}
