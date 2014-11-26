/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;


/**
 * This page is used as a general confirmation page for any
 * actions on items. It will display the item and ask if the
 * user is sure they want to perform the action.
 * 
 * The "confirm" parameter determines what action is confirmed.
 * There are three possible values, "delete", "withdraw", or
 * "reinstate"
 * 
 * @author Jay Paz
 * @author Scott Phillips
 */
public class ConfirmItemForm extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	
	private static final Message T_title = message("xmlui.administrative.item.ConfirmItemForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.ConfirmItemForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.ConfirmItemForm.head1");
	private static final Message T_para_delete = message("xmlui.administrative.item.ConfirmItemForm.para_delete");
	private static final Message T_para_withdraw = message("xmlui.administrative.item.ConfirmItemForm.para_withdraw");
	private static final Message T_para_reinstate = message("xmlui.administrative.item.ConfirmItemForm.para_reinstate");
	private static final Message T_column1 = message("xmlui.administrative.item.ConfirmItemForm.column1");
	private static final Message T_column2 = message("xmlui.administrative.item.ConfirmItemForm.column2");
	private static final Message T_column3 = message("xmlui.administrative.item.ConfirmItemForm.column3");
	private static final Message T_submit_delete = message("xmlui.general.delete");
	private static final Message T_submit_withdraw = message("xmlui.administrative.item.ConfirmItemForm.submit_withdraw");
	private static final Message T_submit_reinstate = message("xmlui.administrative.item.ConfirmItemForm.submit_reinstate");

    private static final Message T_submit_private = message("xmlui.administrative.item.ConfirmItemForm.submit_private");
    private static final Message T_submit_public = message("xmlui.administrative.item.ConfirmItemForm.submit_public");


	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath+"/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	@SuppressWarnings("unchecked") // the cast is correct
	public void addBody(Body body) throws WingException, SQLException 
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		Item item = Item.find(context, itemID);
		final Metadatum[] values = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
		Arrays.sort(values, new DCValueComparator());

		String confirm = parameters.getParameter("confirm",null);


		// DIVISION: Main
		Division main = body.addInteractiveDivision("confirm-item", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative item");
		main.setHead(T_head1.parameterize(item.getHandle()));

		// PARA: descriptive instructions
		if("delete".equals(confirm))
		{
			main.addPara(T_para_delete);
		}
		else if ("reinstate".equals(confirm))
		{
			main.addPara(T_para_reinstate);
		}
		else if ("withdraw".equals(confirm))
		{
			main.addPara(T_para_withdraw);
		}


		// TABLE: metadata table
		Table table = main.addTable("withdrawValues", values.length+1, 3);
		final Row header = table.addRow(Row.ROLE_HEADER);
		header.addCell().addContent(T_column1);
		header.addCell().addContent(T_column2);
		header.addCell().addContent(T_column3);
		for(final Metadatum value:values){
			final String dcValue = value.schema + ". " + value.element + (value.qualifier==null?"":(". " + value.qualifier));
			final Row row = table.addRow();
			row.addCell().addContent(dcValue);
			row.addCell().addContent(value.value);
			row.addCell().addContent(value.language);
		}

		// LIST: actions, confirm or return
		org.dspace.app.xmlui.wing.element.Item actions = main.addList("actions", List.TYPE_FORM).addItem();

		Button confirmButton = actions.addButton("submit_confirm");

		if("delete".equals(confirm))
		{
			confirmButton.setValue(T_submit_delete);
		}
		else if ("reinstate".equals(confirm))
		{
			confirmButton.setValue(T_submit_reinstate);
		}
        else if ("private".equals(confirm))
        {
            confirmButton.setValue(T_submit_private);
        }
        else if ("public".equals(confirm))
        {
            confirmButton.setValue(T_submit_public);
        }
		else if ("withdraw".equals(confirm))
		{
			confirmButton.setValue(T_submit_withdraw);
		}
		actions.addButton("submit_cancel").setValue(T_submit_cancel);

		main.addHidden("administrative-continue").setValue(knot.getId());
	}


	/**
	 * Compare names of two metadata elements so that they may be sorted.
	 */
	static class DCValueComparator implements Comparator, Serializable {
		public int compare(Object arg0, Object arg1) {
			final Metadatum o1 = (Metadatum)arg0;
			final Metadatum o2 = (Metadatum)arg1;
			final String s1 = o1.schema + o1.element + (o1.qualifier==null?"":("." + o1.qualifier));
			final  String s2 = o2.schema + o2.element + (o2.qualifier==null?"":("." + o2.qualifier));
			return s1.compareTo(s2);
		}
	}

}
