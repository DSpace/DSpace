/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.registries;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

	protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
	protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

	
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
		List<MetadataSchema> schemas = metadataSchemaService.findAll(context);
		String idsString = parameters.getParameter("fieldIDs", null);
		
		ArrayList<MetadataField> fields = new ArrayList<MetadataField>();
		for (String id : idsString.split(","))
		{
			MetadataField field = metadataFieldService.find(context,Integer.valueOf(id));
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
    		String fieldID = String.valueOf(field.getID());
			String fieldEelement = field.getElement();
			String fieldQualifier = field.getQualifier();
			
			MetadataSchema schema = field.getMetadataSchema();
			String schemaName = schema.getName();
			
			StringBuilder fieldName = new StringBuilder()
                                            .append(schemaName)
                                            .append(".")
                                            .append(fieldEelement);

			if (fieldQualifier != null && fieldQualifier.length() > 0)
            {
				fieldName.append(".").append(fieldQualifier);
            }
				
			String fieldScopeNote = field.getScopeNote();
    		
    		Row row = table.addRow();
    		row.addCell().addContent(fieldID);
        	row.addCell().addContent(fieldName.toString());
        	row.addCell().addContent(fieldScopeNote);
	    }

    	Row row = table.addRow();
    	Cell cell = row.addCell(1,3);
    	cell.addContent(T_para2);
    	Select toSchema = cell.addSelect("to_schema");
    	for (MetadataSchema schema : schemas)
    	{
    		toSchema.addOption(schema.getID(), schema.getNamespace());
    	}
    	
    	Para buttons = moved.addPara();
    	buttons.addButton("submit_move").setValue(T_submit_move);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	
    	
    	moved.addHidden("administrative-continue").setValue(knot.getId());
    }
}
