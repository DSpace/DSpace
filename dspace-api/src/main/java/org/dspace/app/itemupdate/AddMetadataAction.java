/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;

import org.dspace.core.Context;

/**
 *   Action to add metadata to item
 *
 */
public class AddMetadataAction extends UpdateMetadataAction {
	
	/**
	 * 	Adds metadata specified in the source archive
	 * 
	 *  @param context
	 *  @param itarch
	 *  @param isTest
	 *  @param suppressUndo
	 *  @throws AuthorizeException
	 *  @throws SQLException
	 */
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
					Metadatum[] ardcv = null;
					ardcv = item.getMetadata(dtom.schema, dtom.element, dtom.qualifier, Item.ANY);
					
					boolean found = false;
					for (Metadatum dcv : ardcv)
					{
						if (dcv.value.equals(dtom.value))
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
	        	        	MetadataSchema foundSchema = MetadataSchema.find(context, dtom.schema);
	        	        	
	        	        	if (foundSchema == null)
	        	        	{
	        	        		ItemUpdate.pr("ERROR: schema '" 
	        	        			+ dtom.schema + "' was not found in the registry; found on item " + dirname);
	        	        	}
	        	        	else
	        	        	{
		        	        	int schemaID = foundSchema.getSchemaID();
		        	        	MetadataField foundField = MetadataField.findByElement(context, schemaID, dtom.element, dtom.qualifier);
		        	        	
		        	        	if (foundField == null)
		        	        	{
		        	        		ItemUpdate.pr("ERROR: Metadata field: '" + dtom.schema + "." + dtom.element + "." 
		        	        				+ dtom.qualifier + "' not found in registry; found on item " + dirname);
		        	            }		
	        				}						
						}
						else
						{
							item.addMetadata(dtom.schema, dtom.element, dtom.qualifier, dtom.language, dtom.value);
							ItemUpdate.pr("Metadata added: " + dtom.toString());
	
							if (!suppressUndo)
							{
								//itarch.addUndoDtom(dtom);
								//ItemUpdate.pr("Undo metadata: " + dtom);
								
								// add all as a replace record to be preceded by delete
								for (Metadatum dcval : ardcv)
								{							
									itarch.addUndoMetadataField(DtoMetadata.create(dcval.schema, dcval.element, 
											dcval.qualifier, dcval.language, dcval.value));
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
