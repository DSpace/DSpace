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

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Prompt the user with a list of to-be-deleted metadata fields and
 * ask the user if they are sure they want them deleted.
 * 
 * @author Scott Phillips
 */
public class DeleteMetadataFieldsConfirm extends AbstractDSpaceTransformer   
{
	
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	private static final Message T_submit_delete =
		message("xmlui.general.delete");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");
	private static final Message T_title =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.title");
	private static final Message T_metadata_registry_trail =
		message("xmlui.administrative.registries.general.metadata_registry_trail");
	private static final Message T_trail =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.trail");
	private static final Message T_head =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.head");
	private static final Message T_para1 =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.para1");
	private static final Message T_warning =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.warning");
	private static final Message T_para2 =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.para2");
	private static final Message T_column1 =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.column1");
	private static final Message T_column2 =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.column2");
	private static final Message T_column3 =
		message("xmlui.administrative.registries.DeleteMetadataFieldsConfirm.column3");

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
		String idsString = parameters.getParameter("fieldIDs", null);
		
		ArrayList<MetadataField> fields = new ArrayList<MetadataField>();
		for (String id : idsString.split(","))
		{
			MetadataField field = metadataFieldService.find(context,Integer.valueOf(id));
			fields.add(field);
		}
 
		// DIVISION: metadata-field-confirm-delete
    	Division deleted = body.addInteractiveDivision("metadata-field-confirm-delete",contextPath+"/admin/metadata-registry",Division.METHOD_POST,"primary administrative metadata-registry");
    	deleted.setHead(T_head);
    	deleted.addPara(T_para1);
    	Para warning = deleted.addPara();
    	warning.addHighlight("bold").addContent(T_warning);
    	warning.addContent(T_para2);
    	
    	Table table = deleted.addTable("field-confirm-delete",fields.size() + 1, 3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
    	
    	for (MetadataField field : fields) 
    	{
    		if (field == null)
            {
                continue;
            }
    		
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
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_delete);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
