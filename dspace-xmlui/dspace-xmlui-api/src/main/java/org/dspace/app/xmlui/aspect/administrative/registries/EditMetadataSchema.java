/*
 * EditMetadataSchema.java
 *
 * Version: $Revision: 1.0 $
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
package org.dspace.app.xmlui.aspect.administrative.registries;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;

/**
 * Edit a metadata schema by: listing all the existing fields in
 * the schema, prompt the user to add a new field. If a current
 * field is selected then the field may be updated in the same
 * place where new field addition would be.
 * 
 * @author Scott Phillips
 */
public class EditMetadataSchema extends AbstractDSpaceTransformer   
{	
	
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	private static final Message T_title =
		message("xmlui.administrative.registries.EditMetadataSchema.title");
	private static final Message T_metadata_registry_trail =
		message("xmlui.administrative.registries.general.metadata_registry_trail");
	private static final Message T_trail =
		message("xmlui.administrative.registries.EditMetadataSchema.trail");	
	private static final Message T_head1 =
		message("xmlui.administrative.registries.EditMetadataSchema.head1");	
	private static final Message T_para1 =
		message("xmlui.administrative.registries.EditMetadataSchema.para1");	
	private static final Message T_head2 =
		message("xmlui.administrative.registries.EditMetadataSchema.head2");	
	private static final Message T_column1 =
		message("xmlui.administrative.registries.EditMetadataSchema.column1");	
	private static final Message T_column2 =
		message("xmlui.administrative.registries.EditMetadataSchema.column2");	
	private static final Message T_column3 =
		message("xmlui.administrative.registries.EditMetadataSchema.column3");	
	private static final Message T_column4 =
		message("xmlui.administrative.registries.EditMetadataSchema.column4");	
	private static final Message T_empty =
		message("xmlui.administrative.registries.EditMetadataSchema.empty");	
	private static final Message T_submit_return =
		message("xmlui.general.return");	
	private static final Message T_submit_delete =
		message("xmlui.administrative.registries.EditMetadataSchema.submit_delete");	
	private static final Message T_submit_move =
		message("xmlui.administrative.registries.EditMetadataSchema.submit_move");	
	private static final Message T_head3 =
		message("xmlui.administrative.registries.EditMetadataSchema.head3");	
	private static final Message T_name =
		message("xmlui.administrative.registries.EditMetadataSchema.name");	
	private static final Message T_note =
		message("xmlui.administrative.registries.EditMetadataSchema.note");	
	private static final Message T_note_help =
		message("xmlui.administrative.registries.EditMetadataSchema.note_help");
	private static final Message T_submit_add =
		message("xmlui.administrative.registries.EditMetadataSchema.submit_add");	
	private static final Message T_head4 =
		message("xmlui.administrative.registries.EditMetadataSchema.head4");
	private static final Message T_submit_update =
		message("xmlui.administrative.registries.EditMetadataSchema.submit_update");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	private static final Message T_error =
		message("xmlui.administrative.registries.EditMetadataSchema.error");	
	private static final Message T_error_duplicate_field =
		message("xmlui.administrative.registries.EditMetadataSchema.error_duplicate_field");	
	private static final Message T_error_element_empty =
		message("xmlui.administrative.registries.EditMetadataSchema.error_element_empty");	
	private static final Message T_error_element_badchar =
		message("xmlui.administrative.registries.EditMetadataSchema.error_element_badchar");	
	private static final Message T_error_element_tolong =
		message("xmlui.administrative.registries.EditMetadataSchema.error_element_tolong");	
	private static final Message T_error_qualifier_tolong =
		message("xmlui.administrative.registries.EditMetadataSchema.error_qualifier_tolong");	
	private static final Message T_error_qualifier_badchar =
		message("xmlui.administrative.registries.EditMetadataSchema.error_qualifier_badchar");	
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/metadata-registry",T_metadata_registry_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		// Get our parameters & state
		int schemaID = parameters.getParameterAsInteger("schemaID",-1);
		int updateID = parameters.getParameterAsInteger("updateID",-1);
		int highlightID = parameters.getParameterAsInteger("highlightID",-1);
		MetadataSchema schema = MetadataSchema.find(context,schemaID);
		MetadataField[] fields = MetadataField.findAllInSchema(context, schemaID);
		String schemaName = schema.getName();
		String schemaNamespace = schema.getNamespace();
		
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
		{
			for (String error : errorString.split(","))
				errors.add(error);
		}
		
	
        // DIVISION: edit-schema
		Division main = body.addInteractiveDivision("metadata-schema-edit",contextPath+"/admin/metadata-registry",Division.METHOD_POST,"primary administrative metadata-registry");
		main.setHead(T_head1.parameterize(schemaName));
		main.addPara(T_para1.parameterize(schemaNamespace));
		
		
		// DIVISION: add or updating a metadata field
		if (updateID >= 0)
			// Updating an existing field
			addUpdateFieldForm(main, schemaName, updateID,  errors);
		else
			// Add a new field
			addNewFieldForm(main, schemaName, errors);
		
		
		
		// DIVISION: existing fields
		Division existingFields = main.addDivision("metadata-schema-edit-existing-fields");
		existingFields.setHead(T_head2);
		
		Table table = existingFields.addTable("metadata-schema-edit-existing-fields", fields.length+1, 5);
		
		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		header.addCellContent(T_column2);
		header.addCellContent(T_column3);
		header.addCellContent(T_column4);
		
		for (MetadataField field : fields)
		{
			String id = String.valueOf(field.getFieldID());
			String fieldElement = field.getElement();
			String fieldQualifier = field.getQualifier();
			
			String fieldName = schemaName +"."+ fieldElement;
			if (fieldQualifier != null && fieldQualifier.length() > 0)
				fieldName += "."+fieldQualifier;
				
			boolean highlight = false;
			if (field.getFieldID() == highlightID)
				highlight = true;
			
			String fieldScopeNote = field.getScopeNote();
			
			String url = contextPath + "/admin/metadata-registry?administrative-continue="+knot.getId()+"&submit_edit&fieldID="+id;
			
			Row row;
			if (highlight)
				row = table.addRow(null,null,"highlight");
			else
				row = table.addRow();
			
			CheckBox select = row.addCell().addCheckBox("select_field");
			select.setLabel(id);
			select.addOption(id);
			
			row.addCell().addContent(id);
			row.addCell().addXref(url,fieldName);
			row.addCell().addContent(fieldScopeNote);
		}
		
		if (fields.length == 0)
		{
			// No fields, let the user know.
			table.addRow().addCell(1,4).addHighlight("italic").addContent(T_empty);
			main.addPara().addButton("submit_return").setValue(T_submit_return);
		}
		else
		{
			// Only show the actions if there are fields available to preform them on.
			Para actions = main.addPara();
			actions.addButton("submit_delete").setValue(T_submit_delete);
			if (MetadataSchema.findAll(context).length > 1)
				actions.addButton("submit_move").setValue(T_submit_move);	
			actions.addButton("submit_return").setValue(T_submit_return);
		}
		
		main.addHidden("administrative-continue").setValue(knot.getId());
        
   }
	
	
	/**
	 * Add a form prompting the user to add a new field to the this schema.
	 *  
	 * @param div The division to add the form too.
	 * @param schemaName The schemaName currently being operated on.
	 * @param errors A list of errors from previous attempts at adding new fields.
	 */
	public void addNewFieldForm(Division div, String schemaName, ArrayList<String> errors) throws WingException
	{
		Request request = ObjectModelHelper.getRequest(objectModel);
		String elementValue = request.getParameter("newElement");
		String qualifierValue = request.getParameter("newQualifier");
		String noteValue = request.getParameter("newNote");
		
		Division newField = div.addDivision("edit-schema-new-field");
		newField.setHead(T_head3);
		
		List form = newField.addList("edit-schema-new-field-form",List.TYPE_FORM);
		addFieldErrors(form, errors);
		
		form.addLabel(T_name);
		Highlight item =form.addItem().addHighlight("big");
		
		item.addContent(schemaName+" . ");
		Text element = item.addText("newElement");
		item.addContent(" . ");
		Text qualifier = item.addText("newQualifier");
		
		
		element.setSize(15);
		element.setValue(elementValue);
		
		qualifier.setSize(15);
		qualifier.setValue(qualifierValue);
		
		TextArea scopeNote =form.addItem().addTextArea("newNote");
		scopeNote.setLabel(T_note);
		scopeNote.setHelp(T_note_help);
		scopeNote.setSize(2, 35);
		scopeNote.setValue(noteValue);
		
		form.addItem().addButton("submit_add").setValue(T_submit_add);
	}
	

	/**
	 * Update an existing field by promting the user for it's values.
	 *  
	 * @param div The division to add the form too.
	 * @param schemaName The schemaName currently being operated on.
	 * @param fieldID The id of the field being updated.
	 * @param errors A list of errors from previous attempts at updaating the field.
	 */
	public void addUpdateFieldForm(Division div, String schemaName, int fieldID, ArrayList<String> errors) throws WingException, SQLException
	{
		
		MetadataField field = MetadataField.find(context, fieldID);
		
		Request request = ObjectModelHelper.getRequest(objectModel);
		String elementValue = request.getParameter("updateElement");
		String qualifierValue = request.getParameter("updateQualifier");
		String noteValue = request.getParameter("updateNote");
		
		if (elementValue == null)
			elementValue = field.getElement();
		if (qualifierValue == null)
			qualifierValue = field.getQualifier();
		if (noteValue == null)
			noteValue = field.getScopeNote();
		
		
		Division newField = div.addDivision("edit-schema-update-field");
		newField.setHead(T_head4.parameterize(field.getFieldID()));
		
		List form = newField.addList("edit-schema-update-field-form",List.TYPE_FORM);
		

		addFieldErrors(form, errors);
		
		form.addLabel(T_name);
		Highlight item =form.addItem().addHighlight("big");
		
		item.addContent(schemaName+" . ");
		Text element = item.addText("updateElement");
		item.addContent(" . ");
		Text qualifier = item.addText("updateQualifier");
		
		
		element.setSize(13);
		element.setValue(elementValue);
		
		qualifier.setSize(13);
		qualifier.setValue(qualifierValue);
		
		TextArea scopeNote =form.addItem().addTextArea("updateNote");
		scopeNote.setLabel(T_note);
		scopeNote.setHelp(T_note_help);
		scopeNote.setSize(2, 35);
		scopeNote.setValue(noteValue);
		
		Item actions = form.addItem();
		actions.addButton("submit_update").setValue(T_submit_update);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);
		
	}
	
	/**
	 * Determine if there were any special errors and display approparte 
	 * text. Because of the inline nature of the element and qualifier 
	 * fields these errors can not be placed on the field. Instead they 
	 * have to be added as seperate items above the field.
	 * 
	 * @param form The form to add errors to.
	 * @param errors A list of errors.
	 */
	
	public void addFieldErrors(List form, ArrayList<String> errors) throws WingException 
	{
		if (errors.contains("duplicate_field"))
		{
			form.addLabel(T_error);
			form.addItem(T_error_duplicate_field);
		}
		if (errors.contains("element_empty"))
		{
			form.addLabel(T_error);
			form.addItem(T_error_element_empty);
		}
		if (errors.contains("element_badchar"))
		{
			form.addLabel(T_error);
			form.addItem(T_error_element_badchar);
		}
		if (errors.contains("element_tolong"))
		{
			form.addLabel(T_error);
			form.addItem(T_error_element_tolong);
		}
		if (errors.contains("qualifier_tolong"))
		{
			form.addLabel(T_error);
			form.addItem(T_error_qualifier_tolong);
		}
		if (errors.contains("qualifier_badchar"))
		{
			form.addLabel(T_error);
			form.addItem(T_error_qualifier_badchar);
		}
	}
	
}
