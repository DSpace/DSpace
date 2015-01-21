/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.text.ParseException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 *    Action to delete metadata 
 * 
 *
 */
public class DeleteMetadataAction extends UpdateMetadataAction {

	/**
	 *   Delete metadata from item
	 * 
	 *  @param context
	 *  @param itarch
	 *  @param isTest
	 *  @param suppressUndo
	 *  @throws ParseException
	 *  @throws AuthorizeException
	 */
	public void execute(Context context, ItemArchive itarch, boolean isTest,
            boolean suppressUndo) throws AuthorizeException, ParseException 
	{
		Item item = itarch.getItem();
		for (String f : targetFields)
		{
			DtoMetadata dummy = DtoMetadata.create(f, Item.ANY, "");
			Metadatum[] ardcv = item.getMetadataByMetadataString(f);

			ItemUpdate.pr("Metadata to be deleted: ");
			for (Metadatum dcv : ardcv)
			{
				ItemUpdate.pr("  " + MetadataUtilities.getDCValueString(dcv));
			}

			if (!isTest)
			{
				if (!suppressUndo)
				{
					for (Metadatum dcv : ardcv)
					{
						itarch.addUndoMetadataField(DtoMetadata.create(dcv.schema, dcv.element, 
								dcv.qualifier, dcv.language, dcv.value));					
					}
				}
				
				item.clearMetadata(dummy.schema, dummy.element, dummy.qualifier, Item.ANY);	
			}
		}
	}
}
