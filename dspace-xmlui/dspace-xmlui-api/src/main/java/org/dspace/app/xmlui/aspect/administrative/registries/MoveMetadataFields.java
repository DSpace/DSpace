/*
 * MoveMetadataFields.java
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

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;

/**
 * Show a list of selected fields, and prompt the user to enter a destination schema for these fields.
 * 
 * @author Scott Phillips
 */

public class MoveMetadataFields extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	private static final Message T_title =
		message("xmlui.administrative.registries.MoveMetadataField.title");
	private static final Message T_metadata_registry_trail =
		message("xmlui.administrative.registries.general.metadata_registry_trail");
	private static final Message T_trail =
		message("xmlui.administrative.registries.MoveMetadataField.trail");
	private static final Message T_head1 =
		message("xmlui.administrative.registries.MoveMetadataField.head1");
	private static final Message T_para1 =
		message("xmlui.administrative.registries.MoveMetadataField.para1");
	private static final Message T_column1 =
		message("xmlui.administrative.registries.MoveMetadataField.column1");
	private static final Message T_column2 =
		message("xmlui.administrative.registries.MoveMetadataField.column2");
	private static final Message T_column3 =
		message("xmlui.administrative.registries.MoveMetadataField.column3");
	private static final Message T_para2 =
		message("xmlui.administrative.registries.MoveMetadataField.para2");
	private static final Message T_submit_move =
		message("xmlui.administrative.registries.MoveMetadataField.submit_move");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/metadata-registry",T_metadata_registry_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		// Get all our parameters
		MetadataSchema[] schemas = MetadataSchema.findAll(context); 
		String idsString = parameters.getParameter("fieldIDs", null);
		
		ArrayList<MetadataField> fields = new ArrayList<MetadataField>();
		for (String id : idsString.split(","))
		{
			MetadataField field = MetadataField.find(context,Integer.valueOf(id));
			fields.add(field);
		}
 
		
		// DIVISION: metadata-field-move
    	Division moved = body.addInteractiveDivision("metadata-field-move",contextPath+"/admin/metadata-registry",Division.METHOD_POST,"primary administrative metadata-registry");
    	moved.setHead(T_head1);
    	moved.addPara(T_para1);
    	
    	Table table = moved.addTable("metadata-field-move",fields.size() + 1, 3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
    	
    	for (MetadataField field : fields) 
    	{
    		String fieldID = String.valueOf(field.getFieldID());
			String fieldEelement = field.getElement();
			String fieldQualifier = field.getQualifier();
			
			MetadataSchema schema = MetadataSchema.find(context, field.getSchemaID());
			String schemaName = schema.getName();
			
			String fieldName = schemaName +"."+ fieldEelement;
			if (fieldQualifier != null && fieldQualifier.length() > 0)
				fieldName += "."+fieldQualifier;
				
			String fieldScopeNote = field.getScopeNote();
    		
    		Row row = table.addRow();
    		row.addCell().addContent(fieldID);
        	row.addCell().addContent(fieldName);
        	row.addCell().addContent(fieldScopeNote);
	    }

    	Row row = table.addRow();
    	Cell cell = row.addCell(1,3);
    	cell.addContent(T_para2);
    	Select toSchema = cell.addSelect("to_schema");
    	for (MetadataSchema schema : schemas)
    	{
    		toSchema.addOption(schema.getSchemaID(), schema.getNamespace());
    	}
    	
    	Para buttons = moved.addPara();
    	buttons.addButton("submit_move").setValue(T_submit_move);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	
    	
    	moved.addHidden("administrative-continue").setValue(knot.getId());
    }
}
