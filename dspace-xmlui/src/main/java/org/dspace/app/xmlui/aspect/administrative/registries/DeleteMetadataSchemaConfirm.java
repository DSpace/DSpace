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
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataSchemaService;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Prompt the user to determin if they really want to delete the displayed schemas.
 * 
 * @author Scott Phillips
 */
public class DeleteMetadataSchemaConfirm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	private static final Message T_title =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.title");
	private static final Message T_metadata_registry_trail =
		message("xmlui.administrative.registries.general.metadata_registry_trail");
	private static final Message T_trail =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.trail");
	private static final Message T_head =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.head");
	private static final Message T_para1 =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.para1");
	private static final Message T_warning =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.warning");
	private static final Message T_para2 =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.para2");
	private static final Message T_column1 =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.column1");
	private static final Message T_column2 =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.column2");
	private static final Message T_column3 =
		message("xmlui.administrative.registries.DeleteMetadataSchemaConfirm.column3");
	private static final Message T_submit_delete =
		message("xmlui.general.delete");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");

	protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();

	
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
		String idsString = parameters.getParameter("schemaIDs", null);
		
		ArrayList<MetadataSchema> schemas = new ArrayList<MetadataSchema>();
		for (String id : idsString.split(","))
		{
			MetadataSchema schema = metadataSchemaService.find(context,Integer.valueOf(id));
			schemas.add(schema);
		}
 
		// DIVISION: metadata-schema-confirm-delete
    	Division deleted = body.addInteractiveDivision("metadata-schema-confirm-delete",contextPath+"/admin/metadata-registry",Division.METHOD_POST,"primary administrative metadata-registry");
    	deleted.setHead(T_head);
    	deleted.addPara(T_para1);
    	Para warning = deleted.addPara();
    	warning.addHighlight("bold").addContent(T_warning);
    	warning.addContent(T_para2);
    	
    	Table table = deleted.addTable("schema-confirm-delete",schemas.size() + 1, 3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_column1);
        header.addCell().addContent(T_column2);
        header.addCell().addContent(T_column3);
    	
    	for (MetadataSchema schema : schemas) 
    	{
    		Row row = table.addRow();
    		row.addCell().addContent(schema.getID());
        	row.addCell().addContent(schema.getNamespace());
        	row.addCell().addContent(schema.getName());
	    }
    	Para buttons = deleted.addPara();
    	buttons.addButton("submit_confirm").setValue(T_submit_delete);
    	buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    	
    	deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
