/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;

/**
 *   Action to add metadata to item
 *
 */
public class AddMetadataAction extends UpdateMetadataAction {

    protected MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

	/**
	 *  Adds metadata specified in the source archive
	 * 
	 *  @param context DSpace Context
	 *  @param itarch item archive
	 *  @param isTest test flag
	 *  @param suppressUndo undo flag
	 *  @throws AuthorizeException if authorization error
	 *  @throws SQLException if database error
	 */
	@Override
    public void execute(Context context, ItemArchive itarch, boolean isTest,
            boolean suppressUndo) throws AuthorizeException, SQLException
	{
		Item item = itarch.getItem();
		String dirname = itarch.getDirectoryName();
	    	
		for (DtoMetadata dtom : itarch.getMetadataFields())
		{
			for (String f : targetFields)
			{
				if (dtom.matches(f, false))
				{
					// match against metadata for this field/value in repository
					// qualifier must be strictly matched, possibly null					
					List<MetadataValue> ardcv = null;
					ardcv = itemService.getMetadata(item, dtom.schema, dtom.element, dtom.qualifier, Item.ANY);
					
					boolean found = false;
					for (MetadataValue dcv : ardcv)
					{
						if (dcv.getValue().equals(dtom.value))
						{
							found = true;
							break;
						}
					}
					
					if (found)
					{
						ItemUpdate.pr("Warning:  No new metadata found to add to item " + dirname 
								+ " for element " + f);						
					}
					else
					{
						if (isTest)
						{
							ItemUpdate.pr("Metadata to add: " + dtom.toString());
							   //validity tests that would occur in actual processing
	        	            // If we're just test the import, let's check that the actual metadata field exists.
	        	        	MetadataSchema foundSchema = metadataSchemaService.find(context, dtom.schema);
	        	        	
	        	        	if (foundSchema == null)
	        	        	{
	        	        		ItemUpdate.pr("ERROR: schema '" 
	        	        			+ dtom.schema + "' was not found in the registry; found on item " + dirname);
	        	        	}
	        	        	else
	        	        	{
		        	        	MetadataField foundField = metadataFieldService.findByElement(context, foundSchema, dtom.element, dtom.qualifier);
		        	        	
		        	        	if (foundField == null)
		        	        	{
		        	        		ItemUpdate.pr("ERROR: Metadata field: '" + dtom.schema + "." + dtom.element + "." 
		        	        				+ dtom.qualifier + "' not found in registry; found on item " + dirname);
		        	            }		
	        				}						
						}
						else
						{
							itemService.addMetadata(context, item, dtom.schema, dtom.element, dtom.qualifier, dtom.language, dtom.value);
							ItemUpdate.pr("Metadata added: " + dtom.toString());
	
							if (!suppressUndo)
							{
								//itarch.addUndoDtom(dtom);
								//ItemUpdate.pr("Undo metadata: " + dtom);
								
								// add all as a replace record to be preceded by delete
								for (MetadataValue dcval : ardcv)
								{
                                    MetadataField metadataField = dcval.getMetadataField();
                                    MetadataSchema metadataSchema = metadataField.getMetadataSchema();
                                    itarch.addUndoMetadataField(DtoMetadata.create(metadataSchema.getName(), metadataField.getElement(),
                                            metadataField.getQualifier(), dcval.getLanguage(), dcval.getValue()));
								}
								
							}
						}
					}
					break;  // don't need to check if this field matches any other target fields
				}
			} 				
		}   
	}

}
