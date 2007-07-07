/*
 * MetadataRegistryMain.java
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
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.MetadataSchema;

/**
 * This is the main entry point for managing the metadata registry. This transformer 
 * shows the list of all current schemas and a form for adding new schema's to the
 * registry.
 * 
 * @author Scott Phillips
 */
public class MetadataRegistryMain extends AbstractDSpaceTransformer   
{	
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	private static final Message T_title =
		message("xmlui.administrative.registries.MetadataRegistryMain.title");
	private static final Message T_metadata_registry_trail =
		message("xmlui.administrative.registries.general.metadata_registry_trail");
	private static final Message T_head1 =
		message("xmlui.administrative.registries.MetadataRegistryMain.head1");
	private static final Message T_para1 =
		message("xmlui.administrative.registries.MetadataRegistryMain.para1");
	private static final Message T_column1 =
		message("xmlui.administrative.registries.MetadataRegistryMain.column1");
	private static final Message T_column2 =
		message("xmlui.administrative.registries.MetadataRegistryMain.column2");
	private static final Message T_column3 =
		message("xmlui.administrative.registries.MetadataRegistryMain.column3");
	private static final Message T_column4 =
		message("xmlui.administrative.registries.MetadataRegistryMain.column4");
	private static final Message T_submit_delete =
		message("xmlui.administrative.registries.MetadataRegistryMain.submit_delete");
	private static final Message T_head2 =
		message("xmlui.administrative.registries.MetadataRegistryMain.head2");
	private static final Message T_namespace =
		message("xmlui.administrative.registries.MetadataRegistryMain.namespace");
	private static final Message T_namespace_help =
		message("xmlui.administrative.registries.MetadataRegistryMain.namespace_help");
	private static final Message T_namespace_error =
		message("xmlui.administrative.registries.MetadataRegistryMain.namespace_error");
	private static final Message T_name =
		message("xmlui.administrative.registries.MetadataRegistryMain.name");
	private static final Message T_name_help =
		message("xmlui.administrative.registries.MetadataRegistryMain.name_help");
	private static final Message T_name_error =
		message("xmlui.administrative.registries.MetadataRegistryMain.name_error");
	private static final Message T_submit_add =
		message("xmlui.administrative.registries.MetadataRegistryMain.submit_add");
	
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/metadata-registry",T_metadata_registry_trail);
    }
		
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		// Get all our parameters first
		Request request = ObjectModelHelper.getRequest(objectModel);
		String namespaceValue = request.getParameter("namespace");
		String nameValue = request.getParameter("name");
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
		{
			for (String error : errorString.split(","))
				errors.add(error);
		}
		MetadataSchema[] schemas = MetadataSchema.findAll(context); 
        
		
		
        // DIVISION: metadata-registry-main
		Division main = body.addInteractiveDivision("metadata-registry-main",contextPath+"/admin/metadata-registry",Division.METHOD_POST,"primary administrative metadata-registry ");
		main.setHead(T_head1);
		main.addPara(T_para1);
		
		Table table = main.addTable("metadata-registry-main-table", schemas.length+1, 5);
		
		Row header = table.addRow(Row.ROLE_HEADER);
		header.addCellContent(T_column1);
		header.addCellContent(T_column2);
		header.addCellContent(T_column3);
		header.addCellContent(T_column4);
		
		for (MetadataSchema schema : schemas)
		{
			int schemaID     = schema.getSchemaID();
			String namespace = schema.getNamespace();
			String name      = schema.getName();
			String url = contextPath + "/admin/metadata-registry?administrative-continue="+knot.getId()+"&submit_edit&schemaID="+schemaID;
			
			Row row = table.addRow();
			if (schemaID > 1)
			{
				// If the schema is not in the required DC schema allow the user to delete it.	
				CheckBox select = row.addCell().addCheckBox("select_schema");
				select.setLabel(String.valueOf(schemaID));
				select.addOption(String.valueOf(schemaID));
			}
			else
			{
				// The DC schema can not be removed.
				row.addCell();
			}
			
			row.addCell().addContent(schemaID);
			row.addCell().addXref(url,namespace);
			row.addCell().addXref(url,name);
		}
		if (schemas.length > 1)
		{
			// Only give the delete option if there are more schema's than the required dublin core.
			main.addPara().addButton("submit_delete").setValue(T_submit_delete);
		}
		
		
		
		// DIVISION: add new schema
		Division newSchema = main.addDivision("add-schema");
		newSchema.setHead(T_head2);
		
		List form = newSchema.addList("new-schema",List.TYPE_FORM);
		
		Text namespace = form.addItem().addText("namespace");
		namespace.setLabel(T_namespace);
		namespace.setRequired();
		namespace.setHelp(T_namespace_help);
		if (namespaceValue != null)
			namespace.setValue(namespaceValue);
		if (errors.contains("namespace"))
			namespace.addError(T_namespace_error);
		
		Text name = form.addItem().addText("name");
		name.setLabel(T_name);
		name.setRequired();
		name.setHelp(T_name_help);
		if (nameValue != null)
			name.setValue(nameValue);
		if (errors.contains("name"))
			name.addError(T_name_error);
		
		form.addItem().addButton("submit_add").setValue(T_submit_add);
   
		
		main.addHidden("administrative-continue").setValue(knot.getId());
   }
}
