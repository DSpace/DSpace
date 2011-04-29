/*
 * AddMetadataAction.java
 *
 * Version: $Revision: 3984 $
 *
 * Date: $Date: 2009-06-29 22:33:25 -0400 (Mon, 29 Jun 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
package org.dspace.app.itemupdate;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
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
	 *  @param ItemArchive
	 *  @param isTest
	 *  @param suppressUndo
	 *  @throws AuthorizeException
	 *  @throws SQLException
	 */
	public void execute(Context context, ItemArchive itarch, boolean isTest, boolean suppressUndo) 
	throws AuthorizeException, SQLException
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
					DCValue[] ardcv = null;
					ardcv = item.getMetadata(dtom.schema, dtom.element, dtom.qualifier, Item.ANY);
					
					boolean found = false;
					for (DCValue dcv : ardcv)
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
								for (DCValue dcval : ardcv)
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
