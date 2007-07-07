/*
 * EditItemMetadataForm.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;

/**
 * Display a list of all metadata available for this item and allow the user to 
 * add, remove, or update it.
 * 
 * @author Jay Paz
 * @author Scott Phillips
 */

public class EditItemMetadataForm extends AbstractDSpaceTransformer {
	
	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_update = message("xmlui.general.update");
	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");
	private static final Message T_option_status = message("xmlui.administrative.item.general.option_status");
	private static final Message T_option_bitstreams = message("xmlui.administrative.item.general.option_bitstreams");
	private static final Message T_option_metadata = message("xmlui.administrative.item.general.option_metadata");
	private static final Message T_option_view = message("xmlui.administrative.item.general.option_view");

	private static final Message T_title = message("xmlui.administrative.item.EditItemMetadataForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.EditItemMetadataForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.EditItemMetadataForm.head1");
	private static final Message T_name_label = message("xmlui.administrative.item.EditItemMetadataForm.name_label");
	private static final Message T_value_label = message("xmlui.administrative.item.EditItemMetadataForm.value_label");
	private static final Message T_lang_label = message("xmlui.administrative.item.EditItemMetadataForm.lang_label");
	private static final Message T_submit_add = message("xmlui.administrative.item.EditItemMetadataForm.submit_add");
	private static final Message T_para1 = message("xmlui.administrative.item.EditItemMetadataForm.para1");

	
	private static final Message T_head2 = message("xmlui.administrative.item.EditItemMetadataForm.head2");
	private static final Message T_column1 = message("xmlui.administrative.item.EditItemMetadataForm.column1");
	private static final Message T_column2 = message("xmlui.administrative.item.EditItemMetadataForm.column2");
	private static final Message T_column3 = message("xmlui.administrative.item.EditItemMetadataForm.column3");
	private static final Message T_column4 = message("xmlui.administrative.item.EditItemMetadataForm.column4");

	
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	/**
	 * Add either the simple Ajax response document or the full
	 * document with header and full edit item form based on the 
	 * mode parameter.  Mode parameter values are set in the Flowscipt
	 * and can be either 'ajax' or 'normal'.
	 */
	@SuppressWarnings("unchecked") // the cast is correct
	public void addBody(Body body) throws SQLException, WingException
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		Item item = Item.find(context, itemID);
		DCValue[] values = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
		Arrays.sort(values, new DCValueComparator());
		String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();

		Request request = ObjectModelHelper.getRequest(objectModel);
		String previousFieldID = request.getParameter("field");


		// DIVISION: main 
		Division main = body.addInteractiveDivision("edit-item-status", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative item");
		main.setHead(T_option_head);

		
		
		// LIST: options
		List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
		options.addItem().addXref(baseURL+"&submit_status",T_option_status);
		options.addItem().addXref(baseURL+"&submit_bitstreams",T_option_bitstreams);
		options.addItem().addHighlight("bold").addXref(baseURL+"&submit_metadata",T_option_metadata);
		options.addItem().addXref(baseURL + "&view_item", T_option_view);


		// LIST: add new metadata
		List addForm = main.addList("addItemMetadata",List.TYPE_FORM);
		addForm.setHead(T_head1);

		Select addName = addForm.addItem().addSelect("field");
		addName.setLabel(T_name_label);
		MetadataField[] fields = MetadataField.findAll(context);
		for (MetadataField field : fields)
		{
			int fieldID = field.getFieldID();
			MetadataSchema schema = MetadataSchema.find(context, field.getSchemaID());
			String name = schema.getName() +"."+field.getElement();
			if (field.getQualifier() != null)
				name += "."+field.getQualifier();

			addName.addOption(fieldID, name);
		}
		if (previousFieldID != null)
			addName.setOptionSelected(previousFieldID);


		Composite addComposite = addForm.addItem().addComposite("value");
		addComposite.setLabel(T_value_label);
		TextArea addValue = addComposite.addTextArea("value");
		Text addLang = addComposite.addText("language");

		addValue.setSize(4, 35);
		addLang.setLabel(T_lang_label);
		addLang.setSize(6);

		addForm.addItem().addButton("submit_add").setValue(T_submit_add);

		
		

		// PARA: Disclaimer
		main.addPara(T_para1);

		
		Para actions = main.addPara(null,"edit-metadata-actions top" );
		actions.addButton("submit_update").setValue(T_submit_update);
		actions.addButton("submit_return").setValue(T_submit_return);
		
		

		// TABLE: Metadata
		main.addHidden("scope").setValue("*");
		int index = 1;
		Table table = main.addTable("editItemMetadata",1,1);
		table.setHead(T_head2);

		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCell().addContent(T_column1);
		header.addCell().addContent(T_column2);
		header.addCell().addContent(T_column3);
		header.addCell().addContent(T_column4);

		for(DCValue value : values)
		{
			String name = value.schema + "_" + value.element;
			if (value.qualifier != null)
				name += "_" + value.qualifier;

			Row row = table.addRow(name,Row.ROLE_DATA,"metadata-value");

			CheckBox remove = row.addCell().addCheckBox("remove_"+index);
			remove.setLabel("remove");
			remove.addOption(index);

			Cell cell = row.addCell();
			cell.addContent(name.replaceAll("_", ". "));
			cell.addHidden("name_"+index).setValue(name);

			TextArea mdValue = row.addCell().addTextArea("value_"+index);
			mdValue.setSize(4,35);
			mdValue.setValue(value.value);

			Text mdLang = row.addCell().addText("language_"+index);
			mdLang.setSize(6);
			mdLang.setValue(value.language);

			// Tick the index counter;
			index++;
		}

		
		
		
		// PARA: actions
		actions = main.addPara(null,"edit-metadata-actions bottom" );
		actions.addButton("submit_update").setValue(T_submit_update);
		actions.addButton("submit_return").setValue(T_submit_return);


		main.addHidden("administrative-continue").setValue(knot.getId());
	}



	/**
	 * Compare two metadata element's name so that they may be sorted.
	 */
	class DCValueComparator implements Comparator{
		public int compare(Object arg0, Object arg1) {
			final DCValue o1 = (DCValue)arg0;
			final DCValue o2 = (DCValue)arg1;
			final String s1 = o1.schema + o1.element + (o1.qualifier==null?"":("." + o1.qualifier));
			final  String s2 = o2.schema + o2.element + (o2.qualifier==null?"":("." + o2.qualifier));
			return s1.compareTo(s2);
		}
	}

}
